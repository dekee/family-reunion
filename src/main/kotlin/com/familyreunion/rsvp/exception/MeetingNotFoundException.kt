package com.familyreunion.rsvp.exception

class MeetingNotFoundException(id: Long) : RuntimeException("Meeting not found with id: $id")
