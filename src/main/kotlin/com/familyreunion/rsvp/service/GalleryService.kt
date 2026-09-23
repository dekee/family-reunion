package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.GalleryPhoto
import com.familyreunion.rsvp.dto.GalleryResponse
import com.google.api.client.http.GenericUrl
import com.google.api.services.drive.Drive
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

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

    /** Drive-generated thumbnail URL per file id, captured while listing the folder. */
    private var thumbnailLinks: Map<String, String> = emptyMap()

    /** Rendered thumbnails, so the grid doesn't re-fetch from Drive on every view. */
    private val thumbnailCache = ThumbnailCache()

    /**
     * Width Drive is asked to render thumbnails at.
     *
     * Width, not longest side: the grid is a masonry column layout, so width is the constraint and
     * height flows. Sizing the longest side leaves tall photos too narrow — a 1290x2796 phone photo
     * came out 184px wide and got upscaled into a ~291px tile.
     *
     * 291px is the widest tile (1200px page, 4 columns); 600 covers that at 2x for retina.
     */
    private val thumbnailPx = 600

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
        val links = mutableMapOf<String, String>()
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
                file.thumbnailLink?.let { links[file.id] = it }
                photos.add(
                    GalleryPhoto(
                        id = file.id,
                        name = file.name,
                        thumbnailUrl = "/api/gallery/photo/${file.id}?size=thumbnail",
                        fullUrl = "/api/gallery/photo/${file.id}",
                        width = file.imageMediaMetadata?.width,
                        height = file.imageMediaMetadata?.height,
                        createdTime = file.createdTime?.toStringRfc3339(),
                        dateTaken = parseExifTime(file.imageMediaMetadata?.time)
                    )
                )
            }

            drivePageToken = result.nextPageToken
        } while (drivePageToken != null)

        cachedPhotos = photos
        thumbnailLinks = links
        cacheExpiry = System.currentTimeMillis() + cacheTtlMs
        logger.info("Gallery cache refreshed: {} photos", photos.size)
        return photos
    }

    fun getPhotoStream(fileId: String, thumbnail: Boolean): Pair<ByteArray, String> {
        // Serve a cached thumbnail before doing any Drive work at all. Safe to check first: an id
        // only ever enters this cache after passing the folder check below.
        if (thumbnail) {
            thumbnailCache.get(fileId)?.let { return Pair(it, THUMBNAIL_MIME) }
        }

        requireInGalleryFolder(fileId)

        if (thumbnail) {
            renderThumbnail(fileId)?.let { bytes ->
                thumbnailCache.put(fileId, bytes)
                return Pair(bytes, THUMBNAIL_MIME)
            }
            // No Drive thumbnail (unsupported type, or the link failed) — fall through and serve
            // the original rather than showing the visitor a broken tile.
            logger.warn("No thumbnail available for {}, serving the original instead", fileId)
        }

        val mimeType = fileMimeType(fileId)
        val stream = drive.files().get(fileId).executeMediaAsInputStream()
        return Pair(stream.readBytes(), mimeType)
    }

    /**
     * Prevent IDOR: this endpoint is public, so only ever serve images that live directly in the
     * configured gallery folder. Without this check, any Drive file readable by the service
     * account could be exfiltrated via a guessed or leaked fileId.
     *
     * The cached folder listing is the fast path. A miss falls back to asking Drive, so a photo
     * added to the folder out of band still resolves before the 5-minute listing cache expires.
     */
    private fun requireInGalleryFolder(fileId: String) {
        if (cachedPhotos.any { it.id == fileId } && System.currentTimeMillis() < cacheExpiry) return

        val file = drive.files().get(fileId)
            .setFields("id, mimeType, parents")
            .execute()
        if (folderId !in (file.parents ?: emptyList())) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found")
        }
        if (file.mimeType?.startsWith("image/") != true) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found")
        }
    }

    private fun fileMimeType(fileId: String): String =
        drive.files().get(fileId).setFields("mimeType").execute().mimeType ?: "image/jpeg"

    /**
     * Fetches Drive's own rendered thumbnail. Google has already generated it, so this costs us no
     * image processing and works for formats the JVM cannot decode itself (HEIC, for instance).
     * The request goes through the Drive client's request factory so it carries our credentials —
     * the folder is private, and the raw link is not publicly fetchable.
     */
    private fun renderThumbnail(fileId: String): ByteArray? {
        val link = thumbnailLinks[fileId] ?: run {
            loadPhotos()                 // link may simply be missing from a stale listing
            thumbnailLinks[fileId]
        } ?: return null

        return try {
            val response = drive.requestFactory
                .buildGetRequest(GenericUrl(resizeLink(link, thumbnailPx)))
                .execute()
            response.content.use { it.readBytes() }.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            logger.warn("Thumbnail fetch failed for {}: {}", fileId, e.message)
            null
        }
    }




    fun uploadPhoto(filename: String, contentType: String, bytes: ByteArray): GalleryPhoto {
        val metadata = com.google.api.services.drive.model.File().apply {
            name = filename
            parents = listOf(folderId)
        }
        val content = com.google.api.client.http.ByteArrayContent(contentType, bytes)

        val uploaded = drive.files().create(metadata, content)
            .setFields("id, name, imageMediaMetadata, createdTime")
            .setSupportsAllDrives(true)
            .execute()

        logger.info("Uploaded photo '{}' to gallery folder as file {}", filename, uploaded.id)
        clearCache()

        return GalleryPhoto(
            id = uploaded.id,
            name = uploaded.name,
            thumbnailUrl = "/api/gallery/photo/${uploaded.id}?size=thumbnail",
            fullUrl = "/api/gallery/photo/${uploaded.id}",
            width = uploaded.imageMediaMetadata?.width,
            height = uploaded.imageMediaMetadata?.height,
            createdTime = uploaded.createdTime?.toStringRfc3339(),
            dateTaken = parseExifTime(uploaded.imageMediaMetadata?.time)
        )
    }

    // Drive reports EXIF date-taken as "yyyy:MM:dd HH:mm:ss"; convert to ISO-8601
    private fun parseExifTime(exifTime: String?): String? {
        if (exifTime.isNullOrBlank()) return null
        return try {
            java.time.LocalDateTime
                .parse(exifTime, java.time.format.DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"))
                .toString()
        } catch (e: java.time.format.DateTimeParseException) {
            null
        }
    }

    fun clearCache() {
        cachedPhotos = emptyList()
        thumbnailLinks = emptyMap()
        cacheExpiry = 0
        thumbnailCache.clear()
        logger.info("Gallery cache cleared")
    }

    companion object {
        /** Drive renders thumbnails as JPEG regardless of the original's format. */
        private const val THUMBNAIL_MIME = "image/jpeg"
        private val SIZE_SUFFIX = Regex("=[swh]\\d+(-c)?$")

        /**
         * Drive thumbnail links end in a size hint such as "=s220". Rewrite it to "=w<px>", which
         * constrains width rather than the longest side — see [thumbnailPx].
         */
        internal fun resizeLink(link: String, px: Int): String =
            if (SIZE_SUFFIX.containsMatchIn(link)) SIZE_SUFFIX.replace(link, "=w$px") else "$link=w$px"
    }
}
