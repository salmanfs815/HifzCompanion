package ca.salmanfs.hifzcompanion.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MadinahSurahPagesTest {
    @Test fun `last surahs map to final Madinah page`() {
        assertEquals(listOf(604), MadinahSurahPages.pagesForRange(112, 114))
    }

    @Test fun `reversed surah range is normalized`() {
        val pages = MadinahSurahPages.pagesForRange(114, 87)
        assertEquals(591, pages.first())
        assertEquals(604, pages.last())
        assertTrue(pages.size == pages.distinct().size)
    }
}
