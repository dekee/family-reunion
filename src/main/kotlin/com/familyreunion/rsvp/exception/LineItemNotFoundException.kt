package com.familyreunion.rsvp.exception

class LineItemNotFoundException(id: Long) : RuntimeException("Line item not found with id: $id")
