package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.TributeRequest
import com.familyreunion.rsvp.dto.TributeResponse
import com.familyreunion.rsvp.security.IpRateLimiter
import com.familyreunion.rsvp.service.TributeService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/tributes")
class TributeController(
    private val tributeService: TributeService,
    private val rateLimiter: IpRateLimiter
) {

    @GetMapping
    fun getAllTributes(): ResponseEntity<List<TributeResponse>> {
        return ResponseEntity.ok(tributeService.getAllTributes())
    }

    @PostMapping
    fun submitTribute(
        @Valid @RequestBody request: TributeRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<TributeResponse> {
        // Public endpoint: cap submissions per client IP to blunt automated abuse.
        val ip = IpRateLimiter.clientIp(httpRequest)
        if (!rateLimiter.tryAcquire("tribute:$ip", maxRequests = 5, windowSeconds = 600)) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many tributes. Please try again later.")
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(tributeService.submitTribute(request))
    }

    @DeleteMapping("/{id}")
    fun deleteTribute(@PathVariable id: Long): ResponseEntity<Void> {
        tributeService.deleteTribute(id)
        return ResponseEntity.noContent().build()
    }
}
