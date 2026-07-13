-- Migration 014: Add Ground (sports venue) listing type and property types
-- Run AFTER migration 013 in Supabase SQL Editor.

-- ── 1. listing_type: add 'ground' ─────────────────────────────────────────────
ALTER TABLE properties
  DROP CONSTRAINT IF EXISTS properties_listing_type_check;

ALTER TABLE properties
  ADD CONSTRAINT properties_listing_type_check
  CHECK (listing_type IN (
    'rent','sale','off_plan',
    'holiday_stay','ground','contractor'
  ));

-- ── 2. property_type: add Ground sub-types ────────────────────────────────────
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
    -- Holiday Stay
    'hotel','resort','room',
    -- Ground / Sports venue
    'cricket_ground','football','other_open_ground',
    'badminton','swimming_pool','other_closed_ground',
    -- Contractor work types
    'building','villa_house','interior_fitout',
    'civil_work','painting_work','air_conditioning','plumbing','household_equipment',
    -- Generic
    'other'
  ));

-- ── 3. Index for ground category browsing ─────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_properties_ground
  ON properties(listing_type) WHERE listing_type = 'ground';
