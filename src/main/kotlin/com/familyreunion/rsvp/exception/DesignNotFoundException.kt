package com.familyreunion.rsvp.exception

class DesignNotFoundException(id: Long) : RuntimeException("Design not found with id: $id")
