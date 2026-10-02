/**
 * Smart Real Estate Caption Extractor
 * Parses property type, price (raw & numeric), location, and area from Instagram post captions.
 */

function extractRealEstateFields(caption = '') {
  if (!caption) {
    return {
      property_type: 'property',
      price: null,
      location: null,
      area: null
    };
  }

  // 1. Property Type
  let propertyType = 'property';
  const textLower = caption.toLowerCase();
  if (textLower.includes('plot') || textLower.includes('land') || textLower.includes('site')) {
    propertyType = 'plots';
  } else if (textLower.includes('bhk') || textLower.includes('flat') || textLower.includes('apartment')) {
    propertyType = 'flats';
  } else if (textLower.includes('villa') || textLower.includes('house') || textLower.includes('bungalow')) {
    propertyType = 'villas';
  } else if (textLower.includes('commercial') || textLower.includes('office') || textLower.includes('shop')) {
    propertyType = 'commercial';
  }

  // 2. Price Extraction
  let price = null;
  // Match patterns like ₹25L, ₹1.5 Cr, Rs. 50 Lakhs, $500,000, 50 Lacs
  const priceRegex = /(?:₹|rs\.?|\$)\s*([\d,]+(?:\.\d+)?)\s*(cr|crore|crores|l|lakh|lakhs|lac|lacs|k|thousand|m|million)?\b/i;
  const priceMatch = caption.match(priceRegex);

  if (priceMatch) {
    const rawPrice = priceMatch[0].trim();
    const rawNum = parseFloat(priceMatch[1].replace(/,/g, ''));
    const unit = (priceMatch[2] || '').toLowerCase();
    const isINR = rawPrice.startsWith('₹') || rawPrice.toLowerCase().startsWith('rs');
    const currency = isINR ? 'INR' : rawPrice.startsWith('$') ? 'USD' : 'INR';

    let numericValue = rawNum;
    if (unit.startsWith('cr')) {
      numericValue = rawNum * 10000000;
    } else if (unit.startsWith('l') || unit.startsWith('lac')) {
      numericValue = rawNum * 100000;
    } else if (unit.startsWith('k')) {
      numericValue = rawNum * 1000;
    } else if (unit.startsWith('m')) {
      numericValue = rawNum * 1000000;
    }

    price = {
      raw: rawPrice,
      value: numericValue,
      currency: currency
    };
  }

  // 3. Location Extraction
  let location = null;
  const locationMatch = caption.match(/(?:near|at|in|location:?)\s+([A-Z][a-zA-Z0-9\s]+?)(?=[,\.\n\r]|$)/i);
  if (locationMatch && locationMatch[1].trim().length < 30) {
    location = locationMatch[1].trim();
  }

  // 4. Area Extraction
  let area = null;
  const areaMatch = caption.match(/(\d+(?:\.\d+)?\s*(?:sq\.?\s*ft|sq\.?\s*yd|sq\s*yards?|cents?|acres?))/i);
  if (areaMatch) {
    area = areaMatch[1].trim();
  }

  return {
    property_type: propertyType,
    price: price,
    location: location,
    area: area
  };
}

module.exports = { extractRealEstateFields };
