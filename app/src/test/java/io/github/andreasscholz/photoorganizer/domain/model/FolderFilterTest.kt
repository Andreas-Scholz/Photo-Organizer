package io.github.andreasscholz.photoorganizer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderFilterTest {

    private val camera = "DCIM/Camera/"
    private val whatsApp = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/"

    @Test
    fun `all folders includes everything`() {
        assertTrue(FolderFilter.AllFolders.includes(camera))
        assertTrue(FolderFilter.AllFolders.includes(whatsApp))
    }

    @Test
    fun `selection includes only the chosen folders`() {
        val filter = FolderFilter.Only(setOf(camera))

        assertTrue(filter.includes(camera))
        assertFalse(filter.includes(whatsApp))
        assertFalse(filter.includes("DCIM/Camera/Burst/"))
    }

    @Test
    fun `folders can be added to and removed from a selection`() {
        val filter = FolderFilter.Only(setOf(camera))

        assertEquals(FolderFilter.Only(setOf(camera, whatsApp)), filter.with(whatsApp, selected = true))
        assertEquals(FolderFilter.Only(emptySet()), filter.with(camera, selected = false))
    }
}
