package com.familyreunion.rsvp.exception

class EventNotFoundException(id: Long) : RuntimeException("Event not found with id: $id")
