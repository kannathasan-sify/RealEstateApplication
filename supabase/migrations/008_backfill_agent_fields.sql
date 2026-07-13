-- ─────────────────────────────────────────────────────────────────────────────
-- 008_backfill_agent_fields.sql
-- Backfill agent_name / agent_phone / agent_photo for all properties that
-- currently store NULL for those columns. Pulls the values from the poster's
-- profiles row (full_name, phone, avatar_url).
--
-- Run once in Supabase SQL Editor (or via supabase db push) after deploying the
-- updated backend that now auto-populates agent fields on every new property.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE public.properties AS p
SET
    agent_name  = COALESCE(NULLIF(p.agent_name,  ''), pr.full_name),
    agent_phone = COALESCE(NULLIF(p.agent_phone, ''), pr.phone),
    agent_photo = COALESCE(NULLIF(p.agent_photo, ''), pr.avatar_url),
    updated_at  = NOW()
FROM public.profiles AS pr
WHERE p.owner_id = pr.id
  AND (
       p.agent_name  IS NULL OR p.agent_name  = '' OR
       p.agent_phone IS NULL OR p.agent_phone = '' OR
       p.agent_photo IS NULL OR p.agent_photo = ''
  );

-- Verify: count of properties that still have no agent_phone after backfill
SELECT COUNT(*) AS missing_phone
FROM   public.properties
WHERE  agent_phone IS