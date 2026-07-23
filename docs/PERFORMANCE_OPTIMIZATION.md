# NestX — Screen Performance & Loading Design

Reference for how each Android screen loads data, what it caches, and why.
Last updated: 2026-07-23.

---

## 1. Core principles

1. **Never block on a spinner.** Every screen paints its real structure immediately using a
   *skeleton* sized to match the final content, so there is **no layout jump** when data lands.
2. **Skeleton only on a cold load.** A refresh keeps existing content on screen and updates it
   in place — users never watch content they already had disappear.
3. **Don't refetch what you just fetched.** Each ViewModel keeps a `lastLoadedAt` timestamp and
   skips a reload inside a freshness window (60s). Explicit refresh always bypasses it.
4. **Cache only what is safe to cache.** Public listing data may hit the HTTP disk cache;
   anything user-scoped must not (see §3).
5. **Never serialise independent requests.** If a screen needs two endpoints, fire them
   concurrently with `async`.

---

## 2. Shared building blocks

| Piece | Location | Purpose |
|---|---|---|
| `rememberShimmerAlpha()` | `ui/components/Skeleton.kt` | One pulse timing (0.25→0.6, 750ms) so all screens feel identical |
| `SkeletonBlock` | `ui/components/Skeleton.kt` | A single shimmering placeholder; size it via `Modifier` |
| `ListRowSkeleton` / `ListSkeleton` | `ui/components/Skeleton.kt` | Reusable list-row skeleton (thumbnail + 3 lines) |
| Freshness guard | each ViewModel | `lastLoadedAt` + `force: Boolean = false` |
| HTTP disk cache | `di/NetworkModule.kt`, `data/api/ApiClient.kt` | 10 MB OkHttp cache, allowlisted GETs, `max-age=120` |
| In-memory property cache | `data/repository/PropertyRepository.kt` | `Map<id, Property>`, 5-min TTL, seeded from list responses |

### The freshness-guard pattern

```kotlin
private var lastLoadedAt = 0L

fun load(force: Boolean = false) {
    val now = System.currentTimeMillis()
    val hasData = /* current state already holds data */
    if (!force && hasData && now - lastLoadedAt < FRESH_MS) return
    lastLoadedAt = now
    viewModelScope.launch {
        if (!hasData) showSkeleton()   // cold load only
        ...
    }
}
```

> ⚠️ **Any Refresh / Retry button must call `load(force = true)`.**
> With the guard in place, `onRefresh = viewModel::load` would silently do nothing within the
> window and the button would look broken.

---

## 3. Caching policy (important)

OkHttp keys its disk cache **by URL**. Caching a user-scoped endpoint would let one account's
data be served to the next account that hits the same URL on a shared device.

Implemented in `ApiClient.isPubliclyCacheable(path, method)`:

| Cached (120s) | Never cached (`no-store`) |
|---|---|
| `/properties` (list, search, detail, `/similar`) | `/auth`, `/saved`, `/searches`, `/bookings` |
| `/agencies` | `/dashboard/*`, `/admin/*` |
| `/reviews` | `/subscriptions`, `/support`, `/service-requests` |
| | `/ads/*` — personalised + budget/expiry sensitive |
| | Sub-paths: `/mine`, `/leads`, `/discussions`, `/interests`, `/analytics` |

The FastAPI backend does not send `Cache-Control`, so a **network interceptor** supplies it.

**Why dashboards are not disk-cached:** the payloads are per-user (your commission, your leads)
and admin analytics is platform-sensitive. They use the in-memory guard only, which dies with
the process.

---

## 4. Per-screen design

### 4.1 Home — `ui/home/`

| | |
|---|---|
| **Loading UI** | `AdBannerSkeleton` (184dp — exactly the ad banner height) |
| **Guard** | `FRESH_WINDOW_MS = 60s` on `loadHome(district, force)` |
| **Caching** | Ads **not** cached (personalised, budget/expiry sensitive) |
| **Requests on open** | **1** (was 6) |

- The 5 category-wise property fetches (Rent/Sale/Holiday/Ground/Contractor) were **commented
  out** — those rows are no longer rendered, but the calls were still firing on every load and
  district change. Re-enable the block in `HomeViewModel.loadHome` if the rows return.
- Full-screen loading spinner removed; the ad feed owns its own state.
- **Fixed a coroutine leak:** `selectedDistrict` was a `get()` property that built a *new*
  `MutableStateFlow` **and launched a new infinite collector** on every access. It is now
  derived once via `.map { }.distinctUntilChanged().stateIn(...)`.
- `_adsLoading` drives the skeleton, and is only raised when the ad list is empty, so a refresh
  never flashes an empty feed.

### 4.2 Property list — `ui/property/PropertyListScreen.kt`

| | |
|---|---|
| **Loading UI** | 5 × `PropertyCardSkeleton` (180dp image + 3 lines — mirrors `PropertyCard`) |
| **Guard** | 60s, keyed on `effectiveFilter.toString()` |
| **Caching** | HTTP disk cache (120s), key includes query params |

- `items(filteredProperties, key = { it.id })` — stable keys let Compose reuse rows instead of
  re-composing the whole list.
- Guard reloads anyway when: a `filter` is passed explicitly, `force = true`, or the current
  state is `Error` (so **Retry always works**).
- Back-navigation from a detail screen → **0 requests**, content already on screen.

### 4.3 Property detail — `ui/property/PropertyDetailScreen.kt`

| | |
|---|---|
| **Loading UI** | `PropertyDetailSkeleton` (260dp hero + price/title/location + 3 chips + 3 lines) |
| **Caching** | In-memory property cache (5-min TTL) + HTTP disk cache |
| **Round-trips before first paint** | **1** (was 2) |

Three separate wins:

1. **Parallel fetch.** `getProperty` and `getSimilar` were *sequential*, so nothing rendered
   until both finished — even though "Similar properties" is below the fold. `getSimilar` now
   runs via `async` and the screen paints as soon as the property itself arrives.
2. **Instant paint from cache.** `PropertyRepository` seeds a `Map<id, Property>` from **every
   list response**, so tapping a card you just saw renders **real content immediately** while
   the network refresh happens underneath.
3. **No skeleton flash on re-entry.** Re-opening the same property keeps the existing content
   and refreshes silently.

Safety details: a late `similar` result is discarded if the user navigated to another property
(`current.property.id == property.id`), and `similarDeferred.cancel()` runs on failure.

### 4.4 Dashboards — `ui/dashboard/` (Owner · Agent · Channel Partner · Admin Analytics)

| | |
|---|---|
| **Loading UI** | `DashboardSkeleton` — 2×2 KPI tiles + 3 card blocks, inside `DashboardScreenScaffold` |
| **Guard** | `DASHBOARD_FRESH_MS = 60s` per ViewModel |
| **Caching** | **In-memory only** — never disk-cached (user-scoped / platform-sensitive) |

- Refresh keeps the current figures on screen; skeleton only on a cold load.
- All Refresh/Retry actions pass `force = true`.
- **Admin extra:** `loadPeople()` (agent / builder / partner rosters for the drill-down filter)
  now runs **once per screen session** instead of 3 extra requests on every open.
- Admin drill-down (`?user_id=`) is admin-only server side via `_resolve_uid`.

### 4.5 My Ads — `ui/myads/`

| | |
|---|---|
| **Loading UI** | `ListSkeleton(count = 5)` |
| **Guard** | `MY_ADS_FRESH_MS = 60s` |
| **Caching** | Not disk-cached — `/properties/mine` is excluded |

Refresh keeps the current ads visible while reloading. `deleteProperty` removes the row locally
for instant feedback.

### 4.6 Enquiries — `ui/leads/`

| | |
|---|---|
| **Loading UI** | `ListSkeleton(count = 5)` |
| **Guard** | `LEADS_FRESH_MS = 60s` |
| **Caching** | Not disk-cached — `/leads` is excluded |
| **Round-trips** | **1** (was 2) |

`getMyLeads()` and `getReceivedLeads()` were sequential, but **both tabs live on the same
screen** — so the user waited for two round-trips before seeing anything. They now run
concurrently via `async`.

---

## 5. Before / after summary

| Screen | Requests on open | Return visit | Loading |
|---|---|---|---|
| Home | 6 → **1** | 0 (within 60s) | ad skeleton |
| Property list | 1 | **0** (guard) / disk cache | 5 card skeletons |
| Property detail | 2 sequential → **1** | instant from cache | detail skeleton |
| Dashboards ×5 | 1 (Admin 4 → 1 + rosters once) | 0 (within 60s) | tile skeleton |
| My Ads | 1 | 0 (within 60s) | list skeleton |
| Enquiries | 2 sequential → **1** | 0 (within 60s) | list skeleton |

---

## 6. Applying this to a new screen — checklist

1. **Skeleton, not spinner.** Reuse `ListSkeleton` / `SkeletonBlock`, or build one sized to the
   real content so nothing shifts when data lands.
2. **Add the freshness guard** (`lastLoadedAt` + `force`) to the ViewModel's `load()`.
3. **Wire Refresh/Retry to `load(force = true)`** — otherwise the guard makes them no-ops.
4. **Show the skeleton only when there's no data yet** (`if (!hasData) ...Loading`).
5. **Parallelise independent calls** with `async` — never `await` one before starting the next.
6. **Decide the cache policy.** If the endpoint is user-scoped, confirm it's excluded in
   `ApiClient.isPubliclyCacheable`. Public listing data can be added to the allowlist.
7. **Use stable `key = { it.id }`** in every `LazyColumn`/`LazyRow` over domain objects.

---

## 7. Known gaps / next steps

- **Logout does not clear caches.** `PropertyRepository.clearPropertyCache()` exists but isn't
  called yet, and the OkHttp `Cache` is injectable but never `evictAll()`-ed. Worth wiring into
  `AuthRepository.logout()` for shared devices. *(Low risk today: only public listing data is
  cached, but it's good hygiene.)*
- **List results themselves aren't cached in memory** — only individual properties. Switching
  district A→B→A re-runs the load (served from the disk cache, so no network, but it re-parses).
  A small multi-entry list cache would remove even that.
- **Room / offline-first was evaluated and deliberately deferred.** It mainly buys *offline*,
  which isn't a NestX requirement yet, and costs TypeConverters for the large `Property` model
  (`List<String>` arrays + `metadata` map), a kapt/KSP change alongside Hilt, and ongoing schema
  migrations. Revisit if "save for offline" or offline field-agent use becomes a real feature.
- **Not yet covered by this pattern:** Saved, My Bookings, Service Request feed, Chat. They
  still use plain spinners and have no freshness guard.
