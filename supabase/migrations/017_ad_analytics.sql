-- ─────────────────────────────────────────────────────────────────────────────
-- Migration 017 — Ad Analytics Events
-- Stores every interaction event fired by the Android AdAnalyticsTracker.
-- Used for CTR, engagement, A/B, and campaign reporting.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS ad_analytics (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Ad / campaign context
    ad_id           TEXT        NOT NULL,
    ad_title        TEXT        NOT NULL,
    campaign_id     TEXT,
    variant         TEXT        NOT NULL DEFAULT 'A',

    -- Event type (mirrors AdEventType enum)
    event_type      TEXT        NOT NULL
                    CHECK (event_type IN (
                        'impression', 'click', 'video_play', 'video_complete',
                        'share', 'interest', 'interest_removed',
                        'cta_click', 'dismiss'
                    )),

    -- User context (nullable for anonymous)
    user_id         UUID        REFERENCES profiles(id) ON DELETE SET NULL,
    user_district   TEXT,
    session_id      TEXT,

    -- Engagement depth
    dwell_seconds   INT         DEFAULT 0,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_ad_analytics_ad_id      ON ad_analytics (ad_id);
CREATE INDEX IF NOT EXISTS idx_ad_analytics_campaign   ON ad_analytics (campaign_id);
CREATE INDEX IF NOT EXISTS idx_ad_analytics_event_type ON ad_analytics (event_type);
CREATE INDEX IF NOT EXISTS idx_ad_analytics_user_id    ON ad_analytics (user_id);
CREATE INDEX IF NOT EXISTS idx_ad_analytics_created_at ON ad_analytics (created_at DESC);

-- ── RLS ───────────────────────────────────────────────────────────────────────
ALTER TABLE ad_analytics ENABLE ROW LEVEL SECURITY;

-- Anyone can insert their own events (INSERT only — no user_id check, supports anonymous)
CREATE POLICY "insert_own_event"
    ON ad_analytics
    FOR INSERT
    WITH CHECK (true);

-- Admins can read everything
CREATE POLICY "admin_read_all"
    ON ad_analytics
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM profiles
            WHERE id = auth.uid() AND role = 'admin'
        )
    );
