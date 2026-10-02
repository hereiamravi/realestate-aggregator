const express = require('express');
const path = require('path');
const fs = require('fs');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const DATA_DIR = path.join(__dirname, 'data');
const FEED_FILE = path.join(DATA_DIR, 'feed.json');

// In-memory mock bookmarks (resets on restart; fine for local demo)
const bookmarks = new Map(); // bookmarkId -> { id, user_id, post_id, created_at }

function loadFeed() {
  try {
    const raw = fs.readFileSync(FEED_FILE, 'utf8');
    return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to load feed.json', e);
    return { items: [], next_cursor: null, page_size: 25 };
  }
}

function decodeCursor(cursor) {
  if (cursor == null || cursor === '') return 0;
  const n = parseInt(cursor, 10);
  return Number.isNaN(n) || n < 0 ? 0 : n;
}

app.get('/v1/feed', (req, res) => {
  const feed = loadFeed();
  const pageSize = Math.min(parseInt(req.query.page_size, 10) || 25, 100);
  const offset = decodeCursor(req.query.cursor);
  const items = feed.items.slice(offset, offset + pageSize);
  const nextOffset = offset + items.length;
  const next_cursor = nextOffset < feed.items.length ? String(nextOffset) : null;
  res.json({ items, next_cursor, page_size: pageSize });
});

app.get('/v1/posts/:id', (req, res) => {
  const feed = loadFeed();
  const post = feed.items.find(p => p.id === req.params.id || p.instagram_post_id === req.params.id);
  if (!post) return res.status(404).json({ code: 404, message: 'Post not found' });
  res.json(post);
});

app.get('/v1/channels', (req, res) => {
  // return channels inferred from posts
  const feed = loadFeed();
  const channelsMap = {};
  feed.items.forEach(p => {
    if (p.channel_id && !channelsMap[p.channel_id]) {
      channelsMap[p.channel_id] = { id: p.channel_id, instagram_handle: p.channel_id, display_name: p.channel_id, profile_url: null, source: 'mock', enabled: true };
    }
  });
  res.json(Object.values(channelsMap));
});

// ---- Mock bookmarks (mirrors openapi.yaml /bookmarks) ----
app.get('/v1/bookmarks', (req, res) => {
  const pageSize = Math.min(parseInt(req.query.page_size, 10) || 25, 100);
  const offset = decodeCursor(req.query.cursor);
  const all = Array.from(bookmarks.values());
  const items = all.slice(offset, offset + pageSize);
  const nextOffset = offset + items.length;
  res.json({ items, next_cursor: nextOffset < all.length ? String(nextOffset) : null });
});

app.post('/v1/bookmarks', (req, res) => {
  const post_id = req.body && req.body.post_id;
  if (!post_id) return res.status(400).json({ code: 400, message: 'post_id is required' });
  const id = `bm_${Date.now()}`;
  const bookmark = { id, user_id: 'mock-user', post_id, created_at: new Date().toISOString() };
  bookmarks.set(id, bookmark);
  res.status(201).json(bookmark);
});

app.delete('/v1/bookmarks/:id', (req, res) => {
  if (!bookmarks.has(req.params.id)) {
    // Also allow deleting by post_id for convenience
    const byPost = Array.from(bookmarks.values()).find(b => b.post_id === req.params.id);
    if (byPost) bookmarks.delete(byPost.id);
    else return res.status(404).json({ code: 404, message: 'Bookmark not found' });
  } else {
    bookmarks.delete(req.params.id);
  }
  res.status(204).send();
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`Mock server listening on http://localhost:${PORT}`);
});
