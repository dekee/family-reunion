package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.GalleryResponse
import com.familyreunion.rsvp.service.GalleryService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/gallery")
@ConditionalOnProperty("google.drive.credentials-file")
class GalleryController(private val galleryService: GalleryService) {

    @GetMapping
    fun getPhotos(
        @RequestParam(required = false) pageToken: String?,
        @RequestParam(defaultValue = "50") pageSize: Int
    ): ResponseEntity<GalleryResponse> {
        return ResponseEntity.ok(galleryService.getPhotos(pageToken, pageSize))
    }

    @GetMapping("/photo/{fileId}")
    fun getPhoto(
        @PathVariable fileId: String,
        @RequestParam(required = false) size: String?
    ): ResponseEntity<ByteArray> {
        val thumbnail = size == "thumbnail"
        val (bytes, mimeType) = galleryService.getPhotoStream(fileId, thumbnail)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(mimeType))
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .body(bytes)
    }

    @PostMapping("/refresh")
    fun refreshCache(): ResponseEntity<Map<String, String>> {
        galleryService.clearCache()
        return ResponseEntity.ok(mapOf("message" to "Cache cleared"))
    }
}
