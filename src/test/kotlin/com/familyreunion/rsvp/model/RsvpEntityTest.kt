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
    fun `should cascade persist FamilyMembers when saving Rsvp`() {
        val rsvp = Rsvp(
            familyName = "Johnson",
            headOfHouseholdName = "Mary Johnson",
            email = "mary@johnson.com"
        )

        val member1 = FamilyMember(name = "Mary Johnson", ageGroup = AgeGroup.ADULT, rsvp = rsvp)
        val member2 = FamilyMember(name = "Tom Johnson", ageGroup = AgeGroup.CHILD, dietaryNeeds = "nut allergy", rsvp = rsvp)
        rsvp.familyMembers.addAll(listOf(member1, member2))

        val saved = entityManager.persistAndFlush(rsvp)
        entityManager.clear()

        val found = entityManager.find(Rsvp::class.java, saved.id)
        assertThat(found.familyMembers).hasSize(2)
        assertThat(found.familyMembers.map { it.name }).containsExactlyInAnyOrder("Mary Johnson", "Tom Johnson")
    }

    @Test
    fun `should remove orphan FamilyMembers when removed from Rsvp`() {
        val rsvp = Rsvp(
            familyName = "Williams",
            headOfHouseholdName = "Bob Williams",
            email = "bob@williams.com"
        )

        val member1 = FamilyMember(name = "Bob Williams", ageGroup = AgeGroup.ADULT, rsvp = rsvp)
        val member2 = FamilyMember(name = "Sue Williams", ageGroup = AgeGroup.ADULT, rsvp = rsvp)
        rsvp.familyMembers.addAll(listOf(member1, member2))

        entityManager.persistAndFlush(rsvp)
        entityManager.clear()

        val found = entityManager.find(Rsvp::class.java, rsvp.id)
        found.familyMembers.removeAt(0)
        entityManager.persistAndFlush(found)
        entityManager.clear()

        val updated = entityManager.find(Rsvp::class.java, rsvp.id)
        assertThat(updated.familyMembers).hasSize(1)
    }

    @Test
    fun `should store AgeGroup as string`() {
        val rsvp = Rsvp(
            familyName = "Davis",
            headOfHouseholdName = "Ann Davis",
            email = "ann@davis.com"
        )
        val member = FamilyMember(name = "Baby Davis", ageGroup = AgeGroup.INFANT, rsvp = rsvp)
        rsvp.familyMembers.add(member)

        entityManager.persistAndFlush(rsvp)
        entityManager.clear()

        val found = entityManager.find(Rsvp::class.java, rsvp.id)
        assertThat(found.familyMembers[0].ageGroup).isEqualTo(AgeGroup.INFANT)
    }
}
