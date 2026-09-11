package dev.ethicaltrading.core;
/** Tick durations are loaded server ticks, never wall-clock time. */
public record WelfareConfig(boolean sleepEnabled, boolean stressEnabled, int nightTicks,
        int firstRestrictedNight, int strikeNight, int sleepRequiredTicks, int skippedNightSleepTicks,
        int stressGraceTicks, int stressMaxTicks, int calmRecoveryPerTick, double maximumStressPenalty) {
    public WelfareConfig {
        if (nightTicks < 1 || nightTicks > 2400000 || firstRestrictedNight < 1 ||
                strikeNight < firstRestrictedNight || strikeNight > 10000 ||
                sleepRequiredTicks < 1 || sleepRequiredTicks > 2400000 ||
                skippedNightSleepTicks < 1 || skippedNightSleepTicks > sleepRequiredTicks ||
                stressGraceTicks < 0 || stressMaxTicks <= stressGraceTicks || stressMaxTicks > 2400000 ||
                calmRecoveryPerTick < 1 || calmRecoveryPerTick > 2400000 ||
                !Double.isFinite(maximumStressPenalty) || maximumStressPenalty < 0 || maximumStressPenalty > 1)
            throw new IllegalArgumentException("Invalid Ethical Trading thresholds; see CONFIGURATION.md");
    }
    public static WelfareConfig defaults() {
        return new WelfareConfig(true, true, 12000, 2, 6, 200, 20, 600, 2400, 1, 0.5);
    }
}
