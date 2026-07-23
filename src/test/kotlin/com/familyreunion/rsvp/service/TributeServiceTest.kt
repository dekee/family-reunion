package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.TributeRequest
import com.familyreunion.rsvp.exception.FamilyMemberNotFoundException
import com.familyreunion.rsvp.exception.TributeNotFoundException
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.Tribute
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.TributeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*

@ExtendWith(MockitoExtension::class)
class TributeServiceTest {

    @Mock
    private lateinit var tributeRepository: TributeRepository

    @Mock
    private lateinit var familyMemberRepository: FamilyMemberRepository

    @InjectMocks
    private lateinit var tributeService: TributeService

    private val founder = FamilyMember(id = 1L, name = "Wesley & Esther", isFounder = true)
    private val sibling = FamilyMember(id = 2L, name = "Cheryl", parent = founder)
    private val author = FamilyMember(id = 10L, name = "Derrick", parent = sibling)

    @Test
    fun `submitTribute should create new tribute for a pillar`() {
        whenever(familyMemberRepository.findById(2L)).thenReturn(Optional.of(sibling))
        whenever(familyMemberRepository.findById(10L)).thenReturn(Optional.of(author))
        whenever(tributeRepository.findBySiblingAndAuthor(sibling, author)).thenReturn(null)
        whenever(tributeRepository.save(any<Tribute>())).thenAnswer { it.arguments[0] }

        val response = tributeService.submitTribute(
            TributeRequest(siblingId = 2L, authorId = 10L, story = "  A wonderful story.  ")
        )

        assertThat(response.siblingName).isEqualTo("Cheryl")
        assertThat(response.authorName).isEqualTo("Derrick")
        assertThat(response.story).isEqualTo("A wonderful story.")
    }

    @Test
    fun `submitTribute should update existing tribute from same author`() {
        val existing = Tribute(id = 5L, sibling = sibling, author = author, story = "Old story")
        whenever(familyMemberRepository.findById(2L)).thenReturn(Optional.of(sibling))
        whenever(familyMemberRepository.findById(10L)).thenReturn(Optional.of(author))
        whenever(tributeRepository.findBySiblingAndAuthor(sibling, author)).thenReturn(existing)
        whenever(tributeRepository.save(any<Tribute>())).thenAnswer { it.arguments[0] }

        val response = tributeService.submitTribute(
            TributeRequest(siblingId = 2L, authorId = 10L, story = "New story")
        )

        val captor = argumentCaptor<Tribute>()
        verify(tributeRepository).save(captor.capture())
        assertThat(captor.firstValue.id).isEqualTo(5L)
        assertThat(response.story).isEqualTo("New story")
    }

    @Test
    fun `submitTribute should reject sibling that is not a pillar`() {
        // author's parent is a sibling, not a founder — not a valid tribute target
        whenever(familyMemberRepository.findById(10L)).thenReturn(Optional.of(author))

        assertThatThrownBy {
            tributeService.submitTribute(TributeRequest(siblingId = 10L, authorId = 10L, story = "Story"))
        }.isInstanceOf(IllegalStateException::class.java)

        verify(tributeRepository, never()).save(any<Tribute>())
    }

    @Test
    fun `submitTribute should throw when sibling not found`() {
        whenever(familyMemberRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy {
            tributeService.submitTribute(TributeRequest(siblingId = 99L, authorId = 10L, story = "Story"))
        }.isInstanceOf(FamilyMemberNotFoundException::class.java)
    }

    @Test
    fun `getAllTributes should map entities to responses`() {
        val tribute = Tribute(id = 7L, sibling = sibling, author = author, story = "Story")
        whenever(tributeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(listOf(tribute))

        val result = tributeService.getAllTributes()

        assertThat(result).hasSize(1)
        assertThat(result[0].siblingId).isEqualTo(2L)
        assertThat(result[0].authorId).isEqualTo(10L)
    }

    @Test
    fun `deleteTribute should throw when not found`() {
        whenever(tributeRepository.existsById(99L)).thenReturn(false)

        assertThatThrownBy { tributeService.deleteTribute(99L) }
            .isInstanceOf(TributeNotFoundException::class.java)
    }

    @Test
    fun `deleteTribute should delete existing tribute`() {
        whenever(tributeRepository.existsById(5L)).thenReturn(true)

        tributeService.deleteTribute(5L)

        verify(tributeRepository).deleteById(5L)
    }
}
