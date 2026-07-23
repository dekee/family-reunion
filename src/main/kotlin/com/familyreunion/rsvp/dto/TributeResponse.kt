package com.familyreunion.rsvp.dto

import java.time.LocalDateTime

data class TributeResponse(
    val id: Long,
    val siblingId: Long,
    val siblingName: String,
    val authorId: Long,
    val authorName: String,
    val story: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
