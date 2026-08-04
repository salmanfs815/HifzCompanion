package ca.salmanfs.hifzcompanion.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.hifzcompanion.core.domain.quran.QuranMetadata

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

    @Test fun `juz boundaries use the 604 page Madinah layout`() {
        assertEquals((1..21).toList(), QuranMetadata.pagesForJuz(1))
        assertEquals((582..604).toList(), QuranMetadata.pagesForJuz(30))
        assertEquals(30, QuranMetadata.juzForPage(604))
    }

    @Test fun `page references include offline surah and ayah ranges`() {
        assertEquals("Al-Fatihah 1–7", QuranMetadata.referenceForPage(1).ayahLabel)
        assertEquals(
            "Al-Ikhlas 1–4; Al-Falaq 1–5; An-Nas 1–6",
            QuranMetadata.referenceForPage(604).ayahLabel
        )
    }
}
