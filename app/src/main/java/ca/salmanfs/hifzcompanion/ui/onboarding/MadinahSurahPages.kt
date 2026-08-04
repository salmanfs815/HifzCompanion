package ca.salmanfs.hifzcompanion.ui.onboarding

object MadinahSurahPages {
    fun pagesForRange(firstSurah: Int, lastSurah: Int): List<Int> =
        com.hifzcompanion.core.domain.quran.QuranMetadata.pagesForSurahRange(firstSurah, lastSurah)
}
