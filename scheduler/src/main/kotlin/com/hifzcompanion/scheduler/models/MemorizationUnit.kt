package com.hifzcompanion.scheduler.models

sealed class MemorizationUnit {
    data class Page(val number: Int) : MemorizationUnit()
    data class Juz(val number: Int) : MemorizationUnit()
    data class Surah(val number: Int, val name: String) : MemorizationUnit()
}
