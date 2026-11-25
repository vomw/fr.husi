package fr.husi.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PathLimitsTest {
    private val androidLimits = PathLimits.current
    private val chineseName = "中".repeat(100) + ".srs"

    @Test
    fun `Android filenames are measured in UTF-8 bytes`() {
        assertEquals(304, androidLimits.lengthOf(chineseName))
        assertFalse(androidLimits.acceptsName(chineseName))
    }

    @Test
    fun `Android name limit is the filesystem component limit`() {
        val longestName = "a".repeat(255)

        assertTrue(androidLimits.acceptsName(longestName))
        assertFalse(androidLimits.acceptsName(longestName + "a"))
    }
}
