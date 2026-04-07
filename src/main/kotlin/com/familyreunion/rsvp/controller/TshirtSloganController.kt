package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.SloganRequest
import com.familyreunion.rsvp.dto.SloganResponse
import com.familyreunion.rsvp.dto.SloganVoteRequest
import com.familyreunion.rsvp.service.TshirtSloganService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/slogans")
class TshirtSloganController(private val sloganService: TshirtSloganService) {

    @GetMapping
    fun getAllSlogans(): ResponseEntity<List<SloganResponse>> {
        return ResponseEntity.ok(sloganService.getAllSlogans())
    }

    @PostMapping("/vote")
    fun vote(@Valid @RequestBody request: SloganVoteRequest): ResponseEntity<SloganResponse> {
        return ResponseEntity.ok(sloganService.vote(request))
    }

    @GetMapping("/my-vote")
    fun getMyVote(@RequestParam voterName: String): ResponseEntity<Map<String, Long?>> {
        val sloganId = sloganService.getVoterChoice(voterName)
        return ResponseEntity.ok(mapOf("sloganId" to sloganId))
    }

    @PostMapping
    fun createSlogan(@Valid @RequestBody request: SloganRequest): ResponseEntity<SloganResponse> {
        val response = sloganService.createSlogan(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping("/{id}")
    fun deleteSlogan(@PathVariable id: Long): ResponseEntity<Void> {
        sloganService.deleteSlogan(id)
        return ResponseEntity.noContent().build()
    }
}
