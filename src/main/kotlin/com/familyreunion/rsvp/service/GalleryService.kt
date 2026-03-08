package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.GalleryPhoto
import com.familyreunion.rsvp.dto.GalleryResponse
import com.google.api.services.drive.Drive
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty("google.drive.credentials-file")
class GalleryService(
    private val drive: Drive,
    @Value("\${google.drive.folder-id}") private val folderId: String
) {
    private val logger = LoggerFactory.getLogger(GalleryService::class.java)

    // Cache photos for 5 minutes to avoid hammering the Drive API
    private var cachedPhotos: List<GalleryPhoto> = emptyList()
    private var cacheExpiry: Long = 0
    private val cacheTtlMs = 5 * 60 * 1000L

    fun getPhotos(pageToken: String?, pageSize: Int = 50): GalleryResponse {
        val allPhotos = loadPhotos()

        // Simple pagination over cached results
        val startIndex = if (pageToken != null) pageToken.toIntOrNull() ?: 0 else 0
        val endIndex = minOf(startIndex + pageSize, allPhotos.size)
        val page = allPhotos.subList(startIndex, endIndex)
        val nextToken = if (endIndex < allPhotos.size) endIndex.toString() else null

        return GalleryResponse(
            photos = page,
            nextPageToken = nextToken,
            totalCount = allPhotos.size
        )
    }

    private fun loadPhotos(): List<GalleryPhoto> {
        if (System.currentTimeMillis() < cacheExpiry && cachedPhotos.isNotEmpty()) {
            return cachedPhotos
        }

        logger.info("Refreshing gallery cache from Google Drive folder: {}", folderId)
        val photos = mutableListOf<GalleryPhoto>()
        var drivePageToken: String? = null

        do {
            val result = drive.files().list()
                .setQ("'$folderId' in parents and mimeType contains 'image/' and trashed = false")
                .setFields("nextPageToken, files(id, name, imageMediaMetadata, createdTime, thumbnailLink)")
                .setPageSize(100)
                .setOrderBy("createdTime desc")
                .setPageToken(drivePageToken)
                .execute()

            result.files?.forEach { file ->
                photos.add(
                    GalleryPhoto(
                        id = file.id,
                        name = file.name,
                        thumbnailUrl = "/api/gallery/photo/${file.id}?size=thumbnail",
                        fullUrl = "/api/gallery/photo/${file.id}",
                        width = file.imageMediaMetadata?.width,
                        height = file.imageMediaMetadata?.height,
                        createdTime = file.createdTime?.toStringRfc3339()
                    )
                )
            }

            drivePageToken = result.nextPageToken
        } while (drivePageToken != null)

        cachedPhotos = photos
        cacheExpiry = System.currentTimeMillis() + cacheTtlMs
        logger.info("Gallery cache refreshed: {} photos", photos.size)
        return photos
    }

    fun getPhotoStream(fileId: String, thumbnail: Boolean): Pair<ByteArray, String> {
        val file = drive.files().get(fileId)
            .setFields("id, name, mimeType, thumbnailLink")
            .execute()

        if (thumbnail) {
            // Get a resized version using Drive's thumbnail export
            val stream = drive.files().get(fileId).executeMediaAsInputStream()
            return Pair(stream.readBytes(), file.mimeType ?: "image/jpeg")
        }

        val stream = drive.files().get(fileId).executeMediaAsInputStream()
        return Pair(stream.readBytes(), file.mimeType ?: "image/jpeg")
    }

    fun clearCache() {
        cachedPhotos = emptyList()
        cacheExpiry = 0
        logger.info("Gallery cache cleared")
    }
}
