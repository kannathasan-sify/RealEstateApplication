-- =============================================================================
-- Migration 003 — Module Redesign
-- Run in Supabase SQL Editor (Dashboard → SQL Editor → New Query)
-- =============================================================================

-- ─── 1. New columns on the properties table ────────────────────────────────

ALTER TABLE properties
  ADD COLUMN IF NOT EXISTS youtube_url        TEXT,
  ADD COLUMN IF NOT EXISTS instagram_url      TEXT,
  ADD COLUMN IF NOT EXISTS rate_per_sqft      FLOAT,
  ADD COLUMN IF NOT EXISTS deposit            FLOAT,
  ADD COLUMN IF NOT EXISTS availability_date  DATE,
  ADD COLUMN IF NOT EXISTS nearby_schools     JSONB DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS nearby_hospitals   JSONB DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS document_urls      JSONB DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS company_profile    TEXT,
  ADD COLUMN IF NOT EXISTS previous_projects  JSONB DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS rating_avg         FLOAT DEFAULT 0,
  ADD COLUMN IF NOT EXISTS rating_count       INT   DEFAULT 0;

-- Raise image limit: no DB constraint change needed (enforced by API)
-- Update existing metadata column to allow any JSONB value (was TEXT JSONB before)
-- This is a no-op if already JSONB.

-- ─── 2. service_requests table ────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS service_requests (
  id            UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID    REFERENCES auth.users(id) ON DELETE CASCADE,
  category      TEXT    NOT NULL CHECK (category IN ('construction', 'maintenance')),
  service_type  TEXT    NOT NULL,
  title         TEXT    NOT NULL,
  description   TEXT,
  district      TEXT    NOT NULL,
  latitude      FLOAT,
  longitude     FLOAT,
  radius_km     INT     NOT NULL DEFAULT 50,
  budget_min    FLOAT,
  budget_max    FLOAT,
  images        JSONB   DEFAULT '[]'::jsonb,
  status        TEXT    NOT NULL DEFAULT 'open'
                CHECK (status IN ('open','in_progress','completed','cancelled')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS for service_requests
ALTER TABLE service_requests ENABLE ROW LEVEL SECURITY;

-- Anyone authenticated can view open requests
CREATE POLICY "view_open_requests" ON service_requests
  FOR SELECT USING (status = 'open' OR auth.uid() = user_id);

-- Users can insert their own requests
CREATE POLICY "insert_own_request" ON service_requests
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Users can update/cancel their own requests
CREATE POLICY "update_own_request" ON service_requests
  FOR UPDATE USING (auth.uid() = user_id);

-- ─── 3. quotations table ──────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS quotations (
  id              UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id      UUID  REFERENCES service_requests(id) ON DELETE CASCADE,
  contractor_id   UUID  REFERENCES auth.users(id) ON DELETE CASCADE,
  property_id     UUID  REFERENCES properties(id) ON DELETE SET NULL,
  amount          FLOAT,
  timeline        TEXT,
  notes           TEXT,
  status          TEXT  NOT NULL DEFAULT 'pending'
                  CHECK (status IN ('pending','accepted','rejected')),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS for quotations
ALTER TABLE quotations ENABLE ROW LEVEL SECURITY;

-- Request owner can see all quotations for their requests
CREATE POLICY "view_quotations_for_owner" ON quotations
  FOR SELECT USING (
    contractor_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM service_requests sr
      WHERE sr.id = quotations.request_id AND sr.user_id = auth.uid()
    )
  );

-- Contractors can insert quotations
CREATE POLICY "insert_quotation" ON quotations
  FOR INSERT WITH CHECK (auth.uid() = contractor_id);

-- Contractor can update their own quotation (before accepted)
CREATE POLICY "update_own_quotation" ON quotations
  FOR UPDATE USING (auth.uid() = contractor_id AND status = 'pending');

-- ─── 4. Auto-compute rate_per_sqft trigger ────────────────────────────────

CREATE OR REPLACE FUNCTION compute_rate_per_sqft()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.area_sqft IS NOT NULL AND NEW.area_sqft > 0 THEN
    NEW.rate_per_sqft := ROUND((NEW.price / NEW.area_sqft)::NUMERIC, 2);
  ELSE
    NEW.rate_per_sqft := NULL;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_rate_per_sqft ON properties;
CREATE TRIGGER trg_rate_per_sqft
  BEFORE INSERT OR UPDATE OF price, area_sqft ON properties
  FOR EACH ROW EXECUTE FUNCTION compute_rate_per_sqft();

-- ─── 5. Update rating_avg when a new review is inserted ──────────────────

CREATE OR REPLACE FUNCTION update_property_rating()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE properties
  SET
    rating_avg   = (SELECT ROUND(AVG(rating)::NUMERIC, 1) FROM reviews WHERE property_id = NEW.property_id),
    rating_count = (SELECT COUNT(*) FROM reviews WHERE property_id = NEW.property_id)
  WHERE id = NEW.property_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_property_rating ON reviews;
CREATE TRIGGER trg_property_rating
  AFTER INSERT OR UPDATE OR DELETE ON reviews
  FOR EACH ROW EXECUTE FUNCTION update_property_rating();

-- ─── 6. Index for district-based service request lookup ───────────────────

CREATE INDEX IF NOT EXISTS idx_service_requests_district
  ON service_requests (district, status);

CREATE INDEX IF NOT EXISTS idx_service_requests_user
  ON service_requests (user_id);

CREATE INDEX IF NOT EXISTS idx_quotations_request
  ON quotations (request_id);

-- ─── Done ─────────────────────────────────────────────────────────────────
-- Run SELECT * FROM service_requests LIMIT 1; to verify table creation.
