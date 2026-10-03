const { extractRealEstateFields } = require('./extractor');

const APIFY_BASE_URL = 'https://api.apify.com/v2/acts';

/**
 * Helper to call an Apify Actor synchronously and return dataset items.
 */
async function callApifyActor(actorSlug, input, apifyToken) {
  const token = apifyToken || process.env.APIFY_TOKEN || process.env.PROVIDER_TOKEN;
  if (!token || token === 'your_apify_api_token_here') {
    throw new Error('APIFY_TOKEN missing in mock/.env');
  }

  const url = `${APIFY_BASE_URL}/${actorSlug}/run-sync-get-dataset-items?token=${token}`;
  console.log(`[Apify Service] Calling actor ${actorSlug}...`);

  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Apify Actor ${actorSlug} failed (${response.status}): ${errText}`);
  }

  return await response.json();
}

/**
 * 1. Posts & Reels (from Profile usernames, direct URLs, or Hashtags)
 */
async function fetchApifyInstagramPosts({ usernames = [], hashtags = [], urls = [], limit = 10, apifyToken }) {
  const directUrls = [];

  if (Array.isArray(usernames)) {
    usernames.forEach(u => directUrls.push(u.startsWith('http') ? u : `https://www.instagram.com/${u}/`));
  }
  if (Array.isArray(hashtags)) {
    hashtags.forEach(h => {
      const tag = h.replace(/^#/, '');
      directUrls.push(`https://www.instagram.com/explore/tags/${tag}/`);
    });
  }
  if (Array.isArray(urls)) {
    urls.forEach(u => directUrls.push(u));
  }

  const input = {
    directUrls: directUrls.length > 0 ? directUrls : ['https://www.instagram.com/realtordotcom/'],
    resultsLimit: limit
  };

  const rawItems = await callApifyActor('apify~instagram-post-scraper', input, apifyToken);
  if (!Array.isArray(rawItems)) return [];

  return rawItems.map((item, idx) => transformApifyPost(item, idx));
}

/**
 * Real Estate Location Filter Fetcher
 * Combines City, Division, SubUrban, and PropertyType with predefined real estate hashtags.
 */
async function fetchApifyRealEstateByFilter({ city, division, subUrban, propertyType, limit = 15, apifyToken }) {
  const hashtags = ['realestate', 'propertyforsale'];

  if (city && city !== 'All Cities') {
    const cleanCity = city.replace(/[^a-zA-Z]/g, '').toLowerCase();
    hashtags.push(`${cleanCity}realestate`);
    hashtags.push(`${cleanCity}property`);
  }

  if (subUrban && subUrban !== 'All Areas') {
    const cleanArea = subUrban.replace(/[^a-zA-Z]/g, '').toLowerCase();
    hashtags.push(`${cleanArea}flats`);
    hashtags.push(`${cleanArea}realestate`);
  }

  if (propertyType && propertyType !== 'All Types') {
    const cleanType = propertyType.replace(/[^a-zA-Z]/g, '').toLowerCase();
    hashtags.push(`${cleanType}forsale`);
  }

  const posts = await fetchApifyInstagramPosts({ hashtags, limit, apifyToken });

  return posts.map(p => {
    if (p.extracted) {
      if (!p.extracted.location && (subUrban || city)) {
        p.extracted.location = [subUrban, city].filter(c => c && !c.startsWith('All')).join(', ');
      }
    }
    return p;
  });
}

/**
 * Profile Info
 */
async function fetchApifyProfileInfo(username, apifyToken) {
  const input = { usernames: [username] };
  try {
    const rawItems = await callApifyActor('apify~instagram-profile-scraper', input, apifyToken);
    if (!Array.isArray(rawItems) || rawItems.length === 0) return null;

    const p = rawItems[0];
    return {
      id: p.username || username,
      instagram_handle: p.username || username,
      display_name: p.fullName || p.username || username,
      profile_url: p.profilePicUrl || p.profilePicUrlHD || null,
      biography: p.biography || '',
      followers_count: p.followersCount || 0,
      following_count: p.followsCount || 0,
      posts_count: p.postsCount || 0,
      external_url: p.externalUrl || null,
      is_verified: p.isVerified || false,
      business_category: p.businessCategoryName || null,
      source: 'apify'
    };
  } catch (e) {
    console.error(`Failed to fetch profile for ${username}:`, e.message);
    return null;
  }
}

/**
 * Comments
 */
async function fetchApifyComments(postUrl, limit = 20, apifyToken) {
  const input = {
    directUrls: [postUrl],
    resultsLimit: limit
  };

  const rawItems = await callApifyActor('apify~instagram-comment-scraper', input, apifyToken);
  if (!Array.isArray(rawItems)) return [];

  return rawItems.map((c, idx) => ({
    id: c.id || `comment_${idx}`,
    post_url: postUrl,
    text: c.text || c.comment || '',
    owner_username: c.ownerUsername || c.owner?.username || 'anonymous',
    owner_profile_pic: c.ownerProfilePicUrl || c.owner?.profile_pic_url || null,
    likes_count: c.likesCount || 0,
    timestamp: c.timestamp || new Date().toISOString()
  }));
}

/**
 * Hashtag Search
 */
async function searchApifyHashtag(hashtag, apifyToken) {
  const cleanTag = hashtag.replace(/^#/, '');
  const posts = await fetchApifyInstagramPosts({ hashtags: [cleanTag], limit: 15, apifyToken });
  return {
    hashtag: `#${cleanTag}`,
    total_posts: posts.length,
    posts: posts
  };
}

/**
 * Location Search
 */
async function searchApifyLocation(locationQuery, limit = 10, apifyToken) {
  const posts = await fetchApifyInstagramPosts({ hashtags: [locationQuery.replace(/\s+/g, '')], limit: limit, apifyToken });
  const filtered = posts.filter(p => p.extracted && (p.extracted.location || p.extracted.property_type));
  return {
    location: locationQuery,
    count: filtered.length,
    posts: filtered.length > 0 ? filtered : posts
  };
}

function transformApifyPost(item, idx) {
  const caption = item.caption || item.text || item.captionText || '';
  const extracted = extractRealEstateFields(caption);

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
    channel_id: item.ownerUsername || item.ownerId || 'instagram_user',
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
}

module.exports = {
  fetchApifyInstagramPosts,
  fetchApifyRealEstateByFilter,
  fetchApifyProfileInfo,
  fetchApifyComments,
  searchApifyHashtag,
  searchApifyLocation
};
