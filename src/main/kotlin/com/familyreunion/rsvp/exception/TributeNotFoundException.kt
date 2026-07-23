package com.familyreunion.rsvp.exception

class TributeNotFoundException(id: Long) : RuntimeException("Tribute not found with id: $id")
