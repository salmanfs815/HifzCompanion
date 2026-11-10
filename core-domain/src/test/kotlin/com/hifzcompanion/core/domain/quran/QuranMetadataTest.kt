package com.hifzcompanion.core.domain.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranMetadataTest {
    private fun majority(vararg surahs: Int) = QuranMetadata.majorityPagesForSurahs(surahs.toList())

    @Test fun `shared page goes to the surah filling most of it`() {
        // Page 537: Al-Waqi'ah 77-96 (10 lines), Al-Hadid 1-3 (5 lines).
        // Page 534: Ar-Rahman 68-78 (6 lines), Al-Waqi'ah 1-16 (9 lines).
        assertEquals((534..537).toList(), majority(56))
        assertEquals((538..541).toList(), majority(57))
    }

    @Test fun `majority rule drops both edge pages when neighbours fill them`() {
        // Ar-Rahman shares 531 (11 of 15 lines) and 534 (6 of 15 lines).
        assertEquals((531..533).toList(), majority(55))
    }

    @Test fun `short surahs count a page only together`() {
        // Page 604: Al-Ikhlas 4 lines, Al-Falaq 5, An-Nas 6 (headers included).
        assertEquals(emptyList<Int>(), majority(112))
        assertEquals(emptyList<Int>(), majority(114))
        assertEquals(listOf(604), majority(112, 113))
    }

    @Test fun `every page sums to fifteen lines across its surahs`() {
        for (page in 3..604) {
            val total = QuranMetadata.referenceForPage(page).surahs.sumOf { QuranMetadata.surahLinesOnPage(page, it.number) }
            assertTrue("page $page has $total lines", total in 14..15)
        }
    }

    @Test fun `contiguous surah range covers every page in between`() {
        assertEquals((582..604).toList(), QuranMetadata.majorityPagesForSurahs((78..114).toList()))
    }

    @Test fun `touched rule keeps every page for memorization`() {
        assertEquals((534..537).toList(), QuranMetadata.pagesForSurahs(listOf(56), SurahPageRule.ALL_TOUCHED_PAGES))
    }

    @Test fun `ayah labels follow the standard Madinah layout`() {
        assertEquals("At-Takwir 1–29", QuranMetadata.ayahLabelForPage(586))
        assertEquals("Al-Ma'idah 77–82", QuranMetadata.ayahLabelForPage(121))
        assertEquals("Al-Waqi'ah 77–96; Al-Hadid 1–3", QuranMetadata.ayahLabelForPage(537))
    }
}
