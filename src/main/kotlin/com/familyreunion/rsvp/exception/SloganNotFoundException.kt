package com.familyreunion.rsvp.exception

class SloganNotFoundException(id: Long) : RuntimeException("Slogan not found with id: $id")
