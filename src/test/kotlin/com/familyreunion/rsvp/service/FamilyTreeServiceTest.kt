package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.FamilyMemberRequest
import com.familyreunion.rsvp.dto.MoveMemberRequest
import com.familyreunion.rsvp.exception.FamilyMemberNotFoundException
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*

@ExtendWith(MockitoExtension::class)
class FamilyTreeServiceTest {

    @Mock
    private lateinit var familyMemberRepository: FamilyMemberRepository

    @InjectMocks
    private lateinit var familyTreeService: FamilyTreeService

    @Test
    fun `should build tree with founders as roots`() {
        val wesley = FamilyMember(id = 1L, name = "Wesley Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        val esther = FamilyMember(id = 2L, name = "Esther Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)

        whenever(familyMemberRepository.findByIsFounderTrue()).thenReturn(listOf(wesley, esther))
        whenever(familyMemberRepository.findAll()).thenReturn(listOf(wesley, esther))

        val result = familyTreeService.buildTree()

        assertThat(result.roots).hasSize(2)
        assertThat(result.roots[0].name).isEqualTo("Wesley Tumblin")
        assertThat(result.roots[1].name).isEqualTo("Esther Tumblin")
        assertThat(result.totalMembers).isEqualTo(2)
    }

    @Test
    fun `should include children nested under parent`() {
        val wesley = FamilyMember(id = 1L, name = "Wesley Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        val gail = FamilyMember(id = 3L, name = "Gail Tumblin", ageGroup = AgeGroup.ADULT, generation = 1, parent = wesley)
        val norris = FamilyMember(id = 4L, name = "Norris Tumblin", ageGroup = AgeGroup.ADULT, generation = 1, parent = wesley)
        wesley.children.addAll(listOf(gail, norris))

        whenever(familyMemberRepository.findByIsFounderTrue()).thenReturn(listOf(wesley))
        whenever(familyMemberRepository.findAll()).thenReturn(listOf(wesley, gail, norris))

        val result = familyTreeService.buildTree()

        assertThat(result.roots).hasSize(1)
        assertThat(result.roots[0].children).hasSize(2)
        assertThat(result.roots[0].children[0].name).isEqualTo("Gail Tumblin")
        assertThat(result.roots[0].children[1].name).isEqualTo("Norris Tumblin")
        assertThat(result.totalMembers).isEqualTo(3)
    }

    @Test
    fun `should build three-generation tree`() {
        val wesley = FamilyMember(id = 1L, name = "Wesley Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        val gail = FamilyMember(id = 2L, name = "Gail Tumblin", ageGroup = AgeGroup.ADULT, generation = 1, parent = wesley)
        val alan = FamilyMember(id = 3L, name = "Alan", ageGroup = AgeGroup.ADULT, generation = 1, parent = gail)
        val aeson = FamilyMember(id = 4L, name = "Aeson", ageGroup = AgeGroup.CHILD, generation = 2, parent = alan)
        alan.children.add(aeson)
        gail.children.add(alan)
        wesley.children.add(gail)

        whenever(familyMemberRepository.findByIsFounderTrue()).thenReturn(listOf(wesley))
        whenever(familyMemberRepository.findAll()).thenReturn(listOf(wesley, gail, alan, aeson))

        val result = familyTreeService.buildTree()

        assertThat(result.roots[0].name).isEqualTo("Wesley Tumblin")
        assertThat(result.roots[0].generation).isEqualTo(0)

        val gailNode = result.roots[0].children[0]
        assertThat(gailNode.name).isEqualTo("Gail Tumblin")
        assertThat(gailNode.generation).isEqualTo(1)

        val alanNode = gailNode.children[0]
        assertThat(alanNode.name).isEqualTo("Alan")
        assertThat(alanNode.generation).isEqualTo(1)

        val aesonNode = alanNode.children[0]
        assertThat(aesonNode.name).isEqualTo("Aeson")
        assertThat(aesonNode.generation).isEqualTo(2)
        assertThat(aesonNode.ageGroup).isEqualTo(AgeGroup.CHILD)

        assertThat(result.totalMembers).isEqualTo(4)
    }

    @Test
    fun `should return empty tree when no founders exist`() {
        whenever(familyMemberRepository.findByIsFounderTrue()).thenReturn(emptyList())
        whenever(familyMemberRepository.findAll()).thenReturn(emptyList())

        val result = familyTreeService.buildTree()

        assertThat(result.roots).isEmpty()
        assertThat(result.totalMembers).isEqualTo(0)
    }

    // --- CRUD Tests ---

    @Test
    fun `addMember should create member with auto-calculated generation from parent`() {
        val parent = FamilyMember(id = 1L, name = "Wesley Tumblin", ageGroup = AgeGroup.ADULT, generation = 0)
        val saved = FamilyMember(id = 10L, name = "New Child", ageGroup = AgeGroup.CHILD, generation = 1, parent = parent)

        whenever(familyMemberRepository.findById(1L)).thenReturn(Optional.of(parent))
        whenever(familyMemberRepository.save(any<FamilyMember>())).thenReturn(saved)

        val request = FamilyMemberRequest(name = "New Child", ageGroup = AgeGroup.CHILD, parentId = 1L)
        val result = familyTreeService.addMember(request)

        assertThat(result.name).isEqualTo("New Child")
        assertThat(result.generation).isEqualTo(1)
        assertThat(result.ageGroup).isEqualTo(AgeGroup.CHILD)
        assertThat(result.parentId).isEqualTo(1L)
    }

    @Test
    fun `addMember should create root member when no parentId given`() {
        val saved = FamilyMember(id = 10L, name = "New Root", ageGroup = AgeGroup.ADULT, generation = 0)

        whenever(familyMemberRepository.save(any<FamilyMember>())).thenReturn(saved)

        val request = FamilyMemberRequest(name = "New Root", ageGroup = AgeGroup.ADULT)
        val result = familyTreeService.addMember(request)

        assertThat(result.name).isEqualTo("New Root")
        assertThat(result.generation).isEqualTo(0)
        assertThat(result.parentId).isNull()
    }

    @Test
    fun `addMember should throw when parent not found`() {
        whenever(familyMemberRepository.findById(999L)).thenReturn(Optional.empty())

        val request = FamilyMemberRequest(name = "Orphan", ageGroup = AgeGroup.CHILD, parentId = 999L)

        assertThatThrownBy { familyTreeService.addMember(request) }
            .isInstanceOf(FamilyMemberNotFoundException::class.java)
            .hasMessageContaining("999")
    }

    @Test
    fun `updateMember should update name and ageGroup`() {
        val existing = FamilyMember(id = 5L, name = "Old Name", ageGroup = AgeGroup.CHILD, generation = 1)

        whenever(familyMemberRepository.findById(5L)).thenReturn(Optional.of(existing))
        whenever(familyMemberRepository.save(any<FamilyMember>())).thenAnswer { it.arguments[0] as FamilyMember }

        val request = FamilyMemberRequest(name = "New Name", ageGroup = AgeGroup.ADULT)
        val result = familyTreeService.updateMember(5L, request)

        assertThat(result.name).isEqualTo("New Name")
        assertThat(result.ageGroup).isEqualTo(AgeGroup.ADULT)
        assertThat(result.generation).isEqualTo(1)
    }

    @Test
    fun `updateMember should throw when member not found`() {
        whenever(familyMemberRepository.findById(999L)).thenReturn(Optional.empty())

        val request = FamilyMemberRequest(name = "X", ageGroup = AgeGroup.ADULT)

        assertThatThrownBy { familyTreeService.updateMember(999L, request) }
            .isInstanceOf(FamilyMemberNotFoundException::class.java)
    }

    @Test
    fun `moveMember should change parent and update generation`() {
        val oldParent = FamilyMember(id = 1L, name = "Old Parent", ageGroup = AgeGroup.ADULT, generation = 0)
        val newParent = FamilyMember(id = 2L, name = "New Parent", ageGroup = AgeGroup.ADULT, generation = 1)
        val member = FamilyMember(id = 5L, name = "Mover", ageGroup = AgeGroup.CHILD, generation = 1, parent = oldParent)

        whenever(familyMemberRepository.findById(5L)).thenReturn(Optional.of(member))
        whenever(familyMemberRepository.findById(2L)).thenReturn(Optional.of(newParent))
        whenever(familyMemberRepository.save(any<FamilyMember>())).thenAnswer { it.arguments[0] as FamilyMember }

        val result = familyTreeService.moveMember(5L, MoveMemberRequest(newParentId = 2L))

        assertThat(result.parentId).isEqualTo(2L)
        assertThat(result.generation).isEqualTo(2)
    }

    @Test
    fun `moveMember should make member root when newParentId is null`() {
        val parent = FamilyMember(id = 1L, name = "Parent", ageGroup = AgeGroup.ADULT, generation = 0)
        val member = FamilyMember(id = 5L, name = "Mover", ageGroup = AgeGroup.ADULT, generation = 1, parent = parent)

        whenever(familyMemberRepository.findById(5L)).thenReturn(Optional.of(member))
        whenever(familyMemberRepository.save(any<FamilyMember>())).thenAnswer { it.arguments[0] as FamilyMember }

        val result = familyTreeService.moveMember(5L, MoveMemberRequest(newParentId = null))

        assertThat(result.parentId).isNull()
        assertThat(result.generation).isEqualTo(0)
    }

    @Test
    fun `deleteMember should delete member`() {
        val member = FamilyMember(id = 5L, name = "ToDelete", ageGroup = AgeGroup.CHILD, generation = 1)

        whenever(familyMemberRepository.findById(5L)).thenReturn(Optional.of(member))

        familyTreeService.deleteMember(5L)

        verify(familyMemberRepository).delete(member)
    }

    @Test
    fun `deleteMember should throw when member not found`() {
        whenever(familyMemberRepository.findById(999L)).thenReturn(Optional.empty())

        assertThatThrownBy { familyTreeService.deleteMember(999L) }
            .isInstanceOf(FamilyMemberNotFoundException::class.java)
    }
}
