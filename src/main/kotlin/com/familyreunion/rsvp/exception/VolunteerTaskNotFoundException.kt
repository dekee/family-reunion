package com.familyreunion.rsvp.exception

class VolunteerTaskNotFoundException(id: Long) : RuntimeException("Volunteer task not found with id: $id")
