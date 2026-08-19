package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.GalleryResponse
import com.familyreunion.rsvp.service.GalleryService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/gallery")
@ConditionalOnProperty("google.drive.credentials-file")
class GalleryController(
    private val galleryService: GalleryService,
    @Value("\${app.gallery.upload-password}") private val uploadPassword: String
) {

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

    @PostMapping("/upload")
    fun uploadPhotos(
        @RequestParam password: String,
        @RequestParam("files") files: List<MultipartFile>
    ): ResponseEntity<Map<String, Any>> {
        if (!MessageDigest.isEqual(password.toByteArray(), uploadPassword.toByteArray())) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect upload password")
        }
        if (files.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided")
        }
        files.forEach { file ->
            if (file.isEmpty || file.contentType?.startsWith("image/") != true) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only image files can be uploaded (${file.originalFilename ?: "unnamed file"})"
                )
            }
        }

        val uploaded = files.map { file ->
            galleryService.uploadPhoto(
                filename = file.originalFilename ?: "photo.jpg",
                contentType = file.contentType ?: "image/jpeg",
                bytes = file.bytes
            )
        }
        return ResponseEntity.ok(mapOf("uploaded" to uploaded.size, "photos" to uploaded))
    }

    @PostMapping("/refresh")
    fun refreshCache(): ResponseEntity<Map<String, String>> {
        galleryService.clearCache()
        return ResponseEntity.ok(mapOf("message" to "Cache cleared"))
    }
}
