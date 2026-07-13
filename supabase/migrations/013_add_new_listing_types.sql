-- Migration 013: Add Holiday Stay & Find a Contractor listing types
-- Run this in Supabase SQL Editor after all previous migrations.

-- ── 1. listing_type: add 'holiday_stay' and 'contractor' ──────────────────────
ALTER TABLE properties
  DROP CONSTRAINT IF EXISTS properties_listing_type_check;

ALTER TABLE properties
  ADD CONSTRAINT properties_listing_type_check
  CHECK (listing_type IN ('rent','sale','off_plan','holiday_stay','contractor'));

-- ── 2. property_type: add Holiday Stay and Contractor work types ──────────────
ALTER TABLE properties
  DROP CONSTRAINT IF EXISTS properties_property_type_check;

ALTER TABLE properties
  ADD CONSTRAINT properties_property_type_check
  CHECK (property_type IN (
    -- Residential
    'apartment','villa','townhouse','penthouse','hotel_apartment',
    'residential_building','villa_compound','residential_floor',
    -- Commercial
    'office','shop','warehouse','labour_camp','commercial_building',
    'commercial_floor','commercial_villa','land','industrial_land','factory',
    -- Holiday Stay types
    'hotel','resort','room',
    -- Contractor work types
    'building','villa_house','interior_fitout',
    'civil_work','painting_work','air_conditioning','plumbing','household_equipment',
    -- Generic
    'other'
  ));

-- ── 3. listed_by: add 'individual', 'company', 'owner' ───────────────────────
ALTER TABLE properties
  DROP CONSTRAINT IF EXISTS properties_listed_by_check;

ALTER TABLE properties
  ADD CONSTRAINT properties_listed_by_check
  CHECK (listed_by IN (
    'landlord','agent','agency','developer','builder',
    'individual','company','owner'
  ));

-- ── 4. New indexes for quick category-wise browsing ───────────────────────────
CREATE INDEX IF NOT EXISTS idx_properties_holiday_stay
  ON properties(listing_type) WHERE listing_type = 'holiday_stay';

CREATE INDEX IF NOT EXISTS idx_properties_contractor
  ON properties(listing_type) WHERE listing_type = 'contractor';
