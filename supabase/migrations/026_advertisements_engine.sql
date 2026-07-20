-- 026_advertisements_engine.sql
-- Advertisement Ranking & Delivery Engine — core schema for the home-page ad slice.
--
-- Scope: the two central tables (ad_campaigns + advertisements) plus a light advertiser
-- record and a unified user-action log. Detailed impression/click analytics can also flow
-- through the existing ad_analytics table (017); this schema adds the *serving* side the
-- ranker reads. Fraud is kept as scores/flags on the ad row for now (a dedicated
-- FraudDetection table + ML pipeline are a later phase).

-- ── Advertisers ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ad_advertisers (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id           UUID REFERENCES profiles(id) ON DELETE SET NULL,   -- optional owning account
    name                 TEXT NOT NULL,
    advertiser_type      TEXT,        -- builder|bank|agent|broker|legal|surveyor|govt|financial|construction|utility|general
    is_verified          BOOLEAN DEFAULT FALSE,     -- business verification
    government_approved   BOOLEAN DEFAULT FALSE,
    rating               NUMERIC DEFAULT 0,          -- customer rating 0..5
    years_in_business    INT DEFAULT 0,
    lead_success_rate    NUMERIC DEFAULT 0,          -- 0..1
    avg_response_minutes INT,
    created_at           TIMESTAMPTZ DEFAULT NOW()
);

-- ── Campaigns (budget + plan + revenue model) ─────────────────────────────────
CREATE TABLE IF NOT EXISTS ad_campaigns (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advertiser_id      UUID REFERENCES ad_advertisers(id) ON DELETE CASCADE,
    name               TEXT NOT NULL,
    revenue_model      TEXT DEFAULT 'cpc'
                       CHECK (revenue_model IN ('cpc', 'cpm', 'cpa', 'subscription', 'featured')),
    bid_amount         NUMERIC DEFAULT 0,
    daily_budget       NUMERIC DEFAULT 0,
    remaining_budget   NUMERIC DEFAULT 0,           -- decremented as the ad spends; 0 => stop serving
    plan               TEXT DEFAULT 'standard'
                       CHECK (plan IN ('standard', 'featured', 'premium')),
    subscription_level TEXT,                         -- free|silver|gold|platinum
    status             TEXT DEFAULT 'active'
                       CHECK (status IN ('active', 'paused', 'ended')),
    start_date         DATE,
    end_date           DATE,
    created_at         TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ad_campaigns_advertiser ON ad_campaigns(advertiser_id);
CREATE INDEX IF NOT EXISTS idx_ad_campaigns_status     ON ad_campaigns(status);

-- ── Advertisements (creative + targeting + quality + fraud + CTA) ──────────────
CREATE TABLE IF NOT EXISTS advertisements (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id          UUID REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    advertiser_id        UUID REFERENCES ad_advertisers(id) ON DELETE CASCADE,
    property_id          UUID REFERENCES properties(id) ON DELETE SET NULL,  -- for property ads

    -- Creative
    title                TEXT NOT NULL,
    subtitle             TEXT,
    image_url            TEXT,

    -- Taxonomy
    ad_type              TEXT,        -- property|financial|legal|construction|government|utility|general
    category             TEXT,        -- residential|home_loan|ec|builder|bank_auction|...
    priority_level       INT DEFAULT 5 CHECK (priority_level BETWEEN 1 AND 5),  -- 1 = highest

    -- Sponsorship + call-to-action
    sponsored_status     TEXT DEFAULT 'organic'
                         CHECK (sponsored_status IN ('organic', 'sponsored', 'featured', 'promoted')),
    cta                  TEXT DEFAULT 'view_property',
                         -- view_property|call_owner|book_site_visit|apply_home_loan|visit_builder|get_legal_verification
    cta_target           TEXT,        -- property id / phone / url / deeplink

    -- Targeting
    target_districts     TEXT[],
    target_listing_types TEXT[],
    target_property_types TEXT[],
    budget_min           NUMERIC,
    budget_max           NUMERIC,
    latitude             FLOAT,
    longitude            FLOAT,

    -- Quality + historical performance (0..1 rates)
    quality_score        NUMERIC DEFAULT 0.5,
    ctr                  NUMERIC DEFAULT 0,
    conversion_rate      NUMERIC DEFAULT 0,
    impressions_count    INT DEFAULT 0,
    clicks_count         INT DEFAULT 0,
    conversions_count    INT DEFAULT 0,

    -- Real-time signals
    is_verified          BOOLEAN DEFAULT FALSE,
    is_urgent            BOOLEAN DEFAULT FALSE,
    has_price_drop       BOOLEAN DEFAULT FALSE,

    -- Fraud / spam (scores 0..1, higher = worse). Ranker applies negative weights.
    fraud_score          NUMERIC DEFAULT 0,
    spam_score           NUMERIC DEFAULT 0,
    is_duplicate         BOOLEAN DEFAULT FALSE,

    -- Lifecycle
    status               TEXT DEFAULT 'active'
                         CHECK (status IN ('active', 'paused', 'expired', 'rejected')),
    expires_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ DEFAULT NOW(),
    updated_at           TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ads_status        ON advertisements(status);
CREATE INDEX IF NOT EXISTS idx_ads_priority      ON advertisements(priority_level);
CREATE INDEX IF NOT EXISTS idx_ads_campaign      ON advertisements(campaign_id);
CREATE INDEX IF NOT EXISTS idx_ads_type          ON advertisements(ad_type);
CREATE INDEX IF NOT EXISTS idx_ads_districts     ON advertisements USING GIN (target_districts);

-- ── Unified user action log (impression/click/conversion/hide/report) ─────────
CREATE TABLE IF NOT EXISTS ad_user_actions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_id      UUID REFERENCES advertisements(id) ON DELETE CASCADE,
    user_id    UUID REFERENCES profiles(id) ON DELETE SET NULL,   -- null = anonymous
    action     TEXT NOT NULL
               CHECK (action IN ('impression', 'click', 'conversion', 'hide', 'report')),
    reason     TEXT,                                              -- for 'report' / 'hide'
    session_id TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ad_actions_ad   ON ad_user_actions(ad_id);
CREATE INDEX IF NOT EXISTS idx_ad_actions_user ON ad_user_actions(user_id);
CREATE INDEX IF NOT EXISTS idx_ad_actions_kind ON ad_user_actions(action);

-- ── RLS ───────────────────────────────────────────────────────────────────────
-- The backend serves/writes with the service-role client; these are defense-in-depth.
ALTER TABLE advertisements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "public_read_active_ads" ON advertisements
    FOR SELECT USING (status = 'active');
CREATE POLICY "admin_write_ads" ON advertisements
    USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'));

ALTER TABLE ad_campaigns ENABLE ROW LEVEL SECURITY;
CREATE POLICY "admin_all_campaigns" ON ad_campaigns
    USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'));

ALTER TABLE ad_advertisers ENABLE ROW LEVEL SECURITY;
CREATE POLICY "public_read_advertisers" ON ad_advertisers FOR SELECT USING (true);
CREATE POLICY "admin_all_advertisers" ON ad_advertisers
    USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'));

ALTER TABLE ad_user_actions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "insert_any_ad_action" ON ad_user_actions FOR INSERT WITH CHECK (true);
CREATE POLICY "admin_read_ad_actions" ON ad_user_actions
    FOR SELECT USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'));
