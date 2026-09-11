package dev.ethicaltrading.core;
/** Loader-independent state machine. One call equals ONE observed entity tick. */
public final class WelfareState {
    private long missedSleepTicks;
    private int sleepStreak;
    private int stressTicks;
    public void tick(boolean restTime, boolean actuallySleeping, boolean panicking, WelfareConfig config) {
        if (config.stressEnabled()) stressTicks = panicking
                ? Math.min(config.stressMaxTicks(), stressTicks + 1)
                : Math.max(0, stressTicks - config.calmRecoveryPerTick());
        sleepStreak = actuallySleeping ? Math.min(config.sleepRequiredTicks(), sleepStreak + 1) : 0;
        if (sleepStreak >= config.sleepRequiredTicks()) missedSleepTicks = 0;
        if (restTime && !actuallySleeping && config.sleepEnabled())
            missedSleepTicks = Math.min((long) config.strikeNight() * config.nightTicks(), missedSleepTicks + 1);
    }
    public record Snapshot(long missedSleepTicks, int stressTicks, int sleepStreak) {}
    public Snapshot snapshot() { return new Snapshot(missedSleepTicks, stressTicks, sleepStreak); }
    public static WelfareState restore(Snapshot data, WelfareConfig config) {
        var result = new WelfareState();
        result.missedSleepTicks = Math.clamp(data.missedSleepTicks(), 0L, (long)config.strikeNight() * config.nightTicks());
        result.stressTicks = Math.clamp(data.stressTicks(), 0, config.stressMaxTicks());
        result.sleepStreak = Math.clamp(data.sleepStreak(), 0, config.sleepRequiredTicks());
        return result;
    }
    /** Called ONLY by Minecraft's actual player-sleep skip, never /time or load. */
    public void onSkippedNight(boolean actuallySleeping, WelfareConfig config) {
        if (actuallySleeping && sleepStreak >= config.skippedNightSleepTicks()) missedSleepTicks = 0;
    }
    public int limit(int originalMaxUses, WelfareConfig config) {
        if (originalMaxUses <= 0) return 0;
        double fraction = Math.min(sleepFraction(config), stressFraction(config));
        return fraction <= 0 ? 0 : Math.max(1, (int) Math.floor(originalMaxUses * fraction + 1e-9));
    }
    public double sleepFraction(WelfareConfig config) {
        long nights = missedSleepTicks / config.nightTicks();
        if (!config.sleepEnabled() || nights < config.firstRestrictedNight()) return 1;
        if (nights >= config.strikeNight()) return 0;
        return (double) (config.strikeNight() - nights) /
                (config.strikeNight() - config.firstRestrictedNight() + 1);
    }
    public double stressFraction(WelfareConfig config) {
        if (!config.stressEnabled() || stressTicks <= config.stressGraceTicks()) return 1;
        double severity = (double) (stressTicks - config.stressGraceTicks()) /
                (config.stressMaxTicks() - config.stressGraceTicks());
        // Five discrete tiers avoid sending a new offer packet on every stress tick.
        return 1 - Math.ceil(Math.min(1, severity) * 5) / 5 * config.maximumStressPenalty();
    }
}
