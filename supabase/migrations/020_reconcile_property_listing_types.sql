-- Migration 020: Reconcile properties.listing_type / property_type CHECK constraints
-- with backend/app/schemas/property.py's ListingType / PropertyType Pydantic enums.
--
-- Root cause: FastAPI validates POST /properties bodies against the Pydantic enums
-- BEFORE the row ever reaches Postgres. Migrations 013/014 gave the DB CHECK
-- constraints their own, older vocabulary (building/villa_house/civil_work/etc. for
-- property_type, and no 'maintenance' for listing_type) that never tracked the
-- Pydantic enums as they grew. Once Android was fixed (2026-07-15) to send values
-- that match the Pydantic enum, those same values then got past FastAPI only to be
-- rejected here with `violates check constraint "properties_listing_type_check"` /
-- "properties_property_type_check" — this migration closes that second gap by
-- making both CHECK constraints exactly mirror the Pydantic enums (see
-- backend/app/schemas/property.py ListingType / PropertyType). This is the
-- reconciliation CLAUDE.md's Doc Drift Note #3 flagged as still outstanding.

-- ── 1. listing_type: add 'maintenance' (already valid in Pydantic ListingType) ──
ALTER TABLE properties
  DROP CONSTRAINT IF EXISTS properties_listing_type_check;

-- NOT VALID: enforce for future inserts/updates only, don't scan/reject existing rows
-- (there's leftover test data using the old pre-020 vocabulary that we don't want to
-- touch or have this migration fail on).
ALTER TABLE properties
  ADD CONSTRAINT properties_listing_type_check
  CHECK (listing_type IN (
    'rent','sale','off_plan','holiday_stay','ground','contractor','maintenance'
  )) NOT VALID;

-- ── 2. property_type: replace with the full Pydantic PropertyType vocabulary ────
ALTER TABLE properties
  DROP CONSTRAINT IF EXISTS properties_property_type_check;

ALTER TABLE properties
  ADD CONSTRAINT properties_property_type_check
  CHECK (property_type IN (
    -- Residential (Buy / Rent)
    'apartment','villa','townhouse','penthouse','independent_house',
    'residential_building','villa_compound','residential_floor','farmhouse',
    -- Commercial (Buy / Rent)
    'office','shop','showroom','warehouse','commercial_building','commercial_floor',
    -- Land / Agricultural / Industrial (Buy)
    'agricultural_land','industrial_land','industrial_property','land',
    -- Rental-specific
    'hotel','resort','home_stay','pg_room','room',
    -- Holiday Stay
    'entire_home',
    -- Ground / Sports
    'cricket_ground','football','badminton','swimming_pool',
    'other_open_ground','other_closed_ground',
    -- Construction Contractor types
    'civil_contractor','builder','architect','structural_engineer','interior_designer',
    'plumbing_contractor','electrical_contractor','painting_contractor','false_ceiling',
    'tiles_contractor','roofing','landscaping',
    -- Maintenance Service types
    'electrician','plumber','carpenter','ac_service','cctv_service','cleaning_service',
    'painting_service','pest_control','borewell','water_tank_cleaning',
    -- Fallback/Sub-category custom mappings
    'residential','commercial','hotel_resort','hotel___resort',
    'home_stay_pg','home_stay___pg','industrial_properties',
    -- Generic
    'other'
  )) NOT VALID;

-- ── 3. Index for maintenance category browsing (mirrors idx_properties_contractor) ──
CREATE INDEX IF NOT EXISTS idx_properties_maintenance
  ON properties(listing_type) WHERE listing_type = 'maintenance';

-- Reload PostgREST's schema cache so the updated constraints are picked up immediately.
NOTIFY pgrst, 'reload schema';
