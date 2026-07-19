-- 024_referrals_and_commissions.sql
-- Two tables behind the Agent + Channel Partner dashboards.
--
--  referrals   — a channel partner refers a buyer/seller against a listing and earns a
--                commission when it converts. Drives the partner "Referral funnel",
--                "Referral pipeline" and (paid rows) the payout trend.
--  commissions — money earned by an agent OR channel partner, one row per earning event,
--                stamped with earned_at so monthly "Commission earned / payout" trends can
--                be aggregated. For partners, commission rows are created from referrals;
--                for agents, from closed deals.

CREATE TABLE IF NOT EXISTS referrals (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id        UUID REFERENCES profiles(id) ON DELETE CASCADE,   -- the channel partner
    referred_name     TEXT,
    referred_phone    TEXT,
    property_id       UUID REFERENCES properties(id) ON DELETE SET NULL,
    property_value    NUMERIC,
    stage             TEXT NOT NULL DEFAULT 'sent'
                      CHECK (stage IN ('sent', 'contacted', 'site_visit', 'converted', 'lost')),
    commission_amount NUMERIC DEFAULT 0,
    commission_status TEXT NOT NULL DEFAULT 'none'
                      CHECK (commission_status IN ('none', 'pending', 'paid')),
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_referrals_partner ON referrals(partner_id);
CREATE INDEX IF NOT EXISTS idx_referrals_stage   ON referrals(stage);
CREATE INDEX IF NOT EXISTS idx_referrals_created ON referrals(created_at);

CREATE TABLE IF NOT EXISTS commissions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES profiles(id) ON DELETE CASCADE,   -- agent or channel_partner
    earner_role  TEXT CHECK (earner_role IN ('agent', 'channel_partner')),
    source       TEXT,                                             -- 'referral' | 'listing_sale' | ...
    reference_id UUID,                                             -- referral id / property id
    amount       NUMERIC DEFAULT 0,
    status       TEXT NOT NULL DEFAULT 'pending'
                 CHECK (status IN ('pending', 'paid')),
    earned_at    TIMESTAMPTZ DEFAULT NOW(),                        -- drives the monthly trend
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_commissions_user   ON commissions(user_id);
CREATE INDEX IF NOT EXISTS idx_commissions_earned ON commissions(earned_at);

-- ── RLS (backend uses the service-role client; these are the defense-in-depth path) ──
ALTER TABLE referrals ENABLE ROW LEVEL SECURITY;

CREATE POLICY "partner_own_referrals" ON referrals
    USING (auth.uid() = partner_id);
CREATE POLICY "admin_all_referrals" ON referrals
    USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'));

ALTER TABLE commissions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "earner_own_commissions" ON commissions
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "admin_all_commissions" ON commissions
    USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'));
