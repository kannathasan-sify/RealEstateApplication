# NestX — WhatsApp Lead Automation Plan

> **Status:** Free path (Phase 0 + Phase 1) IMPLEMENTED as of 2026-07-14. Phase 2 (paid
> WhatsApp Business API) + Phase 3 (agentic AI) remain future roadmap, contingent on
> getting a provider.
> **Author:** drafted with Claude Code, 2026-07-14
>
> **Implemented (free path):**
> - Phase 0 — pre-filled WhatsApp enquiry from the property detail screen (`wa.me?text=`),
>   in `PropertyDetailScreen.kt` (`buildEnquiryMessage`).
> - Phase 1 — property-lead capture: `property_leads` table (migration
>   `supabase/migrations/018_property_leads.sql`), `backend/app/routers/property_leads.py`
>   (POST interest / GET mine / GET received / GET per-property / PATCH status), Android
>   `PropertyLead` model + repo/API wiring, an "I'm Interested" CTA on the property detail
>   screen, and a two-tab **Enquiries** screen (`ui/leads/`) reachable from the Menu.

---

## 1. Goal (in the user's words)

> When a customer/buyer is interested in a property, the property details should be shared
> over WhatsApp — the buyer's enquiry goes to the **owner's** number, and the property
> information goes to the **buyer**. The same flow applies for **agents / brokers**.
> Eventually make this an **agentic AI automated** flow.

So: **interest → auto-share property + contact details between buyer and owner/agent over
WhatsApp**, growing into an AI-driven lead assistant.

---

## 2. What the app already has (reuse, don't rebuild)

| Asset | Where | Relevance |
|-------|-------|-----------|
| Contact numbers per listing | `properties.whatsapp_number`, `properties.agent_phone`, `properties.agent_name` | The owner/agent WhatsApp target already exists on every property |
| WhatsApp button (manual) | `PropertyDetailScreen.kt` → opens `https://wa.me/<number>` | **No pre-filled text today** — buyer opens a blank chat and types manually. Nothing is sent to the owner automatically; nothing goes to the buyer. |
| Lead-capture pattern | `backend/app/routers/ad_interests.py` (+ `ad_interests` table) | Stores a lead: buyer name/phone/email + a status pipeline `pending → contacted → converted → closed`. **This is the exact template for property leads.** |
| Buyer contact data | `profiles.full_name / phone / email` | Everything needed to populate a lead + message the buyer |
| Visit requests | `bookings` table + "Received Inquiries" owner view | Existing owner-facing inbox to extend for leads / visit scheduling |

**The gap:** there is no "I'm interested in *this property*" lead (only the ad-interest
version and the manual call/WhatsApp buttons), and **nothing sends WhatsApp messages**.
Those two things are what this plan adds.

---

## 3. The one constraint that shapes the whole design

There are two fundamentally different ways to "send WhatsApp." The choice drives cost,
setup, and how "automatic" it can be.

### Option A — Click-to-chat deep link (`wa.me` / `api.whatsapp.com`)
- Opens WhatsApp **on the buyer's phone** with a **pre-filled message** they tap *Send* on.
- **Free**, no accounts, no approval — works today.
- **Limitations:** buyer-initiated (owner is *not* notified until the buyer taps Send);
  one direction only (buyer → owner); cannot message two parties; cannot send from a server.

### Option B — WhatsApp Business API (Meta Cloud API, or a BSP)
- Your **backend sends real messages** to the owner **and** the buyer automatically —
  no one has to tap anything.
- **Requirements:**
  - A Meta Business account + business verification.
  - A dedicated WhatsApp Business phone number (a number **not** already registered on
    personal/regular WhatsApp).
  - **Pre-approved message templates** (Meta reviews the wording of any business-initiated
    message; "utility" category fits lead notifications).
  - A provider (see §7).
- **Cost:** Meta charges per conversation/message (India "utility" ≈ ₹0.11–0.35 range;
  Meta has been shifting toward per-message pricing — confirm current rates at build time)
  plus the provider's platform fee.

**Consequence:** the "auto-share to owner *and* buyer" ask is only fully possible with
**Option B**. Option A is the free, instant, one-direction fallback and a great Phase 0.

---

## 4. Phased roadmap

Each phase is independently shippable and builds on the previous one. Phases 0–1 need no
paid account and are the agreed starting point.

### Phase 0 — Pre-filled WhatsApp message (FREE, ship first)
**What:** upgrade the existing `wa.me` button so it opens WhatsApp with a rich, structured
message already typed, so one tap sends a proper enquiry to the owner/agent.

- Buyer taps **"Enquire on WhatsApp"** on `PropertyDetailScreen`.
- App builds a message like:
  ```
  Hi {agent_name}, I'm interested in this property on NestX:

  🏠 {title}
  📍 {neighborhood}, {district}
  💰 {price_display}
  🔖 Ref: {reference_id}
  🔗 {deep_link_or_listing_id}

  — {buyer_name}
  ```
- Opens `https://wa.me/{whatsapp_number}?text={url_encoded_message}`.

**Scope:** Android only, in `PropertyDetailScreen.kt` (and optionally `PropertyCard`).
Zero backend, zero cost. Delivers ~80% of the *perceived* value immediately.
**Limitation to be honest about:** still buyer-initiated; the buyer must tap Send; the
buyer does not automatically receive a copy (they already have the details on-screen).

### Phase 1 — Property-lead capture backend (FREE, do next)
**What:** persist every "interested" event so owners/agents have a real lead list, and so
there's a server-side trigger point for automation later.

- **New table `property_leads`** (mirror `ad_interests`):
  ```sql
  CREATE TABLE property_leads (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id  UUID REFERENCES properties(id),
    property_ref TEXT,            -- denormalized reference_id/title snapshot
    property_title TEXT,
    owner_id     UUID REFERENCES profiles(id),   -- who should receive the lead
    buyer_id     UUID REFERENCES profiles(id),
    buyer_name   TEXT,
    buyer_phone  TEXT,
    buyer_email  TEXT,
    channel      TEXT DEFAULT 'app',   -- app | whatsapp | call
    message      TEXT,
    status       TEXT DEFAULT 'pending'
                 CHECK (status IN ('pending','contacted','visit_scheduled','converted','closed')),
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(property_id, buyer_id)
  );
  -- RLS: buyer sees own leads; owner sees leads on their properties; admin sees all.
  ```
- **New endpoints** (`backend/app/routers/property_leads.py`, mirror `ad_interests.py`):
  | Method | Path | Purpose |
  |--------|------|---------|
  | POST | `/properties/{id}/interest` | Buyer registers interest → creates a lead |
  | GET | `/properties/leads/mine` | Buyer's own enquiry history |
  | GET | `/properties/{id}/leads` | Owner/agent: leads on this property |
  | PATCH | `/properties/leads/{lead_id}/status` | Owner updates pipeline status |
- **Android:** an "I'm Interested" button on `PropertyCard` / `PropertyDetailScreen`, and a
  simple **"My Leads" (buyer)** + **"Enquiries" (owner)** list — reuse the existing
  `ad_interests` UI patterns and the `sealed UiState` convention (see the `nestx-compose-ui`
  skill). Mock-mode support via `MockData` as usual.

At the end of Phase 1 you have a full lead pipeline with **no paid dependencies** — and the
`POST /interest` call is the exact hook Phase 2 automates.

### Phase 2 — True WhatsApp automation (needs Option B account)
**What:** on lead creation, backend sends **two** templated WhatsApp messages automatically.

- **On `POST /properties/{id}/interest`:**
  1. → **owner/agent:** `"🏠 New lead on NestX: {buyer_name} ({buyer_phone}) is interested
     in {property_ref} — {title}, {price}, {district}."`
  2. → **buyer:** `"Thanks for your interest in {title} ({price}, {district}). {agent_name}
     will contact you shortly. Owner WhatsApp: {number}."`
- Requires 2 approved **utility templates** (owner-notify, buyer-confirm).
- Add a thin `services/whatsapp_service.py` abstraction so the provider can be swapped.
- Store provider message IDs + delivery status back on the lead.
- **Broker/agent variant** is just routing: if the property is listed by an agent, the
  "owner" target is the agent; you can also fan out to a broker pool by district/category.

### Phase 3 — Agentic AI layer (needs Phase 2 + Claude API)
Once messages flow both ways through the Business API, layer Claude (Opus 4.8 / Sonnet 5,
per project stack guidance) on top:

1. **AI-drafted messages** — Claude composes each notification from property + buyer context
   instead of a fixed template (still sent within an approved template wrapper where required).
2. **Inbound conversational agent** — buyer messages the business number; Claude answers
   price / availability / amenity questions directly from Supabase and books a visit into
   `bookings`.
3. **Lead qualification & scoring** — Claude extracts budget / timeline / intent from the
   chat, scores the lead, and pings the agent only for hot leads.
4. **Auto-routing** — dispatch each lead to the right agent/broker by district + category.
5. **Follow-up automation** — if the owner ignores a lead for N hours, AI nudges; scheduled
   reminders to the buyer.

Implementation shape: a webhook endpoint for inbound WhatsApp events → intent handling with
the Anthropic SDK → tool calls against existing repositories (properties, bookings, leads).

---

## 5. How the "agent / broker" case maps

The buyer→agent flow is **not a separate system** — it's the same lead pipeline with a
different recipient:

- If `properties.listed_by = agent` (or an `agency_id` is set), the lead's `owner_id` /
  notification target resolves to the **agent** instead of the individual owner.
- Phase 3 routing can additionally broadcast an open lead to a **pool of brokers** filtered
  by district + work/property category, then assign the first responder.

So agents/brokers are handled by *recipient resolution + routing*, layered on the one
`property_leads` core.

---

## 6. What is possible vs. what it costs (honest summary)

| Capability | Phase | Paid account? | Effort |
|------------|-------|---------------|--------|
| Pre-filled WhatsApp enquiry to owner (one tap) | 0 | No | Hours (Android only) |
| Stored lead pipeline + owner "Enquiries" screen | 1 | No | ~Days (backend + 2 screens + migration) |
| Auto-send WhatsApp to owner **and** buyer | 2 | **Yes** (Business API + provider) | Days of dev + provider onboarding/approval (~1–2 wks lead time) + per-message cost |
| AI-drafted messages, inbound bot, scoring, routing, follow-ups | 3 | Yes (Business API + Claude API) | Larger; new webhook service + LLM cost per message |

---

## 7. India WhatsApp Business API providers (for when you're ready — Phase 2)

All wrap Meta's Cloud API; pick on price, onboarding ease, and dashboard.

- **Meta Cloud API (direct)** — cheapest per message, most setup/verification work yourself.
- **AiSensy** — popular with Indian SMBs, low cost, quick onboarding, template management UI.
- **Interakt** (by Haptik/Jio) — strong India SMB fit, good CRM/inbox.
- **Gupshup** — scale-oriented, broad channel support.
- **WATI** — shared team inbox + no-code flows.
- **Twilio** — developer-friendly API, global, typically pricier for India.

> ⚠️ Pricing and template-category rules change frequently — verify current India rates and
> the utility/marketing category rules with the chosen provider before committing.

---

## 8. Recommended next actions (given "start free")

1. **Phase 0** — implement the pre-filled `wa.me` message in `PropertyDetailScreen` (and
   optionally a card-level "Enquire" shortcut). *Immediate, free, high-impact.*
2. **Phase 1** — add the `property_leads` migration, the `property_leads` router, and the
   buyer "My Leads" + owner "Enquiries" screens.
3. **Decide on a provider** (AiSensy / Interakt are the easiest India starts) *only when*
   you're ready to pay for true two-way automation — that unlocks Phase 2.
4. Keep Phase 3 (agentic AI) as the north star; it needs Phases 0–2 in place first.

---

## 9. Open questions to resolve before building Phase 2+

- Which WhatsApp Business provider (drives the `whatsapp_service.py` integration)?
- Will there be a **single** NestX-owned business number sending on behalf of all owners,
  or per-agent numbers? (Single number is far simpler and standard for marketplaces.)
- Do owners/agents consent to receiving lead notifications on WhatsApp (opt-in / template
  compliance)?
- Broker pool model: broadcast-to-all vs. round-robin vs. AI-routed assignment?
