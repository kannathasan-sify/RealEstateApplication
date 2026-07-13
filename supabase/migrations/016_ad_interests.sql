-- ─────────────────────────────────────────────────────────────────────────────
-- Migration 016 — Ad Interests (Lead Capture)
-- Stores users who tapped "I'm Interested" on an advertisement banner.
-- Advertiser / admin uses this table to follow up with interested leads.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS ad_interests (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Advertisement context (no FK — ads are managed externally / in-app)
    ad_id            TEXT        NOT NULL,
    ad_title         TEXT        NOT NULL,
    advertiser_name  TEXT        NOT NULL,
    listing_type     TEXT        NOT NULL DEFAULT 'general',

    -- User who expressed interest
    user_id          UUID        REFERENCES profiles(id) ON DELETE CASCADE,
    user_name        TEXT,
    user_phone       TEXT,
    user_email       TEXT,

    -- Optional note from the user ("I'm interested in 2BHK only…")
    note             TEXT,

    -- Admin follow-up status
    -- pending  → new lead, not yet contacted
    -- contacted → admin/agent reached out to user
    -- converted → user made a booking / purchase via the ad
    -- closed   → lead closed without conversion
    status           TEXT        NOT NULL DEFAULT 'pending'
                                CHECK (status IN ('pending','contacted','converted','closed')),

    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- One interest record per (ad, user) pair — upsert on conflict
    UNIQUE (ad_id, user_id)
);

-- ── Updated-at trigger ───────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_ad_interests_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ad_interests_updated_at
    BEFORE UPDATE ON ad_interests
    FOR EACH ROW EXECUTE FUNCTION update_ad_interests_updated_at();

-- ── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_ad_interests_ad_id   ON ad_interests (ad_id);
CREATE INDEX IF NOT EXISTS idx_ad_interests_user_id ON ad_interests (user_id);
CREATE INDEX IF NOT EXISTS idx_ad_interests_status  ON ad_interests (status);

-- ── Row-Level Security ───────────────────────────────────────────────────────
ALTER TABLE ad_interests ENABLE ROW LEVEL SECURITY;

-- Users can read and manage their own interest records
CREATE POLICY "user_own_interests"
    ON ad_interests
    FOR ALL
    USING (auth.uid() = user_id);

-- Admins have full access to all interest records
CREATE POLICY "admin_all_interests"
    ON ad_interests
    FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM profiles
            WHERE id = auth.uid() AND role = 'admin'
        )
    );
