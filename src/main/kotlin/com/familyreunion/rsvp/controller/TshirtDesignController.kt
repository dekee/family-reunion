package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.DesignResponse
import com.familyreunion.rsvp.dto.DesignVoteRequest
import com.familyreunion.rsvp.security.IpRateLimiter
import com.familyreunion.rsvp.service.TshirtDesignService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/designs")
class TshirtDesignController(
    private val designService: TshirtDesignService,
    private val rateLimiter: IpRateLimiter
) {

    @GetMapping
    fun getAllDesigns(): ResponseEntity<List<DesignResponse>> {
        return ResponseEntity.ok(designService.getAllDesigns())
    }

    @PostMapping("/vote")
    fun vote(
        @Valid @RequestBody request: DesignVoteRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<DesignResponse> {
        // Public endpoint: cap votes per client IP to blunt automated ballot-stuffing.
        val ip = IpRateLimiter.clientIp(httpRequest)
        if (!rateLimiter.tryAcquire("design-vote:$ip", maxRequests = 15, windowSeconds = 600)) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many votes. Please try again later.")
        }
        return ResponseEntity.ok(designService.vote(request))
    }

    @GetMapping("/my-vote")
    fun getMyVote(@RequestParam familyMemberId: Long): ResponseEntity<Map<String, Long?>> {
        val designId = designService.getVoterChoice(familyMemberId)
        return ResponseEntity.ok(mapOf("designId" to designId))
    }
}
