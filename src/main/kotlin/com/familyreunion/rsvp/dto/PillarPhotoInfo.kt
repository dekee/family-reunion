package com.familyreunion.rsvp.dto

/** `version` changes whenever the photo is replaced; the frontend uses it to bust the image cache. */
data class PillarPhotoInfo(
    val siblingId: Long,
    val version: Long
)
