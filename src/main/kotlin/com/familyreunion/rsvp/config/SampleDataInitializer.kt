package com.familyreunion.rsvp.config

import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.Attendee
import com.familyreunion.rsvp.model.Event
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.Meeting
import com.familyreunion.rsvp.model.Rsvp
import com.familyreunion.rsvp.model.TshirtSlogan
import com.familyreunion.rsvp.repository.EventRepository
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.MeetingRepository
import com.familyreunion.rsvp.repository.RsvpRepository
import com.familyreunion.rsvp.repository.TshirtSloganRepository
import java.time.LocalDateTime
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Sample family data for development. Replace with your own family structure.
 * This only runs in the "dev" Spring profile — never in production.
 *
 * To customize for your family:
 * 1. Change the founders (generation 0) to your family's patriarch/matriarch
 * 2. Add your family's branches (generation 1) as children of the founder
 * 3. Add generation 2+ members as children of each branch head
 * 4. Update the sample events, meetings, and t-shirt slogans
 *
 * Or skip this entirely — you can add all family data through the admin UI.
 */
@Component
@Profile("dev")
class SampleDataInitializer(
    private val rsvpRepository: RsvpRepository,
    private val familyMemberRepository: FamilyMemberRepository,
    private val meetingRepository: MeetingRepository,
    private val eventRepository: EventRepository,
    private val tshirtSloganRepository: TshirtSloganRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        if (familyMemberRepository.count() > 0) return

        // ── Generation 0: Founders ─────────────────────────────────
        val founder = familyMemberRepository.save(
            FamilyMember(name = "John Smith", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )
        familyMemberRepository.save(
            FamilyMember(name = "Mary Smith", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )

        // ── Family Branches ────────────────────────────────────────
        // Each branch: name, branch head name, email, children, grandchildren
        val families = listOf(
            branchDef(
                name = "Alice", head = "Alice Smith", email = "alice@example.com",
                gen1Children = listOf("Emma", "Jack"),
                gen2Grandchildren = mapOf(
                    "Emma" to listOf("Lily", "Noah"),
                    "Jack" to listOf("Oliver")
                )
            ),
            branchDef(
                name = "Bob", head = "Bob Smith", email = "bob@example.com",
                gen1Children = listOf("Sarah", "David"),
                gen2Grandchildren = mapOf(
                    "Sarah" to listOf("Ava"),
                    "David" to emptyList()
                )
            ),
            branchDef(
                name = "Carol", head = "Carol Smith", email = "carol@example.com",
                gen1Children = listOf("James"),
                gen2Grandchildren = mapOf(
                    "James" to listOf("Sophia", "Lucas")
                )
            )
        )

        families.forEach { branch ->
            val branchMembers = createFamilyTree(branch, founder)
            createBranchRsvp(branch, branchMembers)
        }

        // ── Sample Meeting ─────────────────────────────────────────
        meetingRepository.save(
            Meeting(
                title = "Reunion Planning Call",
                meetingDateTime = LocalDateTime.of(2026, 3, 15, 14, 0),
                zoomLink = "https://zoom.us/j/1234567890",
                phoneNumber = "+1 (555) 123-4567",
                meetingId = "123 456 7890",
                passcode = "reunion2026",
                notes = "Monthly planning call — all branch heads please attend."
            )
        )

        // ── Sample Events ──────────────────────────────────────────
        eventRepository.save(
            Event(
                title = "Welcome BBQ",
                description = "Kickoff cookout to start the reunion weekend!",
                eventDateTime = LocalDateTime.of(2026, 7, 4, 17, 0),
                address = "123 Main Street",
                hostName = "Alice",
                notes = "Bring a side dish!"
            )
        )
        eventRepository.save(
            Event(
                title = "Family Picnic",
                description = "Outdoor games and food in the park.",
                eventDateTime = LocalDateTime.of(2026, 7, 5, 11, 0),
                address = "City Park Pavilion",
                hostName = null,
                notes = null
            )
        )

        seedSlogans()
    }

    private data class BranchDef(
        val name: String,
        val head: String,
        val email: String,
        val gen1Children: List<String>,
        val gen2Grandchildren: Map<String, List<String>>
    )

    private fun branchDef(
        name: String,
        head: String,
        email: String,
        gen1Children: List<String>,
        gen2Grandchildren: Map<String, List<String>>
    ) = BranchDef(name, head, email, gen1Children, gen2Grandchildren)

    private fun createFamilyTree(branch: BranchDef, founder: FamilyMember): List<FamilyMember> {
        val members = mutableListOf<FamilyMember>()

        val headMember = familyMemberRepository.save(
            FamilyMember(
                name = branch.head,
                ageGroup = AgeGroup.ADULT,
                parent = founder,
                generation = 1
            )
        )
        members.add(headMember)

        branch.gen1Children.forEach { childName ->
            val gen2Member = familyMemberRepository.save(
                FamilyMember(
                    name = childName,
                    ageGroup = AgeGroup.ADULT,
                    parent = headMember,
                    generation = 2
                )
            )
            members.add(gen2Member)

            val grandchildren = branch.gen2Grandchildren[childName] ?: emptyList()
            grandchildren.forEach { grandchildName ->
                val gen3Member = familyMemberRepository.save(
                    FamilyMember(
                        name = grandchildName,
                        ageGroup = AgeGroup.CHILD,
                        parent = gen2Member,
                        generation = 3
                    )
                )
                members.add(gen3Member)
            }
        }

        return members
    }

    private fun createBranchRsvp(branch: BranchDef, branchMembers: List<FamilyMember>) {
        val rsvp = Rsvp(
            familyName = branch.name,
            headOfHouseholdName = branch.head,
            email = branch.email
        )

        branchMembers.forEach { member ->
            rsvp.attendees.add(Attendee(rsvp = rsvp, familyMember = member))
        }

        rsvpRepository.save(rsvp)
    }

    private fun seedSlogans() {
        if (tshirtSloganRepository.count() > 0) return
        val slogans = listOf(
            "Together We Rise, Together We Celebrate",
            "One Family, Many Branches",
            "Rooted in Love, Growing Together",
            "Family First, Always and Forever",
            "Generations of Greatness"
        )
        slogans.forEach { text ->
            tshirtSloganRepository.save(TshirtSlogan(slogan = text))
        }
    }
}
