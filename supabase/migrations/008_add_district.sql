-- ============================================================
-- 008_add_district.sql
-- Add Tamil Nadu district column to properties table
-- Run AFTER 003_properties.sql
-- ============================================================

-- Add district column (Tamil Nadu district name, e.g. "Karur", "Chennai")
ALTER TABLE properties
  ADD COLUMN IF NOT EXISTS district TEXT;

-- Add approval_status column (admin workflow)
ALTER TABLE properties
  ADD COLUMN IF NOT EXISTS approval_status TEXT NOT NULL DEFAULT 'pending'
    CHECK (approval_status IN ('pending', 'approved', 'rejected'));

-- Add rejection_reason column
ALTER TABLE properties
  ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- Add agent_name, agent_phone, agent_photo columns (denormalised for quick display)
ALTER TABLE properties
  ADD COLUMN IF NOT EXISTS agent_name  TEXT;
ALTER TABLE properties
  ADD COLUMN IF NOT EXISTS agent_phone TEXT;
ALTER TABLE properties
  ADD COLUMN IF NOT EXISTS agent_photo TEXT;

-- Index on district for fast district-wise queries
CREATE INDEX IF NOT EXISTS idx_properties_district
  ON properties(district);

-- Index on approval_status for admin dashboard queries
CREATE INDEX IF NOT EXISTS idx_properties_approval_status
  ON properties(approval_status);

-- Public listing view: only approved + active properties
CREATE OR REPLACE VIEW public_properties AS
  SELECT * FROM properties
  WHERE status = 'active'
    AND approval_status = 'approved';

-- Update roles check in profiles to use updated role list (buyer/agent/builder/admin)
ALTER TABLE profiles
  DROP CONSTRAINT IF EXISTS profiles_role_check;
ALTER TABLE profiles
  ADD CONSTRAINT profiles_role_check
    CHECK (role IN ('buyer', 'agent', 'builder', 