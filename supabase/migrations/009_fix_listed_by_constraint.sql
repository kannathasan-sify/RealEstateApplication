-- ============================================================
-- 009_fix_listed_by_constraint.sql
-- Fix properties.listed_by CHECK constraint to include 'builder'
-- Run AFTER 008_add_district.sql
--
-- Root cause: 003_properties.sql only allowed
--   ('landlord','agent','agency','developer')
-- The updated role system uses 'builder' instead of 'developer',
-- and seed data / PostAdViewModel both emit listed_by = 'builder'.
-- ============================================================

-- Step 1: Drop the outdated constraint
ALTER TABLE properties
  DROP CONSTRAINT IF EXISTS properties_listed_by_check;

-- Step 2: Add updated constraint that includes 'builder'
--   Keeps legacy values (landlord, agency, developer) so any
--   existing rows are not invalidated.
ALTER TABLE properties
  ADD CONSTRAINT properties_listed_by_check
    CHECK (listed_by IN ('landlord', 'agent', 'agency', 'developer', 'builder'));
