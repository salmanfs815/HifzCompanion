package com.hifzcompanion.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class HifzAnalyticsTest {
    @Test fun healthLabelsCoverBoundaryValues() {
        assertEquals("Excellent", HifzAnalytics.healthLabel(90))
        assertEquals("Good", HifzAnalytics.healthLabel(75))
        assertEquals("Needs attention", HifzAnalytics.healthLabel(60))
        assertEquals("Recovery recommended", HifzAnalytics.healthLabel(59))
    }
}
