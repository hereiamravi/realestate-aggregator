const { extractRealEstateFields } = require('./extractor');

/**
 * Apify Instagram Scraper Service
 * Fetches real Instagram posts using the Apify Instagram Scraper API actor.
 */
async function fetchApifyInstagramPosts(usernames = ['realtordotcom'], limit = 10, apifyToken) {
  const token = apifyToken || process.env.APIFY_TOKEN || process.env.PROVIDER_TOKEN;

  if (!token) {
    throw new Error('Apify API token missing. Set APIFY_TOKEN in .env or environment variables.');
  }

  const handleList = Array.isArray(usernames) ? usernames : [usernames];
  const directUrls = handleList.map(u => u.startsWith('http') ? u : `https://www.instagram.com/${u}/`);

  // Try apify~instagram-post-scraper or fallback actor endpoints
  const url = `https://api.apify.com/v2/acts/apify~instagram-post-scraper/run-sync-get-dataset-items?token=${token}`;

  const requestBody = {
    username: handleList,
    directUrls: directUrls,
    resultsLimit: limit
  };

  console.log(`[Apify API] Connecting to Apify Actor for handles: ${handleList.join(', ')}...`);

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(requestBody)
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Apify API call failed (${response.status}): ${errText}`);
  }

  const rawItems = await response.json();

  if (!Array.isArray(rawItems)) {
    console.warn('[Apify API] Raw response is not an array:', rawItems);
    return [];
  }

  // Transform raw Apify dataset items to standardized Post schema
  return rawItems.map((item, idx) => {
    const caption = item.caption || item.text || item.captionText || '';
    const extracted = extractRealEstateFields(caption);

    // Build media list
    const media = [];
    const mainImageUrl = item.displayUrl || item.imageUrl || item.url || item.thumbnailUrl;
    if (mainImageUrl) {
      media.push({
        url: mainImageUrl,
        type: item.type === 'Video' || item.isVideo ? 'video' : 'image',
        width: item.dimensionsHeight || 1080,
        height: item.dimensionsWidth || 1080
      });
    }

    // Additional images if carousel
    if (Array.isArray(item.images)) {
      item.images.forEach(imgUrl => {
        if (imgUrl && !media.some(m => m.url === imgUrl)) {
          media.push({ url: imgUrl, type: 'image' });
        }
      });
    }

    const shortcode = item.shortCode || item.code || '';
    const postId = item.id || shortcode || `apify_${Date.now()}_${idx}`;

    return {
      id: postId,
      instagram_post_id: item.id || null,
      shortcode: shortcode,
      channel_id: item.ownerUsername || item.ownerId || handleList[0],
      original_url: item.url || (shortcode ? `https://www.instagram.com/p/${shortcode}/` : null),
      caption: caption,
      media: media,
      media_type: item.type === 'Video' || item.isVideo ? 'video' : 'image',
      timestamp: item.timestamp || new Date().toISOString(),
      likes_count: item.likesCount || item.likes || 0,
      comments_count: item.commentsCount || item.comments || 0,
      extracted: extracted,
      source: 'apify',
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
      is_removed: false
    };
  });
}

module.exports = { fetchApifyInstagramPosts };
