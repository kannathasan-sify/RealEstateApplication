-- ============================================================
-- 011_fix_furnishing_constraint.sql
-- Fix properties.furnishing CHECK constraint mismatch.
-- Run AFTER 010_add_agent_approval_fields.sql
--
-- Root cause: 003_properties.sql used 'semi' as the third furnishing
-- value, but the backend Pydantic schema (Furnishing enum) and the
-- Android app both emit 'semi_furnished'.  Every POST from the app
-- that selects "Semi Furnished" therefore hits a 500:
--   "new row ... violates check constraint properties_furnishing_check"
--
-- Fix: rename the allowed value from 'semi' → 'semi_furnished'.
--   Existing rows with furnishing = 'semi' are migrated first so no
--   data is lost before the constraint is re-added.
-- ============================================================

-- Step 1: Drop the old constraint FIRST (before touching any row data)
--   The UPDATE below would itself violate the existing constraint,
--   so the DROP must come before the data migration.
ALTER TABLE properties
  DROP CONSTRAINT IF EXISTS properties_furnishing_check;

-- Step 2: Migrate any rows that used the old 'semi' value
UPDATE properties
  SET furnishing = 'semi_furnished'
  WHERE furnishing = 'semi';

-- Step 3: Re-add constraint with the correct value
ALTER TABLE properties
  ADD CONSTRAINT properties_furnishing_check
    CHECK (furnishing IN ('furnished', 'unfu