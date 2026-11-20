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

    @Test fun `moving surah ends and starts leaves no page empty`() {
        for (page in 1..604) assertTrue("page $page is empty", QuranMetadata.linesOnPage(page) > 0)
    }

    @Test fun `a split surah joins whichever part is larger`() {
        // Al-'Adiyat 1-9 fills 6 lines of page 599, 10-11 one line of page 600.
        assertEquals("Az-Zalzalah 1–8; Al-'Adiyat 1–11", QuranMetadata.ayahLabelForPage(599))
        assertEquals("Al-Qari'ah 1–11; At-Takathur 1–8", QuranMetadata.ayahLabelForPage(600))
    }

    @Test fun `pages read in order cover every ayah exactly once`() {
        val sequence = (1..604).flatMap { page ->
            QuranMetadata.ayahLabelForPage(page).split("; ").map { it.substringBeforeLast(' ') to it.substringAfterLast(' ') }
        }
        var expectedSurah = 1
        var nextAyah = 1
        for ((name, ayahs) in sequence) {
            val (first, last) = ayahs.split('–').map(String::toInt)
            if (first == 1 && nextAyah != 1) { expectedSurah++; nextAyah = 1 }
            assertEquals(QuranMetadata.surahs[expectedSurah - 1].name, name)
            assertEquals("$name continues from $nextAyah", nextAyah, first)
            nextAyah = last + 1
        }
        assertEquals(114, expectedSurah)
    }

    @Test fun `a surah's end or start moves to its own neighbouring page`() {
        assertEquals("Al-Mumtahanah 6–13", QuranMetadata.ayahLabelForPage(550))
        assertEquals("As-Saff 1–5", QuranMetadata.ayahLabelForPage(551))
        assertEquals(549..550, QuranMetadata.surahs[59].pageRange)
        assertEquals(551..552, QuranMetadata.surahs[60].pageRange)
        assertEquals(listOf("As-Saff"), QuranMetadata.referenceForPage(551).surahs.map { it.name })
        // Al-Hadid 1-3 takes 5 lines of page 537, so it joins page 538.
        assertEquals("Al-Hadid 1–11", QuranMetadata.ayahLabelForPage(538))
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
        assertEquals("Al-Waqi'ah 77–96", QuranMetadata.ayahLabelForPage(537))
    }
}
