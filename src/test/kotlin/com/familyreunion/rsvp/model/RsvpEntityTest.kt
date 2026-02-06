package com.familyreunion.rsvp.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
class RsvpEntityTest @Autowired constructor(
    private val entityManager: TestEntityManager
) {

    @Test
    fun `should persist an Rsvp entity and generate an id`() {
        val rsvp = Rsvp(
            familyName = "Smith",
            headOfHouseholdName = "John Smith",
            email = "john@smith.com",
            needsLodging = true
        )

        val saved = entityManager.persistAndFlush(rsvp)

        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.familyName).isEqualTo("Smith")
        assertThat(saved.needsLodging).isTrue()
    }

    @Test
    fun `should cascade persist Attendees when saving Rsvp`() {
        val rsvp = Rsvp(
            familyName = "Johnson",
            headOfHouseholdName = "Mary Johnson",
            email = "mary@johnson.com"
        )

        val attendee1 = Attendee(rsvp = rsvp, guestName = "Mary Johnson", guestAgeGroup = AgeGroup.ADULT)
        val attendee2 = Attendee(rsvp = rsvp, guestName = "Tom Johnson", guestAgeGroup = AgeGroup.CHILD, dietaryNeeds = "nut allergy")
        rsvp.attendees.addAll(listOf(attendee1, attendee2))

        val saved = entityManager.persistAndFlush(rsvp)
        entityManager.clear()

        val found = entityManager.find(Rsvp::class.java, saved.id)
        assertThat(found.attendees).hasSize(2)
        assertThat(found.attendees.map { it.guestName }).containsExactlyInAnyOrder("Mary Johnson", "Tom Johnson")
    }

    @Test
    fun `should remove orphan Attendees when removed from Rsvp`() {
        val rsvp = Rsvp(
            familyName = "Williams",
            headOfHouseholdName = "Bob Williams",
            email = "bob@williams.com"
        )

        val attendee1 = Attendee(rsvp = rsvp, guestName = "Bob Williams", guestAgeGroup = AgeGroup.ADULT)
        val attendee2 = Attendee(rsvp = rsvp, guestName = "Sue Williams", guestAgeGroup = AgeGroup.ADULT)
        rsvp.attendees.addAll(listOf(attendee1, attendee2))

        entityManager.persistAndFlush(rsvp)
        entityManager.clear()

        val found = entityManager.find(Rsvp::class.java, rsvp.id)
        found.attendees.removeAt(0)
        entityManager.persistAndFlush(found)
        entityManager.clear()

        val updated = entityManager.find(Rsvp::class.java, rsvp.id)
        assertThat(updated.attendees).hasSize(1)
    }

    @Test
    fun `should store AgeGroup as string`() {
        val rsvp = Rsvp(
            familyName = "Davis",
            headOfHouseholdName = "Ann Davis",
            email = "ann@davis.com"
        )
        val attendee = Attendee(rsvp = rsvp, guestName = "Baby Davis", guestAgeGroup = AgeGroup.INFANT)
        rsvp.attendees.add(attendee)

        entityManager.persistAndFlush(rsvp)
        entityManager.clear()

        val found = entityManager.find(Rsvp::class.java, rsvp.id)
        assertThat(found.attendees[0].guestAgeGroup).isEqualTo(AgeGroup.INFANT)
    }

    @Test
    fun `should persist Attendee with family member reference`() {
        val member = entityManager.persistAndFlush(
            FamilyMember(name = "Test Member", ageGroup = AgeGroup.ADULT, generation = 0)
        )

        val rsvp = Rsvp(
            familyName = "Test",
            headOfHouseholdName = "Test Member",
            email = "test@test.com"
        )
        val attendee = Attendee(rsvp = rsvp, familyMember = member, dietaryNeeds = "vegan")
        rsvp.attendees.add(attendee)

        entityManager.persistAndFlush(rsvp)
        entityManager.clear()

        val found = entityManager.find(Rsvp::class.java, rsvp.id)
        assertThat(found.attendees).hasSize(1)
        assertThat(found.attendees[0].familyMember?.name).isEqualTo("Test Member")
        assertThat(found.attendees[0].dietaryNeeds).isEqualTo("vegan")
        assertThat(found.attendees[0].guestName).isNull()
    }
}
