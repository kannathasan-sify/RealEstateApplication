-- 019_property_leads_rejected_status.sql
-- Adds a 'rejected' status to property_leads, for the Admin Dashboard "Enquiries" tab
-- (admin can reject a lead that's spam / not a genuine enquiry, distinct from deleting it).
--
-- The original CHECK constraint in 018_property_leads.sql was declared inline on the
-- column, so Postgres auto-named it "property_leads_status_check". Drop + recreate it
-- with the extra allowed value.

ALTER TABLE property_leads DROP CONSTRAINT IF EXISTS property_leads_status_check;

ALTER TABLE property_leads
    ADD CONSTRAINT property_leads_status_check
    CHECK (status IN ('pending','contacted','visit_scheduled','converted','closed','rejected'));

-- Reload PostgREST's schema cache so the new constraint (and any lagging cache from
-- 018) is picked up immediately instead of waiting for the next auto-reload.
NOTIFY pgrst, 'reload schema';
