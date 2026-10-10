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
        "google.drive.memorial-folder-id=memorial-folder",
        "app.gallery.upload-password=tumblin2026"
    ]
)
@AutoConfigureMockMvc
class MemorialGalleryControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @MockitoBean
    private lateinit var drive: Drive

    @MockitoBean(name = "galleryService")
    private lateinit var galleryService: GalleryService

    @MockitoBean(name = "memorialGalleryService")
    private lateinit var memorialGalleryService: GalleryService

    private val memorialPhoto = GalleryPhoto(
        id = "mem-1",
        name = "grandma.jpg",
        thumbnailUrl = "/api/gallery/memorial/photo/mem-1?size=thumbnail",
        fullUrl = "/api/gallery/memorial/photo/mem-1",
        width = 800,
        height = 600,
        createdTime = "2026-08-05T00:00:00.000Z",
        dateTaken = null
    )

    @Test
    fun `GET memorial album should be public and served from the memorial folder`() {
        whenever(memorialGalleryService.getPhotos(anyOrNull(), any()))
            .thenReturn(GalleryResponse(photos = listOf(memorialPhoto), nextPageToken = null, totalCount = 1))

        mockMvc.perform(get("/api/gallery/memorial"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.photos[0].id").value("mem-1"))

        verify(galleryService, never()).getPhotos(anyOrNull(), any())
    }

    @Test
    fun `memorial upload with correct password should go to the memorial album`() {
        whenever(memorialGalleryService.uploadPhoto(any(), any(), any())).thenReturn(memorialPhoto)

        mockMvc.perform(
            multipart("/api/gallery/memorial/upload")
                .file(MockMultipartFile("files", "grandma.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))
                .param("password", "tumblin2026")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.uploaded").value(1))

        verify(memorialGalleryService).uploadPhoto(any(), any(), any())
        verify(galleryService, never()).uploadPhoto(any(), any(), any())
    }

    @Test
    fun `memorial upload with wrong password should return 403`() {
        mockMvc.perform(
            multipart("/api/gallery/memorial/upload")
                .file(MockMultipartFile("files", "grandma.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))
                .param("password", "wrong-password")
        )
            .andExpect(status().isForbidden)

        verify(memorialGalleryService, never()).uploadPhoto(any(), any(), any())
    }

    @Test
    fun `memorial refresh should require admin auth`() {
        mockMvc.perform(multipart("/api/gallery/memorial/refresh"))
            .andExpect(status().isUnauthorized)
    }
}
