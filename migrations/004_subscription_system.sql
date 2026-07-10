-- 004_subscription_system.sql
-- Add subscription tier and expiration columns to profiles table

ALTER TABLE public.profiles
ADD COLUMN IF NOT EXISTS subscription_tier VARCHAR(50) DEFAULT 'free',
ADD COLUMN IF NOT EXISTS subscription_expires_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

-- Enable index for fast lookups by subscription tier
CREATE INDEX IF NOT EXISTS idx_profiles_subscription_tier ON public.profiles(subscription_tier);
