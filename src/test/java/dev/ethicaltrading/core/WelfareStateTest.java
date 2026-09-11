package dev.ethicaltrading.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WelfareStateTest {
    final WelfareConfig config = WelfareConfig.defaults();
    void ticks(WelfareState state, int count, boolean night, boolean sleeping, boolean panic) {
        for (int i = 0; i < count; i++) state.tick(night, sleeping, panic, config);
    }
    @Test void adequateRealSleepRecoversButAssignedBedAloneDoesNot() {
        var s = new WelfareState();
        ticks(s, 72000, true, false, false);
        ticks(s, 199, true, true, false);
        assertEquals(0, s.limit(10, config));
        ticks(s, 1, true, true, false);
        assertEquals(10, s.limit(10, config));
    }
    @Test void fragmentedSleepDoesNotCountAsOneSufficientPhase() {
        var s = new WelfareState();
        ticks(s, 72000, true, false, false);
        for (int i=0; i<4; i++) { ticks(s, 100, true, true, false); ticks(s, 1, true, false, false); }
        assertEquals(0, s.limit(10, config));
    }
    @Test void stressHasGraceAccumulatesAcrossScaresAndRecoversInCalm() {
        var s = new WelfareState();
        ticks(s, 100, false, false, true);
        assertEquals(10, s.limit(10, config));
        ticks(s, 100, false, false, false);
        assertEquals(10, s.limit(10, config));
        for (int i=0; i<30; i++) { ticks(s, 100, false, false, true); ticks(s, 10, false, false, false); }
        assertTrue(s.limit(10, config) < 10);
        assertTrue(s.limit(10, config) >= 5);
        ticks(s, 2400, false, false, false);
        assertEquals(10, s.limit(10, config));
    }
    @Test void penaltiesUseStrongerNotProductAndLowVolumeTradesSurviveUntilStrike() {
        var s = new WelfareState();
        ticks(s, 36000, true, false, true);
        assertEquals(5, s.limit(10, config));
        assertEquals(1, s.limit(1, config));
    }
    @Test void persistenceRetainsDebtStressAndPartialSleepWithoutOfflineCatchUp() {
        var s = new WelfareState();
        ticks(s, 72000, true, false, true);
        ticks(s, 100, true, true, false);
        var copy = WelfareState.restore(s.snapshot(), config);
        assertEquals(s.snapshot(), copy.snapshot());
        ticks(copy, 100, true, true, false);
        assertEquals(1, copy.sleepFraction(config));
        assertTrue(copy.stressFraction(config) < 1);
    }
    @Test void skippedNightNeverGivesAwakeVillagerSleepCredit() {
        var s = new WelfareState();
        ticks(s, 72000, true, false, false);
        s.onSkippedNight(false, config);
        assertEquals(0, s.limit(10, config));
        ticks(s, 20, true, true, false);
        s.onSkippedNight(true, config);
        assertEquals(10, s.limit(10, config));
    }
    @Test void tinySleepBeforeSkipIsNotEnoughAndDaytimeDoesNotCreateDebt() {
        var s = new WelfareState();
        ticks(s, 72000, false, false, false);
        assertEquals(10, s.limit(10, config));
        ticks(s, 72000, true, false, false);
        ticks(s, 19, true, true, false);
        s.onSkippedNight(true, config);
        assertEquals(0, s.limit(10, config));
    }
    @Test void oneNightIsFreeTwoRestrictSixStrike() {
        var s = new WelfareState();
        ticks(s, 12000, true, false, false);
        assertEquals(10, s.limit(10, config));
        ticks(s, 12000, true, false, false);
        assertEquals(8, s.limit(10, config));
        ticks(s, 48000, true, false, false);
        assertEquals(0, s.limit(10, config));
    }
}
