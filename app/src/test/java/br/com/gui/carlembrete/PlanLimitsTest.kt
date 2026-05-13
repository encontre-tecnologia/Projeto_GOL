package br.com.gui.carlembrete

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanLimitsTest {
    @Test
    fun reminderLimitsMatchPlanStrategy() {
        assertEquals(5, reminderLimitForPlan(PlanTier.FREE))
        assertEquals(50, reminderLimitForPlan(PlanTier.LITE))
        assertEquals(300, reminderLimitForPlan(PlanTier.FROTA))
        assertEquals(Int.MAX_VALUE, reminderLimitForPlan(PlanTier.ENTERPRISE))
    }

    @Test
    fun scannerLimitsMatchPlanStrategy() {
        assertEquals(3, scannerLimitForPlan(PlanTier.FREE))
        assertEquals(30, scannerLimitForPlan(PlanTier.LITE))
        assertEquals(200, scannerLimitForPlan(PlanTier.FROTA))
        assertEquals(Int.MAX_VALUE, scannerLimitForPlan(PlanTier.ENTERPRISE))
    }

    @Test
    fun frotaBlocksMaintenanceWhenAvisosAndRegistrosReach300() {
        assertTrue(canCreateMore(reminderLimitForPlan(PlanTier.FROTA), currentCount = 299, newCount = 1))
        assertFalse(canCreateMore(reminderLimitForPlan(PlanTier.FROTA), currentCount = 300, newCount = 1))
        assertFalse(canCreateMore(reminderLimitForPlan(PlanTier.FROTA), currentCount = 200, newCount = 101))
    }

    @Test
    fun enterpriseAllowsMaintenanceAbove300() {
        assertTrue(canCreateMore(reminderLimitForPlan(PlanTier.ENTERPRISE), currentCount = 300, newCount = 1))
        assertTrue(canCreateMore(reminderLimitForPlan(PlanTier.ENTERPRISE), currentCount = 1_000, newCount = 500))
    }

    @Test
    fun fuelRecordsUseSameLimitsForFrotaAndEnterprise() {
        assertEquals(reminderLimitForPlan(PlanTier.FROTA), fuelRecordLimitForPlan(PlanTier.FROTA))
        assertEquals(reminderLimitForPlan(PlanTier.ENTERPRISE), fuelRecordLimitForPlan(PlanTier.ENTERPRISE))

        assertTrue(canCreateMore(fuelRecordLimitForPlan(PlanTier.FROTA), currentCount = 299, newCount = 1))
        assertFalse(canCreateMore(fuelRecordLimitForPlan(PlanTier.FROTA), currentCount = 300, newCount = 1))
        assertTrue(canCreateMore(fuelRecordLimitForPlan(PlanTier.ENTERPRISE), currentCount = 300, newCount = 200))
    }

    private fun canCreateMore(limit: Int, currentCount: Int, newCount: Int): Boolean {
        if (newCount <= 0 || limit == Int.MAX_VALUE) return true
        return currentCount + newCount <= limit
    }
}
