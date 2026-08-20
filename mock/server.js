const express = require('express');
const path = require('path');
const fs = require('fs');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const DATA_DIR = path.join(__dirname, 'data');
const FEED_FILE = path.join(DATA_DIR, 'feed.json');

function loadFeed() {
  try {
    const raw = fs.readFileSync(FEED_FILE, 'utf8');
    return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to load feed.json', e);
    return { items: [], next_cursor: null, page_size: 25 };
  }
}

app.get('/v1/feed', (req, res) => {
  const feed = loadFeed();
  // basic paging: page_size and cursor (cursor is ignored in mock)
  const pageSize = Math.min(parseInt(req.query.page_size) || 25, 100);
  const items = feed.items.slice(0, pageSize);
  res.json({ items, next_cursor: feed.next_cursor || null, page_size: pageSize });
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

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`Mock server listening on http://localhost:${PORT}`);
});
