package dev.ethicaltrading.test;

import dev.ethicaltrading.WelfareCarrier;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

/** Acceptance test: no direct WelfareState.tick(), no seeded debt, no clock jumps after setup. */
public class NightBoundaryGameTests {
    @GameTest(environment="ethical-trading-test:nights", maxTicks=145000)
    public void sixRealNightsProgressFromFreeTradingToStrike(GameTestHelper h) {
        var level = h.getLevel();
        var clock = level.dimensionType().defaultClock().orElseThrow();
        level.clockManager().setTotalTicks(clock, 10);
        level.clockManager().setPaused(clock, false);
        level.getGameRules().set(GameRules.ADVANCE_TIME, true, level.getServer());
        level.environmentAttributes().invalidateTickCache();
        level.updateSkyBrightness();
        h.setBlock(1, 0, 1, Blocks.STONE);
        var villager = h.spawn(EntityTypes.VILLAGER, 1, 1, 1);
        // Isolate the night sensor from unrelated wandering/hostiles; never force the sleep state.
        villager.setNoAi(true);
        villager.setInvulnerable(true);
        var offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD), 10, 1, 0));
        villager.setOffers(offers);
        var offer = villager.getOffers().getFirst();
        var state = ((WelfareCarrier)villager).ethicalTrading$state();
        h.assertValueEqual(state.snapshot().missedSleepTicks(), 0L, "Fresh villager has no seeded debt");
        int[] expected = {10, 8, 6, 4, 2, 0};
        int[] completed = {0};
        long[] lastClock = {level.getDefaultClockTime()};
        long startGameTime = level.getGameTime();
        h.onEachTick(() -> {
            long now = level.getDefaultClockTime();
            h.assertTrue(now - lastClock[0] >= 0 && now - lastClock[0] <= 1,
                    "Vanilla clock must advance without skips or synthetic jumps");
            lastClock[0] = now;
            h.assertTrue(!villager.isSleeping(), "No bed and no synthetic sleep");
            h.assertValueEqual(state.snapshot().stressTicks(), 0, "Night test excludes stress");
            if (completed[0] == 0 && now < 12000)
                h.assertValueEqual(state.snapshot().missedSleepTicks(), 0L, "Loaded daytime is debt-free");
            if (completed[0] < 6 && now >= (completed[0] + 1L) * 24000L + 10L) {
                int night = ++completed[0];
                h.assertValueEqual(offer.getMaxUses(), expected[night - 1], "Real night " + night + " capacity");
                h.assertValueEqual(offer.getUses(), 0, "Night restrictions do not invent trades");
                h.assertTrue(level.getGameTime() - startGameTime >= night * 24000L - 1,
                        "Each elapsed night includes actual server game ticks");
                System.out.println("[ETHICAL-NIGHTS] night=" + night + " clock=" + now
                        + " loadedGameTicks=" + (level.getGameTime() - startGameTime)
                        + " debt=" + state.snapshot().missedSleepTicks() + " capacity=" + offer.getMaxUses());
                if (night == 6) {
                    h.assertTrue(offer.isOutOfStock(), "Six real nights cause an actual offer strike");
                    h.succeed();
                }
            }
        });
    }
}
