package com.familyreunion.rsvp.exception

class FamilyMemberNotFoundException(id: Long) : RuntimeException("Family member not found with id: $id")
