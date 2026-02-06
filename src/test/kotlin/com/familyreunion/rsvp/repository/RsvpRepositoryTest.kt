package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.Rsvp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
class RsvpRepositoryTest @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val rsvpRepository: RsvpRepository
) {

    private fun createRsvp(familyName: String, email: String): Rsvp {
        val rsvp = Rsvp(
            familyName = familyName,
            headOfHouseholdName = "$familyName Head",
            email = email
        )
        val member = FamilyMember(name = "$familyName Member", ageGroup = AgeGroup.ADULT, rsvp = rsvp)
        rsvp.familyMembers.add(member)
        return rsvp
    }

    @Test
    fun `should find all RSVPs`() {
        entityManager.persist(createRsvp("Smith", "smith@test.com"))
        entityManager.persist(createRsvp("Johnson", "johnson@test.com"))
        entityManager.persist(createRsvp("Williams", "williams@test.com"))
        entityManager.flush()

        val result = rsvpRepository.findAll()

        assertThat(result).hasSize(3)
    }

    @Test
    fun `should find RSVP by id`() {
        val rsvp = entityManager.persistAndFlush(createRsvp("Brown", "brown@test.com"))

        val result = rsvpRepository.findById(rsvp.id)

        assertThat(result).isPresent
        assertThat(result.get().familyName).isEqualTo("Brown")
    }

    @Test
    fun `should return empty optional for nonexistent id`() {
        val result = rsvpRepository.findById(999L)

        assertThat(result).isEmpty()
    }

    @Test
    fun `should delete RSVP by id`() {
        val rsvp = entityManager.persistAndFlush(createRsvp("Taylor", "taylor@test.com"))

        rsvpRepository.deleteById(rsvp.id)
        entityManager.flush()
        entityManager.clear()

        val result = rsvpRepository.findById(rsvp.id)
        assertThat(result).isEmpty()
    }
}
