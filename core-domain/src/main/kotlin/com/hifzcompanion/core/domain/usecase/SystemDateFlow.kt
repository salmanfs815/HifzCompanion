package com.hifzcompanion.core.domain.usecase

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

internal fun systemDateFlow(): Flow<LocalDate> = flow {
    while (currentCoroutineContext().isActive) {
        val now = ZonedDateTime.now()
        emit(now.toLocalDate())
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L))
    }
}
