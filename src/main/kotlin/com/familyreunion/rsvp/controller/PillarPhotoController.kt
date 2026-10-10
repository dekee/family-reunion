package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.PillarPhotoInfo
import com.familyreunion.rsvp.security.IpRateLimiter
import com.familyreunion.rsvp.service.PillarPhotoService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Portraits for the Pillars of Our Family page. Uploads use the shared family gallery password. */
@RestController
@RequestMapping("/api/tributes/photos")
class PillarPhotoController(
    private val photoService: PillarPhotoService,
    private val rateLimiter: IpRateLimiter,
    @Value("\${app.gallery.upload-password}") private val uploadPassword: String
) {

    @GetMapping
    fun listPhotos(): ResponseEntity<List<PillarPhotoInfo>> =
        ResponseEntity.ok(photoService.listVersions())

    // The frontend requests `?v=<version>`, so each URL's bytes never change and can be cached forever.
    @GetMapping("/{siblingId}")
    fun getPhoto(@PathVariable siblingId: Long): ResponseEntity<ByteArray> {
        val photo = photoService.get(siblingId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.contentType))
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
            .body(photo.data)
    }

    @PostMapping("/{siblingId}")
    fun uploadPhoto(
        @PathVariable siblingId: Long,
        @RequestParam password: String,
        @RequestParam("file") file: MultipartFile,
        httpRequest: HttpServletRequest
    ): ResponseEntity<PillarPhotoInfo> {
        val ip = IpRateLimiter.clientIp(httpRequest)
        if (!rateLimiter.tryAcquire("pillar-photo:$ip", maxRequests = 10, windowSeconds = 600)) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many uploads. Please try again later.")
        }
        if (uploadPassword.isBlank() ||
            !MessageDigest.isEqual(password.toByteArray(), uploadPassword.toByteArray())) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect upload password")
        }
        val contentType = file.contentType
        if (file.isEmpty || contentType == null || !contentType.startsWith("image/")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files can be uploaded")
        }
        if (file.size > MAX_BYTES) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be 5 MB or smaller")
        }
        return ResponseEntity.ok(photoService.save(siblingId, contentType, file.bytes))
    }

    @DeleteMapping("/{siblingId}")
    fun deletePhoto(@PathVariable siblingId: Long): ResponseEntity<Void> {
        photoService.delete(siblingId)
        return ResponseEntity.noContent().build()
    }

    companion object {
        private const val MAX_BYTES = 5L * 1024 * 1024
    }
}
