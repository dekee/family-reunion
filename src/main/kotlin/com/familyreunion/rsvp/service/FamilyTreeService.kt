package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.FamilyMemberRequest
import com.familyreunion.rsvp.dto.FamilyTreeNode
import com.familyreunion.rsvp.dto.FamilyTreeResponse
import com.familyreunion.rsvp.dto.MoveMemberRequest
import com.familyreunion.rsvp.exception.FamilyMemberNotFoundException
import com.familyreunion.rsvp.model.FamilyMember
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FamilyTreeService(private val familyMemberRepository: FamilyMemberRepository) {

    fun buildTree(): FamilyTreeResponse {
        val founders = familyMemberRepository.findByIsFounderTrue()
        val allMembers = familyMemberRepository.findAll()
        val roots = founders.map { toNode(it) }
        return FamilyTreeResponse(
            roots = roots,
            totalMembers = allMembers.size
        )
    }

    @Transactional
    fun addMember(request: FamilyMemberRequest): FamilyTreeNode {
        val parent = request.parentId?.let {
            familyMemberRepository.findById(it)
                .orElseThrow { FamilyMemberNotFoundException(it) }
        }

        val generation = request.generation ?: parent?.let { (it.generation ?: 0) + 1 } ?: 0

        val member = FamilyMember(
            name = request.name,
            ageGroup = request.ageGroup,
            parent = parent,
            generation = generation
        )

        val saved = familyMemberRepository.save(member)
        return toNode(saved)
    }

    @Transactional
    fun updateMember(id: Long, request: FamilyMemberRequest): FamilyTreeNode {
        val member = familyMemberRepository.findById(id)
            .orElseThrow { FamilyMemberNotFoundException(id) }

        member.name = request.name
        member.ageGroup = request.ageGroup
        if (request.generation != null) {
            member.generation = request.generation
        }

        val saved = familyMemberRepository.save(member)
        return toNode(saved)
    }

    @Transactional
    fun moveMember(id: Long, request: MoveMemberRequest): FamilyTreeNode {
        val member = familyMemberRepository.findById(id)
            .orElseThrow { FamilyMemberNotFoundException(id) }

        val newParent = request.newParentId?.let {
            familyMemberRepository.findById(it)
                .orElseThrow { FamilyMemberNotFoundException(it) }
        }

        member.parent = newParent
        val newGeneration = newParent?.let { (it.generation ?: 0) + 1 } ?: 0
        member.generation = newGeneration

        val saved = familyMemberRepository.save(member)
        updateDescendantGenerations(saved)
        return toNode(saved)
    }

    @Transactional
    fun deleteMember(id: Long) {
        val member = familyMemberRepository.findById(id)
            .orElseThrow { FamilyMemberNotFoundException(id) }
        familyMemberRepository.delete(member)
    }

    private fun updateDescendantGenerations(member: FamilyMember) {
        for (child in member.children) {
            child.generation = (member.generation ?: 0) + 1
            familyMemberRepository.save(child)
            updateDescendantGenerations(child)
        }
    }

    private fun toNode(member: FamilyMember): FamilyTreeNode {
        return FamilyTreeNode(
            id = member.id,
            name = member.name,
            generation = member.generation,
            ageGroup = member.ageGroup,
            parentId = member.parent?.id,
            children = member.children.map { toNode(it) }
        )
    }
}
