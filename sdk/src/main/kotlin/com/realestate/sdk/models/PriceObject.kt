package com.realestate.sdk.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PriceObject(
    val raw: String? = null,
    val value: Double? = null,
    val currency: String? = null
)
