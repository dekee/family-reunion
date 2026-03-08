package com.familyreunion.rsvp.dto

data class GalleryPhoto(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val fullUrl: String,
    val width: Int?,
    val height: Int?,
    val createdTime: String?
)

data class GalleryResponse(
    val photos: List<GalleryPhoto>,
    val nextPageToken: String?,
    val totalCount: Int
)
