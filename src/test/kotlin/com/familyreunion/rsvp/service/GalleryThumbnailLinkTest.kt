package com.familyreunion.rsvp.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GalleryThumbnailLinkTest {

    @Test
    fun `replaces the size hint Drive puts on a thumbnail link`() {
        val link = "https://lh3.googleusercontent.com/drive-storage/abc123=s220"
        assertThat(GalleryService.resizeLink(link, 400))
            .isEqualTo("https://lh3.googleusercontent.com/drive-storage/abc123=s400")
    }

    @Test
    fun `replaces a cropped size hint too`() {
        val link = "https://lh3.googleusercontent.com/drive-storage/abc123=s220-c"
        assertThat(GalleryService.resizeLink(link, 400))
            .isEqualTo("https://lh3.googleusercontent.com/drive-storage/abc123=s400")
    }

    @Test
    fun `appends a size hint when the link has none`() {
        val link = "https://lh3.googleusercontent.com/drive-storage/abc123"
        assertThat(GalleryService.resizeLink(link, 400)).isEqualTo("$link=s400")
    }

    @Test
    fun `does not mangle a size-like sequence in the middle of the link`() {
        // Only a trailing hint is a size directive; "=s99" mid-path is part of the id.
        val link = "https://lh3.googleusercontent.com/drive-storage/x=s99y/abc"
        assertThat(GalleryService.resizeLink(link, 400)).isEqualTo("$link=s400")
    }
}
