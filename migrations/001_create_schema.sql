-- RealEstate Aggregator — initial PostgreSQL schema
-- Derived from openapi.yaml (Channel, Post, Bookmark, FetchLog)
-- Apply with: psql "$DATABASE_URL" -f migrations/001_create_schema.sql

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------- channels
CREATE TABLE IF NOT EXISTS channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instagram_handle TEXT NOT NULL UNIQUE,
    display_name TEXT,
    profile_url TEXT,
    source TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_fetched_at TIMESTAMPTZ,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- posts
CREATE TABLE IF NOT EXISTS posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instagram_post_id TEXT UNIQUE,
    shortcode TEXT,
    channel_id UUID REFERENCES channels (id) ON DELETE SET NULL,
    original_url TEXT,
    caption TEXT,
    media JSONB NOT NULL DEFAULT '[]'::jsonb,
    media_type TEXT CHECK (media_type IN ('image', 'video', 'carousel')),
    timestamp TIMESTAMPTZ,
    likes_count INTEGER NOT NULL DEFAULT 0,
    comments_count INTEGER NOT NULL DEFAULT 0,
    extracted JSONB NOT NULL DEFAULT '{}'::jsonb,
    source TEXT,
    raw_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_removed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_posts_channel_id ON posts (channel_id);
CREATE INDEX IF NOT EXISTS idx_posts_timestamp_desc ON posts (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_posts_instagram_post_id ON posts (instagram_post_id);
-- Keep raw_payload for debugging; purge on a retention window (see notes below).
CREATE INDEX IF NOT EXISTS idx_posts_extracted_gin ON posts USING GIN (extracted);

-- ---------------------------------------------------------------- bookmarks
CREATE TABLE IF NOT EXISTS bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_id UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_bookmarks_user_post UNIQUE (user_id, post_id)
);

CREATE INDEX IF NOT EXISTS idx_bookmarks_user_id ON bookmarks (user_id);
CREATE INDEX IF NOT EXISTS idx_bookmarks_post_id ON bookmarks (post_id);

-- ---------------------------------------------------------------- fetch_logs
CREATE TABLE IF NOT EXISTS fetch_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID REFERENCES channels (id) ON DELETE SET NULL,
    job_id TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    status TEXT,
    details JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_fetch_logs_channel_id ON fetch_logs (channel_id);
CREATE INDEX IF NOT EXISTS idx_fetch_logs_status ON fetch_logs (status);

-- ---------------------------------------------------------------- helper: touch updated_at on posts/channels
CREATE OR REPLACE FUNCTION touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_posts_touch ON posts;
CREATE TRIGGER trg_posts_touch BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

DROP TRIGGER IF EXISTS trg_channels_touch ON channels;
CREATE TRIGGER trg_channels_touch BEFORE UPDATE ON channels
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- NOTE (from DEVELOPMENT_TODO.md): keep raw_payload for debugging but consider
-- rotation/purging after a retention window, e.g.:
--   DELETE FROM posts WHERE is_removed AND updated_at < now() - INTERVAL '90 days';
