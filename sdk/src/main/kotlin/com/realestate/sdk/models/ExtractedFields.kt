package com.realestate.sdk.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExtractedFields(
    val property_type: String? = null,
    val price: PriceObject? = null,
    val location: String? = null,
    val area: String? = null
)
