package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.AttendeeDto
import com.familyreunion.rsvp.dto.RsvpRequest
import com.familyreunion.rsvp.exception.RsvpNotFoundException
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.Attendee
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.Rsvp
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.RsvpRepository
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
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class RsvpServiceTest {

    @Mock
    private lateinit var rsvpRepository: RsvpRepository

    @Mock
    private lateinit var familyMemberRepository: FamilyMemberRepository

    @InjectMocks
    private lateinit var rsvpService: RsvpService

    private fun createRequest(
        familyName: String = "Smith",
        needsLodging: Boolean = false
    ) = RsvpRequest(
        familyName = familyName,
        headOfHouseholdName = "$familyName Head",
        email = "${familyName.lowercase()}@test.com",
        phone = "555-0100",
        attendees = listOf(
            AttendeeDto(guestName = "Adult Member", guestAgeGroup = AgeGroup.ADULT),
            AttendeeDto(guestName = "Child Member", guestAgeGroup = AgeGroup.CHILD, dietaryNeeds = "vegetarian")
        ),
        needsLodging = needsLodging,
        arrivalDate = LocalDate.of(2026, 7, 4),
        departureDate = LocalDate.of(2026, 7, 6),
        notes = "Looking forward to it!"
    )

    private fun createEntity(id: Long = 1L, familyName: String = "Smith", needsLodging: Boolean = false): Rsvp {
        val rsvp = Rsvp(
            id = id,
            familyName = familyName,
            headOfHouseholdName = "$familyName Head",
            email = "${familyName.lowercase()}@test.com",
            phone = "555-0100",
            needsLodging = needsLodging,
            arrivalDate = LocalDate.of(2026, 7, 4),
            departureDate = LocalDate.of(2026, 7, 6),
            notes = "Looking forward to it!"
        )
        val attendee1 = Attendee(id = 1L, rsvp = rsvp, guestName = "Adult Member", guestAgeGroup = AgeGroup.ADULT)
        val attendee2 = Attendee(id = 2L, rsvp = rsvp, guestName = "Child Member", guestAgeGroup = AgeGroup.CHILD, dietaryNeeds = "vegetarian")
        rsvp.attendees.addAll(listOf(attendee1, attendee2))
        return rsvp
    }

    @Test
    fun `should create RSVP and return response`() {
        val request = createRequest()
        whenever(rsvpRepository.save(any<Rsvp>())).thenAnswer { invocation ->
            val saved = invocation.getArgument<Rsvp>(0)
            Rsvp(
                id = 1L,
                familyName = saved.familyName,
                headOfHouseholdName = saved.headOfHouseholdName,
                email = saved.email,
                phone = saved.phone,
                needsLodging = saved.needsLodging,
                arrivalDate = saved.arrivalDate,
                departureDate = saved.departureDate,
                notes = saved.notes
            ).also { it.attendees.addAll(saved.attendees) }
        }

        val result = rsvpService.createRsvp(request)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.familyName).isEqualTo("Smith")
        assertThat(result.attendees).hasSize(2)
        assertThat(result.email).isEqualTo("smith@test.com")
        verify(rsvpRepository).save(any<Rsvp>())
    }

    @Test
    fun `should return all RSVPs as response list`() {
        whenever(rsvpRepository.findAll()).thenReturn(
            listOf(createEntity(1L, "Smith"), createEntity(2L, "Johnson"))
        )

        val result = rsvpService.getAllRsvps()

        assertThat(result).hasSize(2)
        assertThat(result[0].familyName).isEqualTo("Smith")
        assertThat(result[1].familyName).isEqualTo("Johnson")
    }

    @Test
    fun `should return RSVP by id`() {
        whenever(rsvpRepository.findById(1L)).thenReturn(Optional.of(createEntity()))

        val result = rsvpService.getRsvpById(1L)

        assertThat(result.familyName).isEqualTo("Smith")
        assertThat(result.attendees).hasSize(2)
    }

    @Test
    fun `should throw RsvpNotFoundException for unknown id`() {
        whenever(rsvpRepository.findById(999L)).thenReturn(Optional.empty())

        assertThatThrownBy { rsvpService.getRsvpById(999L) }
            .isInstanceOf(RsvpNotFoundException::class.java)
            .hasMessageContaining("999")
    }

    @Test
    fun `should update existing RSVP`() {
        val existing = createEntity()
        whenever(rsvpRepository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(rsvpRepository.save(any<Rsvp>())).thenAnswer { it.getArgument<Rsvp>(0) }

        val updateRequest = RsvpRequest(
            familyName = "Smith-Updated",
            headOfHouseholdName = "Jane Smith",
            email = "jane@smith.com",
            attendees = listOf(
                AttendeeDto(guestName = "Jane Smith", guestAgeGroup = AgeGroup.ADULT)
            ),
            needsLodging = true
        )

        val result = rsvpService.updateRsvp(1L, updateRequest)

        assertThat(result.familyName).isEqualTo("Smith-Updated")
        assertThat(result.headOfHouseholdName).isEqualTo("Jane Smith")
        assertThat(result.attendees).hasSize(1)
        assertThat(result.needsLodging).isTrue()
    }

    @Test
    fun `should delete RSVP by id`() {
        whenever(rsvpRepository.existsById(1L)).thenReturn(true)

        rsvpService.deleteRsvp(1L)

        verify(rsvpRepository).deleteById(1L)
    }

    @Test
    fun `should throw RsvpNotFoundException when deleting unknown id`() {
        whenever(rsvpRepository.existsById(999L)).thenReturn(false)

        assertThatThrownBy { rsvpService.deleteRsvp(999L) }
            .isInstanceOf(RsvpNotFoundException::class.java)
            .hasMessageContaining("999")
    }

    @Test
    fun `should calculate summary correctly`() {
        val rsvp1 = createEntity(1L, "Smith", needsLodging = true).also {
            it.attendees.clear()
            it.attendees.add(Attendee(rsvp = it, guestName = "Adult1", guestAgeGroup = AgeGroup.ADULT))
            it.attendees.add(Attendee(rsvp = it, guestName = "Child1", guestAgeGroup = AgeGroup.CHILD))
            it.attendees.add(Attendee(rsvp = it, guestName = "Infant1", guestAgeGroup = AgeGroup.INFANT))
        }
        val rsvp2 = createEntity(2L, "Johnson", needsLodging = false).also {
            it.attendees.clear()
            it.attendees.add(Attendee(rsvp = it, guestName = "Adult2", guestAgeGroup = AgeGroup.ADULT))
            it.attendees.add(Attendee(rsvp = it, guestName = "Adult3", guestAgeGroup = AgeGroup.ADULT))
        }

        whenever(rsvpRepository.findAll()).thenReturn(listOf(rsvp1, rsvp2))

        val summary = rsvpService.getSummary()

        assertThat(summary.totalFamilies).isEqualTo(2)
        assertThat(summary.totalHeadcount).isEqualTo(5)
        assertThat(summary.adultCount).isEqualTo(3)
        assertThat(summary.childCount).isEqualTo(1)
        assertThat(summary.infantCount).isEqualTo(1)
        assertThat(summary.lodgingCount).isEqualTo(1)
    }
}
