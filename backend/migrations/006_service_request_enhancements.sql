-- 006_service_request_enhancements.sql
-- Adds urgency, preferred start date, and a direct contact phone to service_requests,
-- so the Service Feed can be filtered/sorted by urgency and requesters can be reached
-- without going through the in-app quotation flow.
--
-- Apply manually (this file lives in backend/migrations/, not supabase/migrations/ —
-- it is NOT picked up by `supabase db push`, same as 003/004/005 in this folder).

ALTER TABLE service_requests
  ADD COLUMN IF NOT EXISTS urgency TEXT DEFAULT 'normal',
  ADD COLUMN IF NOT EXISTS preferred_date DATE,
  ADD COLUMN IF NOT EXISTS contact_phone TEXT;

ALTER TABLE service_requests
  DROP CONSTRAINT IF EXISTS service_requests_urgency_check;

ALTER TABLE service_requests
  ADD CONSTRAINT service_requests_urgency_check
    CHECK (urgency IN ('normal', 'urgent', 'emergency'));

-- Speeds up the Service Feed's "Urgent first" sort/filter and admin triage.
CREATE INDEX IF NOT EXISTS idx_service_requests_urgency ON service_requests(urgency);

COMMENT ON COLUMN service_requests.urgency IS 'normal | urgent | emergency — drives Service Feed badge, filter, and "Urgent first" sort';
COMMENT ON COLUMN service_requests.preferred_date IS 'Requester''s desired start date for the work (optional)';
COMMENT ON COLUMN service_requests.contact_phone IS 'Direct phone number shown to contractors on the request detail screen (optional)';
