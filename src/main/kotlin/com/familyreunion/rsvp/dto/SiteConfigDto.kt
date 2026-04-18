package com.familyreunion.rsvp.dto

data class SiteConfigResponse(
    val settings: Map<String, String>,
    val imageUrls: Map<String, String>
)

data class SiteConfigUpdateRequest(
    val settings: Map<String, String>
)
