package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.GalleryPhoto
import com.familyreunion.rsvp.dto.GalleryResponse
import com.familyreunion.rsvp.service.GalleryService
import com.google.api.services.drive.Drive
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "google.drive.credentials-file=test-credentials.json",
        "google.drive.folder-id=test-folder",
        "app.gallery.upload-password=tumblin2026"
    ]
)
@AutoConfigureMockMvc
class GalleryControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @MockitoBean
    private lateinit var drive: Drive

    @MockitoBean
    private lateinit var galleryService: GalleryService

    private val samplePhoto = GalleryPhoto(
        id = "file-1",
        name = "pic.jpg",
        thumbnailUrl = "/api/gallery/photo/file-1?size=thumbnail",
        fullUrl = "/api/gallery/photo/file-1",
        width = 800,
        height = 600,
        createdTime = "2026-08-05T00:00:00.000Z",
        dateTaken = "2026-08-01T12:00:00"
    )

    private fun imageFile(name: String = "pic.jpg", contentType: String = "image/jpeg") =
        MockMultipartFile("files", name, contentType, byteArrayOf(1, 2, 3))

    @Test
    fun `GET gallery should be public`() {
        whenever(galleryService.getPhotos(anyOrNull(), any()))
            .thenReturn(GalleryResponse(photos = listOf(samplePhoto), nextPageToken = null, totalCount = 1))

        mockMvc.perform(get("/api/gallery"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalCount").value(1))
    }

    @Test
    fun `upload with correct password should succeed without auth`() {
        whenever(galleryService.uploadPhoto(any(), any(), any())).thenReturn(samplePhoto)

        mockMvc.perform(
            multipart("/api/gallery/upload")
                .file(imageFile())
                .param("password", "tumblin2026")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.uploaded").value(1))
            .andExpect(jsonPath("$.photos[0].id").value("file-1"))

        verify(galleryService).uploadPhoto(any(), any(), any())
    }

    @Test
    fun `upload with wrong password should return 403`() {
        mockMvc.perform(
            multipart("/api/gallery/upload")
                .file(imageFile())
                .param("password", "wrong-password")
        )
            .andExpect(status().isForbidden)

        verify(galleryService, never()).uploadPhoto(any(), any(), any())
    }

    @Test
    fun `upload without password should be rejected`() {
        mockMvc.perform(
            multipart("/api/gallery/upload")
                .file(imageFile())
        )
            .andExpect(status().isBadRequest)

        verify(galleryService, never()).uploadPhoto(any(), any(), any())
    }

    @Test
    fun `upload of non-image file should return 400`() {
        mockMvc.perform(
            multipart("/api/gallery/upload")
                .file(imageFile(name = "notes.pdf", contentType = "application/pdf"))
                .param("password", "tumblin2026")
        )
            .andExpect(status().isBadRequest)

        verify(galleryService, never()).uploadPhoto(any(), any(), any())
    }

    @Test
    fun `gallery refresh should still require admin auth`() {
        mockMvc.perform(multipart("/api/gallery/refresh"))
            .andExpect(status().isUnauthorized)
    }
}
