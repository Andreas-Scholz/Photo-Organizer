package io.github.andreasscholz.photoorganizer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumTest {

    @Test
    fun `new albums are placed in Pictures`() {
        assertEquals(Album("Holiday 2025", "Pictures/Holiday 2025/"), Album.create("  Holiday 2025 "))
    }

    @Test
    fun `names that are not a single folder name are rejected`() {
        assertTrue(Album.isValidName("Familie & Freunde"))
        assertFalse(Album.isValidName("   "))
        assertFalse(Album.isValidName(".."))
        assertFalse(Album.isValidName("a/b"))
        assertFalse(Album.isValidName("what?"))
    }
}
