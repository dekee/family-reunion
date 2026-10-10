package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(properties = ["app.gallery.upload-password=tumblin2026"])
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PillarPhotoControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val familyMemberRepository: FamilyMemberRepository
) {

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3)

    private fun seedFamily(): Pair<FamilyMember, FamilyMember> {
        val founder = familyMemberRepository.save(FamilyMember(name = "Wesley Tumblin", isFounder = true))
        val pillar = familyMemberRepository.save(FamilyMember(name = "Cheryl Johnson", parent = founder))
        return founder to pillar
    }

    private fun upload(siblingId: Long, password: String = "tumblin2026", file: MockMultipartFile =
        MockMultipartFile("file", "cheryl.jpg", "image/jpeg", jpeg)) =
        mockMvc.perform(multipart("/api/tributes/photos/$siblingId").file(file).param("password", password))

    @Test
    fun `upload with the family password stores the photo and lists its version`() {
        val (_, pillar) = seedFamily()

        upload(pillar.id)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.siblingId").value(pillar.id))
            .andExpect(jsonPath("$.version").isNumber)

        mockMvc.perform(get("/api/tributes/photos"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].siblingId").value(pillar.id))

        mockMvc.perform(get("/api/tributes/photos/${pillar.id}"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "image/jpeg"))
            .andExpect(content().bytes(jpeg))
    }

    @Test
    fun `uploading again replaces the photo`() {
        val (_, pillar) = seedFamily()
        upload(pillar.id).andExpect(status().isOk)
        val png = byteArrayOf(9, 8, 7)
        upload(pillar.id, file = MockMultipartFile("file", "new.png", "image/png", png)).andExpect(status().isOk)

        mockMvc.perform(get("/api/tributes/photos")).andExpect(jsonPath("$", hasSize<Any>(1)))
        mockMvc.perform(get("/api/tributes/photos/${pillar.id}"))
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(content().bytes(png))
    }

    @Test
    fun `wrong password is rejected`() {
        val (_, pillar) = seedFamily()
        upload(pillar.id, password = "nope").andExpect(status().isForbidden)
    }

    @Test
    fun `non-image files are rejected`() {
        val (_, pillar) = seedFamily()
        upload(pillar.id, file = MockMultipartFile("file", "notes.txt", "text/plain", "hi".toByteArray()))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `oversized files are rejected`() {
        val (_, pillar) = seedFamily()
        upload(pillar.id, file = MockMultipartFile("file", "big.jpg", "image/jpeg", ByteArray(5 * 1024 * 1024 + 1)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `photos can only be attached to pillars`() {
        val (founder, pillar) = seedFamily()
        val grandchild = familyMemberRepository.save(FamilyMember(name = "Derrick Johnson", parent = pillar))

        upload(founder.id).andExpect(status().isBadRequest)
        upload(grandchild.id).andExpect(status().isBadRequest)
        upload(999999).andExpect(status().isNotFound)
    }

    @Test
    fun `missing photo returns 404`() {
        val (_, pillar) = seedFamily()
        mockMvc.perform(get("/api/tributes/photos/${pillar.id}")).andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admin can remove a photo`() {
        val (_, pillar) = seedFamily()
        upload(pillar.id).andExpect(status().isOk)

        mockMvc.perform(delete("/api/tributes/photos/${pillar.id}")).andExpect(status().isNoContent)
        mockMvc.perform(get("/api/tributes/photos")).andExpect(jsonPath("$", hasSize<Any>(0)))
    }
}
