-- 022_add_channel_partner_role.sql
-- Adds 'channel_partner' as a valid profile role (referral partners — see the Channel
-- Partner dashboard). Recreates profiles_role_check to include it alongside the existing
-- (incl. legacy) role vocabulary. Keep this in sync with backend VALID_ROLES
-- (schemas/auth.py) and the Android UserRole enum.

ALTER TABLE profiles DROP CONSTRAINT IF EXISTS profiles_role_check;

ALTER TABLE profiles ADD CONSTRAINT profiles_role_check
    CHECK (role IN (
        'buyer', 'agent', 'builder', 'admin', 'channel_partner',
        -- legacy values retained so existing rows keep validating
        'landlord', 'agency', 'developer'
    ));
