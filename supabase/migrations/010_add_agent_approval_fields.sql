-- ============================================================
-- 010_add_agent_approval_fields.sql
-- Add agent contact info + admin approval workflow columns
-- Run AFTER 009_fix_listed_by_constraint.sql
-- ============================================================

-- Agent / builder contact fields stored directly on the property
-- (populated when listed_by = 'agent' or 'builder')
ALTER TABLE properties
  ADD COLUMN IF NOT EXISTS agent_name  TEXT,
  ADD COLUMN IF NOT EXISTS agent_phone TEXT,
  ADD COLUMN IF NOT EXISTS agent_photo TEXT;

-- Admin approval workflow
--   PENDING  → submitted, awaiting review
--   APPROVED → visible in public listing
--   REJECTED → agent notified with reason; can edit and resubmit
ALTER TABLE properties
  ADD COLUMN IF NOT EXISTS approval_status TEXT NOT NULL DEFAULT 'pending'
    CHECK (approval_status IN ('pending', 'approved', 'rejected'));

ALTER TABLE properties
  ADD COLUMN IF NOT EXISTS rejection_reason TEXT;  -- admin fills on reject

-- Public listing should only show approved properties
-- Update the existing public_read RLS policy to enforce approval_status
-- (The policy in 007_rls_policies.sql currently only checks status = 'active')
DROP POLICY IF EXISTS "public_read" ON properties;
CREATE POLICY "public_read" ON properties
  FOR SELECT
  USING (status = 'active' AND approval_status = 'approved');

-- Index for admin dashboard queries by approval status
CREATE INDEX IF NOT EXISTS idx_properties_approval_status
  ON properties(approval_status);
