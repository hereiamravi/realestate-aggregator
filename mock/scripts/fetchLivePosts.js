const { fetchApifyInstagramPosts } = require('../services/apifyProvider');
const fs = require('fs');
const path = require('path');

// Simple .env loader
const ENV_FILE = path.join(__dirname, '../.env');
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

const FEED_FILE = path.join(__dirname, '../data/feed.json');

async function run() {
  const token = process.env.APIFY_TOKEN || process.env.PROVIDER_TOKEN;
  console.log('--- Apify Instagram Live Fetcher ---');

  if (!token || token === 'your_apify_api_token_here') {
    console.error('Error: Please set APIFY_TOKEN in mock/.env first!');
    process.exit(1);
  }

  try {
    const handles = ['realtordotcom', 'realestate'];
    console.log(`Connecting to Apify with token: ${token.slice(0, 10)}... for handles: ${handles.join(', ')}`);
    const posts = await fetchApifyInstagramPosts(handles, 10, token);

    if (posts && posts.length > 0) {
      console.log(`\nSuccessfully fetched ${posts.length} live Instagram posts!`);
      const feedData = {
        items: posts,
        next_cursor: null,
        page_size: posts.length
      };

      fs.writeFileSync(FEED_FILE, JSON.stringify(feedData, null, 2), 'utf8');
      console.log(`Updated ${FEED_FILE} with real live Instagram listings!`);
    } else {
      console.log('Apify returned 0 posts or actor run is processing.');
    }
  } catch (err) {
    console.error('Fetch failed:', err.message);
  }
}

run();
