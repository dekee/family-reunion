package com.familyreunion.rsvp.config

import com.familyreunion.rsvp.model.TshirtDesign
import com.familyreunion.rsvp.repository.TshirtDesignRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Seeds the two t-shirt design candidates in every environment (idempotent by name).
 * Unlike DataInitializer this must run in prod — the designs are fixed ballot
 * options whose images ship as static frontend assets under /designs/.
 */
@Component
class TshirtDesignInitializer(
    private val designRepository: TshirtDesignRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val designs = listOf(
            TshirtDesign(name = "Legacy Tree", imageUrl = "/designs/design-legacy-tree.jpg"),
            TshirtDesign(name = "Heart & Branches", imageUrl = "/designs/design-heart-tree.jpg")
        )
        for (design in designs) {
            if (!designRepository.existsByName(design.name)) {
                designRepository.save(design)
            }
        }
    }
}
