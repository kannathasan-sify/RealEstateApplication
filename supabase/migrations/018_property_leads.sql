-- 018_property_leads.sql
-- Property-level buyer leads ("I'm Interested" on a listing).
-- Mirrors ad_interests, but for actual property listings. This is the trigger point
-- the future paid WhatsApp automation (Phase 2) hooks into — on insert, a backend
-- worker can notify the owner/agent + buyer. For now it just stores the lead so the
-- owner/agent has a real enquiry inbox and the buyer has an enquiry history.

CREATE TABLE IF NOT EXISTS property_leads (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id    UUID REFERENCES properties(id) ON DELETE CASCADE,
    -- Denormalized snapshot so the lead still reads well if the listing changes/deletes
    property_ref   TEXT,
    property_title TEXT,
    -- Who should receive the lead (property owner, or the agent who listed it)
    owner_id       UUID REFERENCES profiles(id),
    -- The interested buyer
    buyer_id       UUID REFERENCES profiles(id),
    buyer_name     TEXT,
    buyer_phone    TEXT,
    buyer_email    TEXT,
    channel        TEXT DEFAULT 'app',   -- app | whatsapp | call
    message        TEXT,
    status         TEXT DEFAULT 'pending'
                   CHECK (status IN ('pending','contacted','visit_scheduled','converted','closed')),
    created_at     TIMESTAMPTZ DEFAULT NOW(),
    updated_at     TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(property_id, buyer_id)
);

CREATE INDEX IF NOT EXISTS idx_property_leads_owner ON property_leads(owner_id);
CREATE INDEX IF NOT EXISTS idx_property_leads_buyer ON property_leads(buyer_id);
CREATE INDEX IF NOT EXISTS idx_property_leads_property ON property_leads(property_id);

ALTER TABLE property_leads ENABLE ROW LEVEL SECURITY;

-- Buyer sees their own enquiries; owner/agent sees leads on their listings.
CREATE POLICY "buyer_own_leads" ON property_leads
    FOR SELECT USING (auth.uid() = buyer_id);

CREATE POLICY "owner_incoming_leads" ON property_leads
    FOR SELECT USING (auth.uid() = owner_id);

CREATE POLICY "buyer_insert_lead" ON property_leads
    FOR INSERT WITH CHECK (auth.uid() = buyer_id);

-- Owner updates the follow-up status of a lead on their listing.
CREATE POLICY "owner_update_lead" ON property_leads
    FOR UPDATE USING (auth.uid() = owner_id);

CREATE POLICY "admin_all_leads" ON property_leads
    USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'));
