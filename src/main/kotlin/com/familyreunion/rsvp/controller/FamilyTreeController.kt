package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.FamilyMemberRequest
import com.familyreunion.rsvp.dto.FamilyTreeNode
import com.familyreunion.rsvp.dto.FamilyTreeResponse
import com.familyreunion.rsvp.dto.MoveMemberRequest
import com.familyreunion.rsvp.service.FamilyTreeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/family-tree")
class FamilyTreeController(private val familyTreeService: FamilyTreeService) {

    @GetMapping
    fun getFamilyTree(): ResponseEntity<FamilyTreeResponse> {
        return ResponseEntity.ok(familyTreeService.buildTree())
    }

    @PostMapping("/members")
    fun addMember(@Valid @RequestBody request: FamilyMemberRequest): ResponseEntity<FamilyTreeNode> {
        val node = familyTreeService.addMember(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(node)
    }

    @PutMapping("/members/{id}")
    fun updateMember(
        @PathVariable id: Long,
        @Valid @RequestBody request: FamilyMemberRequest
    ): ResponseEntity<FamilyTreeNode> {
        val node = familyTreeService.updateMember(id, request)
        return ResponseEntity.ok(node)
    }

    @PatchMapping("/members/{id}/move")
    fun moveMember(
        @PathVariable id: Long,
        @RequestBody request: MoveMemberRequest
    ): ResponseEntity<FamilyTreeNode> {
        val node = familyTreeService.moveMember(id, request)
        return ResponseEntity.ok(node)
    }

    @DeleteMapping("/members/{id}")
    fun deleteMember(@PathVariable id: Long): ResponseEntity<Void> {
        familyTreeService.deleteMember(id)
        return ResponseEntity.noContent().build()
    }
}
