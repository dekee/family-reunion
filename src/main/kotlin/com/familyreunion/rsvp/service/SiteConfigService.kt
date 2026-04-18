package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.SiteConfigResponse
import com.familyreunion.rsvp.model.SiteConfig
import com.familyreunion.rsvp.model.SiteImage
import com.familyreunion.rsvp.repository.SiteConfigRepository
import com.familyreunion.rsvp.repository.SiteImageRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class SiteConfigService(
    private val configRepository: SiteConfigRepository,
    private val imageRepository: SiteImageRepository
) {
    private val cache = ConcurrentHashMap<String, String>()

    companion object {
        val ALLOWED_IMAGE_KEYS = setOf("heroPhoto", "founderPhoto", "angelBackground")
        val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        const val MAX_IMAGE_SIZE = 2 * 1024 * 1024 // 2MB
    }

    @PostConstruct
    fun loadCache() {
        configRepository.findAll().forEach { cache[it.key] = it.value }
    }

    fun getAllConfig(): SiteConfigResponse {
        val settings = cache.toMap()
        val imageUrls = imageRepository.findAll()
            .associate { it.key to "/api/site-config/images/${it.key}" }
        return SiteConfigResponse(settings, imageUrls)
    }

    fun updateConfig(updates: Map<String, String>) {
        updates.forEach { (key, value) ->
            val entity = configRepository.findById(key).orElse(SiteConfig(key = key))
            entity.value = value
            entity.updatedAt = LocalDateTime.now()
            configRepository.save(entity)
            cache[key] = value
        }
    }

    fun uploadImage(key: String, contentType: String, data: ByteArray) {
        require(key in ALLOWED_IMAGE_KEYS) { "Invalid image key: $key. Allowed: $ALLOWED_IMAGE_KEYS" }
        require(contentType in ALLOWED_CONTENT_TYPES) { "Invalid content type: $contentType. Allowed: $ALLOWED_CONTENT_TYPES" }
        require(data.size <= MAX_IMAGE_SIZE) { "Image too large. Max size: ${MAX_IMAGE_SIZE / 1024 / 1024}MB" }

        val entity = imageRepository.findById(key).orElse(SiteImage(key = key))
        entity.contentType = contentType
        entity.data = data
        entity.updatedAt = LocalDateTime.now()
        imageRepository.save(entity)
    }

    fun getImage(key: String): SiteImage? {
        return imageRepository.findById(key).orElse(null)
    }

    fun deleteImage(key: String) {
        imageRepository.deleteById(key)
    }

    fun getResolvedValue(key: String, default: String): String {
        return cache[key] ?: default
    }
}
