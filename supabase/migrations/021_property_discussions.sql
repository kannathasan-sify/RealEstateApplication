-- 021_property_discussions.sql
-- Property Q&A / discussion threads (see backend/app/routers/discussions.py).
-- Until now this table only existed implicitly: the router falls back to an in-memory
-- cache whenever the DB query fails, which is exactly what happens in any environment
-- where this table was never created. This migration formalizes it so the Q&A feature
-- persists for real instead of living in process memory.
--
-- Shape mirrors what the router actually reads/writes:
--   * INSERT writes only property_id, user_id, message, parent_id.
--   * SELECT embeds profiles(full_name) via the user_id FK to derive user_name at read
--     time — so there is deliberately NO user_name column here (it is never stored).
--   * parent_id NULL  -> top-level question; parent_id set -> reply to that question.

CREATE TABLE IF NOT EXISTS property_discussions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID REFERENCES properties(id) ON DELETE CASCADE,
    -- FK to profiles is required for the PostgREST `profiles(full_name)` embed to resolve.
    -- CASCADE: a deleted user's questions/replies are removed (nothing is snapshotted here,
    -- so an orphaned row would just render as the "User" fallback anyway).
    user_id     UUID REFERENCES profiles(id) ON DELETE CASCADE,
    message     TEXT NOT NULL,
    -- Self-reference: NULL = top-level question, set = reply. Deleting a question removes
    -- its replies.
    parent_id   UUID REFERENCES property_discussions(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_property_discussions_property ON property_discussions(property_id);
CREATE INDEX IF NOT EXISTS idx_property_discussions_parent   ON property_discussions(parent_id);
CREATE INDEX IF NOT EXISTS idx_property_discussions_user     ON property_discussions(user_id);

ALTER TABLE property_discussions ENABLE ROW LEVEL SECURITY;

-- Q&A on public listings is public: anyone can read the thread.
-- (The router itself uses the service-role client which bypasses RLS; these policies are
--  the defense-in-depth path for any anon/user-scoped client.)
CREATE POLICY "public_read_discussions" ON property_discussions
    FOR SELECT USING (true);

-- A user may only post messages as themselves.
CREATE POLICY "insert_own_discussion" ON property_discussions
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Author may edit/delete their own message.
CREATE POLICY "update_own_discussion" ON property_discussions
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "delete_own_discussion" ON property_discussions
    FOR DELETE USING (auth.uid() = user_id);

-- Admins have full access (moderation).
CREATE POLICY "admin_all_discussions" ON property_discussions
    USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'));
