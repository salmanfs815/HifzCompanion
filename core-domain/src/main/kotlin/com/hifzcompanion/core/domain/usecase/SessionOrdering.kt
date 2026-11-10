package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.models.SessionSortOrder
import com.hifzcompanion.scheduler.models.Progress

/**
 * Orders today's review pages for display. [this] must already be in scheduler priority order;
 * PAGE_NUMBER re-sorts by page while PRIORITY keeps the list as given.
 */
fun List<Progress>.inSessionOrder(order: SessionSortOrder): List<Progress> = when (order) {
    SessionSortOrder.PRIORITY -> this
    SessionSortOrder.PAGE_NUMBER -> sortedBy { it.unitId }
}
