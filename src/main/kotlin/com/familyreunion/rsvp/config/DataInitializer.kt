package com.familyreunion.rsvp.config

import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.model.Rsvp
import com.familyreunion.rsvp.repository.RsvpRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class DataInitializer(private val rsvpRepository: RsvpRepository) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        if (rsvpRepository.count() > 0) return

        val families = listOf(
            family(
                name = "Gail",
                head = "Gail Tumblin",
                email = "gail@tumblin.family",
                adults = listOf("Alan", "Alana", "Kristy", "Candace"),
                children = listOf("Aeson", "Anasiya", "Azael", "Alfie", "Kalah", "Darinam", "Oren", "Chad", "Austin")
            ),
            family(
                name = "Wesley II",
                head = "Wesley Tumblin II",
                email = "wesleyii@tumblin.family",
                adults = listOf("Wesley III", "Thomas", "Justin", "Jessica"),
                children = listOf("Kierra", "Deontia", "Jalanrique", "Duri", "Brooklyn", "Paloma")
            ),
            family(
                name = "Norris",
                head = "Norris Tumblin",
                email = "norris@tumblin.family",
                adults = listOf("Torey", "Michael", "Norris Jr", "Ahmad", "Aishah", "Surah"),
                children = listOf("Braionna", "Dejia", "Nasir", "Zamia", "Kaetyla", "Malika", "Majir", "Josephine", "Leona", "Malei")
            ),
            family(
                name = "Michael",
                head = "Michael Tumblin",
                email = "michael@tumblin.family",
                adults = listOf("Michelle"),
                children = listOf("Niorielle", "Milewisee", "Ely")
            ),
            family(
                name = "Cheryl",
                head = "Cheryl Tumblin",
                email = "cheryl@tumblin.family",
                adults = listOf("Kendrick", "Derrick", "Kiera"),
                children = listOf("Malachi", "Imari", "Izak")
            ),
            family(
                name = "Stephen",
                head = "Stephen Tumblin",
                email = "stephen@tumblin.family",
                adults = listOf("Trina", "Tina", "Toriano", "Chris", "Casey", "Cameron", "Kayla"),
                children = listOf("Patrick", "Preston", "Elijah", "Jose", "Toriano Jr", "Brinae", "Melody", "James", "Dominic")
            ),
            family(
                name = "Kendra",
                head = "Kendra Tumblin",
                email = "kendra@tumblin.family",
                adults = listOf("Byron", "Dinez", "Brandon"),
                children = listOf("Kajia", "Layza", "Logan", "Ragia", "Ramario")
            ),
            family(
                name = "Wendell",
                head = "Wendell Tumblin",
                email = "wendell@tumblin.family",
                adults = listOf("Wendell Jr", "Wendy", "Tony", "Corea"),
                children = listOf("Kitan", "Madison", "Allure")
            ),
            family(
                name = "Donald",
                head = "Donald Tumblin",
                email = "donald@tumblin.family",
                adults = listOf("Ashaunta", "Diondra", "Dana"),
                children = listOf("Avery", "Ada", "Leona", "Bernard", "Benjamin", "Bentley", "Dain", "Emari", "Berkell", "Bailey")
            ),
            family(
                name = "Myra",
                head = "Myra Tumblin",
                email = "myra@tumblin.family",
                adults = listOf("Daillyn", "Angelisha"),
                children = listOf("Jasir", "Jazmyn", "Aiden", "Amartrez")
            ),
            family(
                name = "Chantell",
                head = "Chantell Tumblin",
                email = "chantell@tumblin.family",
                adults = listOf("Donald", "Danielle", "Joy"),
                children = listOf("Rada", "Kaylei", "Syli", "Juvonna", "Jaynai", "Anthony Jr", "Nehemiah", "Kason", "Trinity")
            )
        )

        rsvpRepository.saveAll(families)
    }

    private fun family(
        name: String,
        head: String,
        email: String,
        adults: List<String>,
        children: List<String>
    ): Rsvp {
        val rsvp = Rsvp(
            familyName = name,
            headOfHouseholdName = head,
            email = email
        )

        adults.forEach { memberName ->
            rsvp.familyMembers.add(
                FamilyMember(name = memberName, ageGroup = AgeGroup.ADULT, rsvp = rsvp)
            )
        }

        children.forEach { memberName ->
            rsvp.familyMembers.add(
                FamilyMember(name = memberName, ageGroup = AgeGroup.CHILD, rsvp = rsvp)
            )
        }

        return rsvp
    }
}
