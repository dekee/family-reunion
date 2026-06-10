package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.SloganRequest
import com.familyreunion.rsvp.dto.SloganResponse
import com.familyreunion.rsvp.dto.SloganVoteRequest
import com.familyreunion.rsvp.security.IpRateLimiter
import com.familyreunion.rsvp.service.TshirtSloganService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/slogans")
class TshirtSloganController(
    private val sloganService: TshirtSloganService,
    private val rateLimiter: IpRateLimiter
) {

    @GetMapping
    fun getAllSlogans(): ResponseEntity<List<SloganResponse>> {
        return ResponseEntity.ok(sloganService.getAllSlogans())
    }

    @PostMapping("/vote")
    fun vote(
        @Valid @RequestBody request: SloganVoteRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<SloganResponse> {
        // Public endpoint: cap votes per client IP to blunt automated ballot-stuffing.
        val ip = IpRateLimiter.clientIp(httpRequest)
        if (!rateLimiter.tryAcquire("slogan-vote:$ip", maxRequests = 15, windowSeconds = 600)) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many votes. Please try again later.")
        }
        return ResponseEntity.ok(sloganService.vote(request))
    }

    @GetMapping("/my-vote")
    fun getMyVote(@RequestParam familyMemberId: Long): ResponseEntity<Map<String, Long?>> {
        val sloganId = sloganService.getVoterChoice(familyMemberId)
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
