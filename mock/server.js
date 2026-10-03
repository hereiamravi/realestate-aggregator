const express = require('express');
const path = require('path');
const fs = require('fs');
const cors = require('cors');
const {
  fetchApifyInstagramPosts,
  fetchApifyProfileInfo,
  fetchApifyComments,
  searchApifyHashtag,
  searchApifyLocation
} = require('./services/apifyProvider');

// Simple .env parser
const ENV_FILE = path.join(__dirname, '.env');
if (fs.existsSync(ENV_FILE)) {
  const envContent = fs.readFileSync(ENV_FILE, 'utf8');
  envContent.split('\n').forEach(line => {
    const trimmed = line.trim();
    if (trimmed && !trimmed.startsWith('#') && trimmed.includes('=')) {
      const [key, ...val] = trimmed.split('=');
      process.env[key.trim()] = val.join('=').trim();
    }
  });
}

const app = express();
app.use(cors());
app.use(express.json());

const DATA_DIR = path.join(__dirname, 'data');
const FEED_FILE = path.join(DATA_DIR, 'feed.json');

// In-memory mock bookmarks
const bookmarks = new Map();

function loadFeed() {
  try {
    const raw = fs.readFileSync(FEED_FILE, 'utf8');
    return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to load feed.json', e);
    return { items: [], next_cursor: null, page_size: 25 };
  }
}

function saveFeed(feedData) {
  try {
    fs.writeFileSync(FEED_FILE, JSON.stringify(feedData, null, 2), 'utf8');
  } catch (e) {
    console.error('Failed to save feed.json', e);
  }
}

function decodeCursor(cursor) {
  if (cursor == null || cursor === '') return 0;
  const n = parseInt(cursor, 10);
  return Number.isNaN(n) || n < 0 ? 0 : n;
}

// GET /v1/feed
app.get('/v1/feed', async (req, res) => {
  const apifyToken = process.env.APIFY_TOKEN || process.env.PROVIDER_TOKEN;

  // Whenever a valid Apify API token is configured, automatically fetch live Instagram posts
  if (apifyToken && apifyToken !== 'your_apify_api_token_here') {
    try {
      const handles = req.query.handles ? req.query.handles.split(',') : ['realtordotcom', 'realestate'];
      console.log(`[Apify Scraper] Fetching live Instagram posts for handles: ${handles.join(', ')}...`);
      const livePosts = await fetchApifyInstagramPosts({ usernames: handles, limit: 10, apifyToken });

      if (livePosts.length > 0) {
        console.log(`[Apify Scraper] Successfully fetched ${livePosts.length} live Instagram posts!`);
        const feed = loadFeed();

        // Remove old mock fallback posts if real posts are fetched
        const realItems = feed.items.filter(p => p.source === 'apify');
        const existingIds = new Set(realItems.map(p => p.id));
        const newPosts = livePosts.filter(p => !existingIds.has(p.id));

        feed.items = [...livePosts, ...realItems];
        saveFeed(feed);
      }
    } catch (err) {
      console.error('[Apify Scraper Error]', err.message);
    }
  }

  const feed = loadFeed();
  const pageSize = Math.min(parseInt(req.query.page_size, 10) || 25, 100);
  const offset = decodeCursor(req.query.cursor);
  const items = feed.items.slice(offset, offset + pageSize);
  const nextOffset = offset + items.length;
  const next_cursor = nextOffset < feed.items.length ? String(nextOffset) : null;
  res.json({ items, next_cursor, page_size: pageSize });
});

// Admin endpoint to trigger Instagram fetch for a specific channel/handle
app.post('/v1/admin/channels/:id/fetch', async (req, res) => {
  const handle = req.params.id;
  const apifyToken = process.env.APIFY_TOKEN || process.env.PROVIDER_TOKEN;

  if (!apifyToken || apifyToken === 'your_apify_api_token_here') {
    return res.status(400).json({
      code: 400,
      message: 'APIFY_TOKEN is missing in mock/.env file.'
    });
  }

  try {
    const livePosts = await fetchApifyInstagramPosts({ usernames: [handle], limit: 10, apifyToken });
    const feed = loadFeed();
    const existingIds = new Set(feed.items.map(p => p.id));
    const newPosts = livePosts.filter(p => !existingIds.has(p.id));
    feed.items = [...newPosts, ...feed.items];
    saveFeed(feed);

    res.json({
      status: 'success',
      channel_id: handle,
      posts_fetched: livePosts.length,
      new_posts_added: newPosts.length
    });
  } catch (err) {
    console.error('Error fetching channel from Apify:', err);
    res.status(500).json({ code: 500, message: err.message });
  }
});

// 1. Live Profile Info Endpoint
app.get('/v1/channels/:id/profile', async (req, res) => {
  const handle = req.params.id;
  const apifyToken = process.env.APIFY_TOKEN || process.env.PROVIDER_TOKEN;

  if (apifyToken && apifyToken !== 'your_apify_api_token_here') {
    try {
      const profile = await fetchApifyProfileInfo(handle, apifyToken);
      if (profile) return res.json(profile);
    } catch (err) {
      console.error('Failed to fetch Apify profile info:', err.message);
    }
  }

  res.json({
    id: handle,
    instagram_handle: handle,
    display_name: handle,
    profile_url: null,
    source: 'mock'
  });
});

// 2. Live Comments Endpoint
app.get('/v1/posts/:id/comments', async (req, res) => {
  const postId = req.params.id;
  const feed = loadFeed();
  const post = feed.items.find(p => p.id === postId || p.instagram_post_id === postId);
  const postUrl = post ? post.original_url : `https://www.instagram.com/p/${postId}/`;

  const apifyToken = process.env.APIFY_TOKEN || process.env.PROVIDER_TOKEN;
  if (apifyToken && apifyToken !== 'your_apify_api_token_here' && postUrl) {
    try {
      const comments = await fetchApifyComments(postUrl, 20, apifyToken);
      return res.json({ post_id: postId, comments: comments });
    } catch (err) {
      console.error('Failed to fetch Apify comments:', err.message);
    }
  }

  res.json({ post_id: postId, comments: [] });
});

// 3. Hashtag & Search Volume Endpoint
app.get('/v1/hashtags/search', async (req, res) => {
  const hashtag = req.query.q || 'realestate';
  const apifyToken = process.env.APIFY_TOKEN || process.env.PROVIDER_TOKEN;

  if (apifyToken && apifyToken !== 'your_apify_api_token_here') {
    try {
      const result = await searchApifyHashtag(hashtag, apifyToken);
      return res.json(result);
    } catch (err) {
      console.error('Failed to search Apify hashtag:', err.message);
    }
  }

  res.json({ hashtag: `#${hashtag}`, total_posts: 0, posts: [] });
});

// 4. Place / Location Search Endpoint
app.get('/v1/locations/search', async (req, res) => {
  const query = req.query.q || 'Mumbai';
  const apifyToken = process.env.APIFY_TOKEN || process.env.PROVIDER_TOKEN;

  if (apifyToken && apifyToken !== 'your_apify_api_token_here') {
    try {
      const result = await searchApifyLocation(query, 10, apifyToken);
      return res.json(result);
    } catch (err) {
      console.error('Failed to search Apify location:', err.message);
    }
  }

  res.json({ location: query, count: 0, posts: [] });
});

app.get('/v1/posts/:id', (req, res) => {
  const feed = loadFeed();
  const post = feed.items.find(p => p.id === req.params.id || p.instagram_post_id === req.params.id);
  if (!post) return res.status(404).json({ code: 404, message: 'Post not found' });
  res.json(post);
});

app.get('/v1/channels', (req, res) => {
  const feed = loadFeed();
  const channelsMap = {};
  feed.items.forEach(p => {
    if (p.channel_id && !channelsMap[p.channel_id]) {
      channelsMap[p.channel_id] = { id: p.channel_id, instagram_handle: p.channel_id, display_name: p.channel_id, profile_url: null, source: p.source || 'apify', enabled: true };
    }
  });
  res.json(Object.values(channelsMap));
});

// ---- Bookmarks ----
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
  console.log(`Server listening on http://localhost:${PORT}`);
  if (process.env.APIFY_TOKEN && process.env.APIFY_TOKEN !== 'your_apify_api_token_here') {
    console.log(`[Apify Integration Active] Using API token: ${process.env.APIFY_TOKEN.slice(0, 12)}...`);
  } else {
    console.log('[Notice] Set APIFY_TOKEN in mock/.env to fetch live Instagram feeds.');
  }
});
