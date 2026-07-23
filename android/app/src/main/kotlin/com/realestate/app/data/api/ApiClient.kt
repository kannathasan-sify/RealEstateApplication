package com.realestate.app.data.api

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.realestate.app.BuildConfig
import com.realestate.app.data.models.ApprovalStatus
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /**
     * Gson instance with two custom adapters:
     *
     * 1. Null-safe String adapter — converts JSON null → "" so non-nullable
     *    Kotlin String fields never receive null at runtime.
     *
     * 2. Case-insensitive ApprovalStatus adapter — the backend stores
     *    approval_status as lowercase ("pending", "approved", "rejected") but
     *    Kotlin enum constants are uppercase (PENDING, APPROVED, REJECTED).
     *    Gson's default enum handling is case-sensitive, so without this adapter
     *    every deserialized approvalStatus would be null, causing:
     *      • Pending/Approved/Rejected filter tabs to show nothing
     *      • "All" tab + review screen to crash with NoWhenBranchMatchedException
     */
    private val gson = GsonBuilder()
        // ── 1. Null-safe String ──────────────────────────────────────────────
        .registerTypeAdapter(String::class.java, object : TypeAdapter<String>() {
            override fun write(out: JsonWriter, value: String?) { out.value(value) }
            override fun read(input: JsonReader): String {
                if (input.peek() == JsonToken.NULL) {
                    input.nextNull()
                    return ""
                }
                return input.nextString()
            }
        })
        // ── 2. Case-insensitive ApprovalStatus ───────────────────────────────
        .registerTypeAdapter(ApprovalStatus::class.java, object : TypeAdapter<ApprovalStatus>() {
            override fun write(out: JsonWriter, value: ApprovalStatus?) {
                out.value(value?.name?.lowercase())
            }
            override fun read(input: JsonReader): ApprovalStatus {
                if (input.peek() == JsonToken.NULL) {
                    input.nextNull()
                    return ApprovalStatus.PENDING   // safe default
                }
                val raw = input.nextString()
                // Match case-insensitively: "pending" → PENDING, "approved" → APPROVED, etc.
                return ApprovalStatus.values()
                    .firstOrNull { it.name.equals(raw, ignoreCase = true) }
                    ?: ApprovalStatus.PENDING       // unknown value → treat as pending
            }
        })
        .create()

    /** How long a cacheable GET stays fresh on disk before OkHttp re-validates. */
    private const val CACHE_MAX_AGE_SECONDS = 120

    /**
     * Only *public, non-user-specific* GETs may be written to the disk cache.
     *
     * OkHttp keys its cache by URL, so caching a user-scoped endpoint would let one
     * account's data be served to the next account that hits the same URL. Everything
     * outside this allowlist is explicitly marked `no-store`.
     *
     * Excluded on purpose: /auth, /saved, /searches, /bookings, /dashboard, /admin,
     * /subscriptions, /support, /service-requests (user-scoped) and /ads (personalised +
     * budget/expiry sensitive — serving a stale ad would be wrong).
     */
    private fun isPubliclyCacheable(path: String, method: String): Boolean {
        if (method != "GET") return false
        val p = path.lowercase()
        // User-scoped or fast-changing sub-resources under an otherwise public path.
        if (p.contains("/mine") || p.contains("/leads") || p.contains("/discussions") ||
            p.contains("/interests") || p.contains("/analytics")
        ) return false
        return p.contains("/properties") || p.contains("/agencies") || p.contains("/reviews")
    }

    fun create(authInterceptor: AuthInterceptor, cache: Cache?): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

        // Network interceptor (must be a *network* interceptor to influence caching):
        // the FastAPI backend doesn't send Cache-Control, so we supply it ourselves.
        val cacheControl = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            val directive =
                if (isPubliclyCacheable(request.url.encodedPath, request.method))
                    "public, max-age=$CACHE_MAX_AGE_SECONDS"
                else
                    "no-store"
            response.newBuilder()
                .header("Cache-Control", directive)
                .removeHeader("Pragma")   // legacy no-cache header would defeat the above
                .build()
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .addNetworkInterceptor(cacheControl)
            .apply { if (cache != null) cache(cache) }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))   // ← custom gson
            .build()
            .create(ApiService::class.java)
    }
}
