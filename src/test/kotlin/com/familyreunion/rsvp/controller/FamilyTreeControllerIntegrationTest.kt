package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FamilyTreeControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val familyMemberRepository: FamilyMemberRepository
) {

    @Test
    fun `GET family-tree should return empty tree when no data`() {
        mockMvc.perform(get("/api/family-tree"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.roots", hasSize<Any>(0)))
            .andExpect(jsonPath("$.totalMembers").value(0))
    }

    @Test
    fun `GET family-tree should return founders as roots`() {
        familyMemberRepository.save(
            FamilyMember(name = "Wesley Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )
        familyMemberRepository.save(
            FamilyMember(name = "Esther Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )

        mockMvc.perform(get("/api/family-tree"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.roots", hasSize<Any>(2)))
            .andExpect(jsonPath("$.roots[0].name").value("Wesley Tumblin"))
            .andExpect(jsonPath("$.roots[0].generation").value(0))
            .andExpect(jsonPath("$.roots[1].name").value("Esther Tumblin"))
            .andExpect(jsonPath("$.totalMembers").value(2))
    }

    @Test
    fun `GET family-tree should return nested hierarchy`() {
        val wesley = familyMemberRepository.save(
            FamilyMember(name = "Wesley Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )
        val gail = familyMemberRepository.save(
            FamilyMember(name = "Gail Tumblin", ageGroup = AgeGroup.ADULT, generation = 1, parent = wesley)
        )
        familyMemberRepository.save(
            FamilyMember(name = "Alan", ageGroup = AgeGroup.ADULT, generation = 1, parent = gail)
        )
        familyMemberRepository.save(
            FamilyMember(name = "Aeson", ageGroup = AgeGroup.CHILD, generation = 2, parent = gail)
        )

        mockMvc.perform(get("/api/family-tree"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.roots", hasSize<Any>(1)))
            .andExpect(jsonPath("$.roots[0].name").value("Wesley Tumblin"))
            .andExpect(jsonPath("$.roots[0].children", hasSize<Any>(1)))
            .andExpect(jsonPath("$.roots[0].children[0].name").value("Gail Tumblin"))
            .andExpect(jsonPath("$.roots[0].children[0].children", hasSize<Any>(2)))
            .andExpect(jsonPath("$.totalMembers").value(4))
    }

    // --- CRUD Integration Tests ---

    @Test
    fun `POST members should create member under parent`() {
        val wesley = familyMemberRepository.save(
            FamilyMember(name = "Wesley Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )

        mockMvc.perform(
            post("/api/family-tree/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"New Child","ageGroup":"CHILD","parentId":${wesley.id}}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("New Child"))
            .andExpect(jsonPath("$.ageGroup").value("CHILD"))
            .andExpect(jsonPath("$.generation").value(1))
            .andExpect(jsonPath("$.parentId").value(wesley.id))
    }

    @Test
    fun `POST members should create root member when no parentId`() {
        mockMvc.perform(
            post("/api/family-tree/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"New Root","ageGroup":"ADULT"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("New Root"))
            .andExpect(jsonPath("$.generation").value(0))
            .andExpect(jsonPath("$.parentId").isEmpty)
    }

    @Test
    fun `POST members should return 404 when parent not found`() {
        mockMvc.perform(
            post("/api/family-tree/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Orphan","ageGroup":"CHILD","parentId":999}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Family member not found with id: 999"))
    }

    @Test
    fun `POST members should return 400 when name is blank`() {
        mockMvc.perform(
            post("/api/family-tree/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"","ageGroup":"ADULT"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT members should update member`() {
        val member = familyMemberRepository.save(
            FamilyMember(name = "Old Name", ageGroup = AgeGroup.CHILD, generation = 1)
        )

        mockMvc.perform(
            put("/api/family-tree/members/${member.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"New Name","ageGroup":"ADULT"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.ageGroup").value("ADULT"))
            .andExpect(jsonPath("$.generation").value(1))
    }

    @Test
    fun `PUT members should return 404 when member not found`() {
        mockMvc.perform(
            put("/api/family-tree/members/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"X","ageGroup":"ADULT"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PATCH move should change parent and update generation`() {
        val oldParent = familyMemberRepository.save(
            FamilyMember(name = "Old Parent", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )
        val newParent = familyMemberRepository.save(
            FamilyMember(name = "New Parent", ageGroup = AgeGroup.ADULT, generation = 1)
        )
        val member = familyMemberRepository.save(
            FamilyMember(name = "Mover", ageGroup = AgeGroup.CHILD, generation = 1, parent = oldParent)
        )

        mockMvc.perform(
            patch("/api/family-tree/members/${member.id}/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"newParentId":${newParent.id}}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Mover"))
            .andExpect(jsonPath("$.parentId").value(newParent.id))
            .andExpect(jsonPath("$.generation").value(2))
    }

    @Test
    fun `DELETE members should delete member and return 204`() {
        val member = familyMemberRepository.save(
            FamilyMember(name = "ToDelete", ageGroup = AgeGroup.CHILD, generation = 1)
        )

        mockMvc.perform(delete("/api/family-tree/members/${member.id}"))
            .andExpect(status().isNoContent)

        mockMvc.perform(
            put("/api/family-tree/members/${member.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"X","ageGroup":"ADULT"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE members should cascade delete descendants`() {
        val parent = familyMemberRepository.save(
            FamilyMember(name = "Parent", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )
        familyMemberRepository.save(
            FamilyMember(name = "Child1", ageGroup = AgeGroup.CHILD, generation = 1, parent = parent)
        )
        familyMemberRepository.save(
            FamilyMember(name = "Child2", ageGroup = AgeGroup.CHILD, generation = 1, parent = parent)
        )

        mockMvc.perform(delete("/api/family-tree/members/${parent.id}"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/family-tree"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalMembers").value(0))
    }

    @Test
    fun `DELETE members should return 404 when member not found`() {
        mockMvc.perform(delete("/api/family-tree/members/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST member should appear in tree`() {
        val wesley = familyMemberRepository.save(
            FamilyMember(name = "Wesley Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )

        mockMvc.perform(
            post("/api/family-tree/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"New Kid","ageGroup":"CHILD","parentId":${wesley.id}}""")
        )
            .andExpect(status().isCreated)

        mockMvc.perform(get("/api/family-tree"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.roots[0].children", hasSize<Any>(1)))
            .andExpect(jsonPath("$.roots[0].children[0].name").value("New Kid"))
            .andExpect(jsonPath("$.totalMembers").value(2))
    }
}
