package com.familyreunion.rsvp.config

import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.Attendee
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.Meeting
import com.familyreunion.rsvp.model.Rsvp
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.MeetingRepository
import com.familyreunion.rsvp.repository.RsvpRepository
import java.time.LocalDateTime
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class DataInitializer(
    private val rsvpRepository: RsvpRepository,
    private val familyMemberRepository: FamilyMemberRepository,
    private val meetingRepository: MeetingRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        if (familyMemberRepository.count() > 0) return

        // Generation 0: Founders
        val wesley = familyMemberRepository.save(
            FamilyMember(name = "Wesley Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )
        familyMemberRepository.save(
            FamilyMember(name = "Esther Tumblin", ageGroup = AgeGroup.ADULT, generation = 0, isFounder = true)
        )

        // Each branch: Gen 1 head is child of Wesley, spouse and Gen 2 children linked appropriately
        val families = listOf(
            branchDef(
                name = "Gail", head = "Gail Tumblin", email = "gail@tumblin.family",
                gen1Children = listOf("Alan", "Alana", "Kristy", "Candace"),
                gen2Grandchildren = mapOf(
                    "Alan" to listOf("Aeson", "Anasiya"),
                    "Alana" to listOf("Azael", "Alfie"),
                    "Kristy" to listOf("Kalah", "Darinam"),
                    "Candace" to listOf("Oren", "Chad", "Austin")
                )
            ),
            branchDef(
                name = "Wesley II", head = "Wesley Tumblin II", email = "wesleyii@tumblin.family",
                gen1Children = listOf("Wesley III", "Thomas", "Justin", "Jessica"),
                gen2Grandchildren = mapOf(
                    "Wesley III" to listOf("Kierra"),
                    "Thomas" to listOf("Deontia", "Jalanrique"),
                    "Justin" to listOf("Duri", "Brooklyn"),
                    "Jessica" to listOf("Paloma")
                )
            ),
            branchDef(
                name = "Norris", head = "Norris Tumblin", email = "norris@tumblin.family",
                gen1Children = listOf("Torey", "Michael", "Norris Jr", "Ahmad", "Aishah", "Surah"),
                gen2Grandchildren = mapOf(
                    "Torey" to listOf("Braionna", "Dejia"),
                    "Michael" to listOf("Nasir", "Zamia"),
                    "Norris Jr" to listOf("Kaetyla", "Malika"),
                    "Ahmad" to listOf("Majir", "Josephine"),
                    "Aishah" to listOf("Leona", "Malei"),
                    "Surah" to emptyList()
                )
            ),
            branchDef(
                name = "Michael", head = "Michael Tumblin", email = "michael@tumblin.family",
                gen1Children = listOf("Michelle"),
                gen2Grandchildren = mapOf(
                    "Michelle" to listOf("Niorielle", "Milewisee", "Ely")
                )
            ),
            branchDef(
                name = "Cheryl", head = "Cheryl Tumblin", email = "cheryl@tumblin.family",
                gen1Children = listOf("Kendrick", "Derrick", "Kiera"),
                gen2Grandchildren = mapOf(
                    "Kendrick" to emptyList(),
                    "Derrick" to listOf("Ariel", "Malachi", "Isaiah"),
                    "Kiera" to listOf("Christian", "Tyler", "Leeah")
                )
            ),
            branchDef(
                name = "Stephen", head = "Stephen Tumblin", email = "stephen@tumblin.family",
                gen1Children = listOf("Trina", "Tina", "Toriano", "Chris", "Casey", "Cameron", "Kayla"),
                gen2Grandchildren = mapOf(
                    "Trina" to listOf("Patrick", "Preston"),
                    "Tina" to listOf("Elijah", "Jose"),
                    "Toriano" to listOf("Toriano Jr", "Brinae"),
                    "Chris" to listOf("Melody"),
                    "Casey" to listOf("James", "Dominic"),
                    "Cameron" to emptyList(),
                    "Kayla" to emptyList()
                )
            ),
            branchDef(
                name = "Kendra", head = "Kendra Tumblin", email = "kendra@tumblin.family",
                gen1Children = listOf("Byron", "Dinez", "Brandon"),
                gen2Grandchildren = mapOf(
                    "Byron" to listOf("Kajia", "Layza"),
                    "Dinez" to listOf("Logan", "Ragia"),
                    "Brandon" to listOf("Ramario")
                )
            ),
            branchDef(
                name = "Wendell", head = "Wendell Tumblin", email = "wendell@tumblin.family",
                gen1Children = listOf("Wendell Jr", "Wendy", "Tony", "Corea"),
                gen2Grandchildren = mapOf(
                    "Wendell Jr" to listOf("Kitan"),
                    "Wendy" to listOf("Madison"),
                    "Tony" to listOf("Allure"),
                    "Corea" to emptyList()
                )
            ),
            branchDef(
                name = "Donald", head = "Donald Tumblin", email = "donald@tumblin.family",
                gen1Children = listOf("Ashaunta", "Diondra", "Dana"),
                gen2Grandchildren = mapOf(
                    "Ashaunta" to listOf("Avery", "Ada", "Leona"),
                    "Diondra" to listOf("Bernard", "Benjamin", "Bentley"),
                    "Dana" to listOf("Dain", "Emari", "Berkell", "Bailey")
                )
            ),
            branchDef(
                name = "Myra", head = "Myra Tumblin", email = "myra@tumblin.family",
                gen1Children = listOf("Daillyn", "Angelisha"),
                gen2Grandchildren = mapOf(
                    "Daillyn" to listOf("Jasir", "Jazmyn"),
                    "Angelisha" to listOf("Aiden", "Amartrez")
                )
            ),
            branchDef(
                name = "Chantell", head = "Chantell Tumblin", email = "chantell@tumblin.family",
                gen1Children = listOf("Donald", "Danielle", "Joy"),
                gen2Grandchildren = mapOf(
                    "Donald" to listOf("Rada", "Kaylei", "Syli"),
                    "Danielle" to listOf("Juvonna", "Jaynai", "Anthony Jr"),
                    "Joy" to listOf("Nehemiah", "Kason", "Trinity")
                )
            )
        )

        families.forEach { branch ->
            val branchMembers = createFamilyTree(branch, wesley)
            createBranchRsvp(branch, branchMembers)
        }

        // Seed a sample meeting
        meetingRepository.save(
            Meeting(
                title = "Reunion Planning Call",
                meetingDateTime = LocalDateTime.of(2026, 3, 15, 14, 0),
                zoomLink = "https://zoom.us/j/1234567890",
                phoneNumber = "+1 (312) 626-6799",
                meetingId = "123 456 7890",
                passcode = "tumblin2026",
                notes = "Monthly planning call — all family branch heads please attend."
            )
        )
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

        // Branch head is Gen 1, child of founder
        val headMember = familyMemberRepository.save(
            FamilyMember(
                name = branch.head,
                ageGroup = AgeGroup.ADULT,
                parent = founder,
                generation = 1
            )
        )
        members.add(headMember)

        // Gen 1 children and their Gen 2 grandchildren
        branch.gen1Children.forEach { childName ->
            val gen1Member = familyMemberRepository.save(
                FamilyMember(
                    name = childName,
                    ageGroup = AgeGroup.ADULT,
                    parent = headMember,
                    generation = 1
                )
            )
            members.add(gen1Member)

            val grandchildren = branch.gen2Grandchildren[childName] ?: emptyList()
            grandchildren.forEach { grandchildName ->
                val gen2Member = familyMemberRepository.save(
                    FamilyMember(
                        name = grandchildName,
                        ageGroup = AgeGroup.CHILD,
                        parent = gen1Member,
                        generation = 2
                    )
                )
                members.add(gen2Member)
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
}
