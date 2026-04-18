package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.SiteConfigResponse
import com.familyreunion.rsvp.dto.SiteConfigUpdateRequest
import com.familyreunion.rsvp.service.SiteConfigService
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.TimeUnit

@RestController
class SiteConfigController(private val siteConfigService: SiteConfigService) {

    @GetMapping("/api/site-config")
    fun getConfig(): SiteConfigResponse {
        return siteConfigService.getAllConfig()
    }

    @PutMapping("/api/admin/site-config")
    fun updateConfig(@RequestBody request: SiteConfigUpdateRequest): ResponseEntity<Void> {
        siteConfigService.updateConfig(request.settings)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/api/admin/site-config/images/{key}")
    fun uploadImage(
        @PathVariable key: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Void> {
        siteConfigService.uploadImage(
            key = key,
            contentType = file.contentType ?: "application/octet-stream",
            data = file.bytes
        )
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/api/admin/site-config/images/{key}")
    fun deleteImage(@PathVariable key: String): ResponseEntity<Void> {
        siteConfigService.deleteImage(key)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/site-config/images/{key}")
    fun getImage(@PathVariable key: String): ResponseEntity<ByteArray> {
        val image = siteConfigService.getImage(key)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType))
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .body(image.data)
    }
}
