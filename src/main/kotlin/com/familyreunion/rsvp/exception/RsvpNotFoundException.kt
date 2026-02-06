package com.familyreunion.rsvp.exception

class RsvpNotFoundException(id: Long) : RuntimeException("RSVP not found with id: $id")
