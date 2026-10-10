package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.dto.PillarPhotoInfo
import com.familyreunion.rsvp.exception.FamilyMemberNotFoundException
import com.familyreunion.rsvp.model.PillarPhoto
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.PillarPhotoRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
@Transactional
class PillarPhotoService(
    private val photoRepository: PillarPhotoRepository,
    private val familyMemberRepository: FamilyMemberRepository
) {

    @Transactional(readOnly = true)
    fun listVersions(): List<PillarPhotoInfo> =
        photoRepository.findAllVersions().map {
            PillarPhotoInfo(siblingId = it.siblingId, version = it.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli())
        }

    @Transactional(readOnly = true)
    fun get(siblingId: Long): PillarPhoto =
        photoRepository.findById(siblingId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "No photo for this pillar")
        }

    fun save(siblingId: Long, contentType: String, bytes: ByteArray): PillarPhotoInfo {
        val member = familyMemberRepository.findById(siblingId)
            .orElseThrow { FamilyMemberNotFoundException(siblingId) }
        // Pillars are the founders' children — don't let the public endpoint attach photos to anyone else.
        if (member.isFounder || member.parent?.isFounder != true) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Photos can only be added for the eleven pillars")
        }

        val photo = photoRepository.findById(siblingId).orElse(null) ?: PillarPhoto(siblingId = siblingId)
        photo.contentType = contentType
        photo.data = bytes
        photo.updatedAt = LocalDateTime.now()
        val saved = photoRepository.save(photo)
        return PillarPhotoInfo(saved.siblingId, saved.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli())
    }

    fun delete(siblingId: Long) {
        if (!photoRepository.existsById(siblingId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No photo for this pillar")
        }
        photoRepository.deleteById(siblingId)
    }
}
