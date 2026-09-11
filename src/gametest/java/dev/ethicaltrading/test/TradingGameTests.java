package dev.ethicaltrading.test;
import dev.ethicaltrading.*;
import dev.ethicaltrading.core.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.*;
public class TradingGameTests {
    public static String lastTitle = "";
    @GameTest public void realTradeInteractionShowsReasonAndRejectsStrike(GameTestHelper h) {
        var v = h.spawn(EntityTypes.VILLAGER,1,1,1); v.setNoAi(true);
        var offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD,1),new ItemStack(Items.BREAD),10,1,0));
        v.setOffers(offers);
        var s = ((WelfareCarrier)v).ethicalTrading$state();
        var player = h.makeMockServerPlayerInLevel();
        for (int i=0;i<24000;i++) s.tick(true,false,false,WelfareConfig.defaults());
        v.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(lastTitle.contains("Übermüdet"), "Actual menu title explains fatigue to vanilla client");
        player.closeContainer();
        for (int i=0;i<48000;i++) s.tick(true,false,false,WelfareConfig.defaults());
        v.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(!v.isTrading(), "Strike rejects opening trade");
        for (int i=0;i<200;i++) s.tick(true,true,false,WelfareConfig.defaults());
        v.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(v.isTrading(), "Recovery restores real trade interaction");
        h.assertTrue(!lastTitle.contains("Übermüdet"), "Recovered title is clean");
        h.getLevel().clockManager().setTotalTicks(h.getLevel().dimensionType().defaultClock().orElseThrow(), 14000);
        h.getLevel().environmentAttributes().invalidateTickCache();
        for (int i=0;i<23999;i++) s.tick(true,false,false,WelfareConfig.defaults());
        v.tick();
        h.assertTrue(!v.isTrading(), "Open menu is closed when welfare capacity changes");
        player.discard(); h.succeed();
    }
    @GameTest(structure="ethical-trading-test:hall", maxTicks=600)
    public void reachableBedSleepsNaturallyButBlockedBedDoesNot(GameTestHelper h) {
        var level = h.getLevel();
        var clock = level.dimensionType().defaultClock().orElseThrow();
        level.clockManager().setTotalTicks(clock, 14000);
        level.clockManager().setPaused(clock, true);
        for (int x=0; x<14; x++) for (int z=0; z<6; z++) h.setBlock(x, 0, z, net.minecraft.world.level.block.Blocks.STONE);
        placeBed(h, 2, 2); placeBed(h, 10, 2);
        for (int y=1; y<=2; y++) {
            h.setBlock(6,y,2,net.minecraft.world.level.block.Blocks.STONE);
            h.setBlock(8,y,2,net.minecraft.world.level.block.Blocks.STONE);
            h.setBlock(7,y,1,net.minecraft.world.level.block.Blocks.STONE);
            h.setBlock(7,y,3,net.minecraft.world.level.block.Blocks.STONE);
        }
        var free = h.spawn(EntityTypes.VILLAGER, 1, 1, 2);
        var blocked = h.spawn(EntityTypes.VILLAGER, 7, 1, 2);
        setHome(h, free, 3, 2); setHome(h, blocked, 11, 2);
        for (var v : java.util.List.of(free, blocked)) {
            var s = ((WelfareCarrier)v).ethicalTrading$state();
            for (int i=0; i<72000; i++) s.tick(true, false, false, WelfareConfig.defaults());
        }
        h.runAfterDelay(450, () -> {
            h.assertTrue(free.isSleeping(), "Reachable bed: vanilla AI actually sleeps");
            h.assertValueEqual(((WelfareCarrier)free).ethicalTrading$state().sleepFraction(WelfareConfig.defaults()), 1.0, "Real sleep recovers from strike");
            h.assertTrue(!blocked.isSleeping(), "Blocked bed is not slept in");
            h.assertValueEqual(((WelfareCarrier)blocked).ethicalTrading$state().sleepFraction(WelfareConfig.defaults()), 0.0, "Assigned unreachable bed gives no recovery");
            h.succeed();
        });
    }
    static void placeBed(GameTestHelper h, int x, int z) {
        var state = net.minecraft.world.level.block.Blocks.BED.red().defaultBlockState()
                .setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.EAST);
        h.setBlock(x,1,z,state.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
        h.setBlock(x+1,1,z,state.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
    }
    static void setHome(GameTestHelper h, net.minecraft.world.entity.npc.villager.Villager v, int x, int z) {
        v.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME,
                net.minecraft.core.GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new net.minecraft.core.BlockPos(x,1,z))));
        v.getBrain().setActiveActivityIfPossible(net.minecraft.world.entity.schedule.Activity.REST);
    }
    @GameTest public void staleResultCannotBeTakenByClickOrShiftClick(GameTestHelper h) {
        var v = h.spawn(EntityTypes.VILLAGER, 1, 1, 1);
        var offer = new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD), 10, 1, 0);
        var offers = new MerchantOffers(); offers.add(offer); v.setOffers(offers);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        v.setTradingPlayer(player);
        var menu = new net.minecraft.world.inventory.MerchantMenu(1, player.getInventory(), v);
        menu.getSlot(0).set(new ItemStack(Items.EMERALD, 2));
        h.assertTrue(menu.getSlot(2).hasItem(), "Real merchant result was prepared");
        var s = ((WelfareCarrier)v).ethicalTrading$state();
        for (int i=0; i<72000; i++) s.tick(true, false, false, WelfareConfig.defaults());
        h.assertTrue(!menu.getSlot(2).mayPickup(player), "Stale output cannot be picked up after strike");
        h.assertTrue(menu.quickMoveStack(player, 2).isEmpty(), "Shift-click cannot bypass strike");
        menu.clicked(2,0,net.minecraft.world.inventory.ContainerInput.PICKUP,player);
        h.assertTrue(menu.getCarried().isEmpty(), "Normal click cannot bypass strike");
        h.assertValueEqual(menu.getSlot(0).getItem().getCount(), 2, "No payment consumed by rejected trade");
        h.assertValueEqual(offer.getUses(), 0, "Rejected trade does not increment uses");
        h.succeed();
    }
    @GameTest(structure="ethical-trading-test:hall", environment="ethical-trading-test:skip", maxTicks=350)
    public void actualPlayerSkipCreditsShortRealSleepButNotAwakeVillager(GameTestHelper h) {
        var level = h.getLevel();
        var clock = level.dimensionType().defaultClock().orElseThrow();
        level.clockManager().setTotalTicks(clock, 14000);
        level.clockManager().setPaused(clock, false);
        // Clock changed mid-tick by the fixture: refresh the cached daylight before ticking a sleeper.
        level.environmentAttributes().invalidateTickCache();
        level.updateSkyBrightness();
        h.assertTrue(level.isDarkOutside(), "Player-sleep fixture must start in actual darkness");
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, true, level.getServer());
        for (int x=0;x<14;x++) for (int z=0;z<6;z++) h.setBlock(x,0,z,net.minecraft.world.level.block.Blocks.STONE);
        placeBed(h,2,2); placeBed(h,10,2);
        var asleep = h.spawn(EntityTypes.VILLAGER, 3,1,2); asleep.setNoAi(true);
        var awake = h.spawn(EntityTypes.VILLAGER, 7,1,2); awake.setNoAi(true);
        for (var v : java.util.List.of(asleep, awake)) {
            var state = ((WelfareCarrier)v).ethicalTrading$state();
            for (int i=0;i<72000;i++) state.tick(true,false,false,WelfareConfig.defaults());
        }
        asleep.startSleeping(h.absolutePos(new net.minecraft.core.BlockPos(3,1,2)));
        var player = h.makeMockServerPlayerInLevel();
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(11.5,1,2.5)));
        player.startSleeping(h.absolutePos(new net.minecraft.core.BlockPos(11,1,2)));
        level.updateSleepingPlayerList();
        // The embedded test connection has no normal socket tick to call doTick.
        h.onEachTick(player::doTick);
        h.runAfterDelay(50, () -> h.assertValueEqual(((WelfareCarrier)asleep).ethicalTrading$state().sleepFraction(WelfareConfig.defaults()), 0.0, "Short sleep not yet sufficient normally"));
        h.runAfterDelay(130, () -> {
            h.assertTrue(!player.isSleeping(), "Actual server player was woken by night skip");
            h.assertTrue(level.getDefaultClockTime() >= 24000, "Actual world clock skipped to morning");
            h.assertValueEqual(((WelfareCarrier)asleep).ethicalTrading$state().sleepFraction(WelfareConfig.defaults()), 1.0, "Player skip credits observed sleep shorter than 200 ticks");
            h.assertValueEqual(((WelfareCarrier)awake).ethicalTrading$state().sleepFraction(WelfareConfig.defaults()), 0.0, "Awake villager gets neither catch-up penalty nor fake recovery");
            player.discard();
            h.succeed();
        });
    }
    @GameTest(structure="ethical-trading-test:hall", environment="ethical-trading-test:panic", maxTicks=3500)
    public void naturalZombiePanicHasGraceAndCalmRecovers(GameTestHelper h) {
        var level=h.getLevel(); var clock=level.dimensionType().defaultClock().orElseThrow();
        level.clockManager().setTotalTicks(clock,14000); level.clockManager().setPaused(clock,true);
        for(int x=0;x<14;x++) for(int z=0;z<6;z++) h.setBlock(x,0,z,net.minecraft.world.level.block.Blocks.STONE);
        var v=h.spawn(EntityTypes.VILLAGER,2,1,2);
        v.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);
        var zombie=h.spawn(EntityTypes.ZOMBIE,5,1,2); zombie.setNoAi(true);
        var s=((WelfareCarrier)v).ethicalTrading$state();
        h.runAfterDelay(120,()->{
            h.assertTrue(s.snapshot().stressTicks()>0,"Vanilla AI sensed zombie panic");
            h.assertValueEqual(s.stressFraction(WelfareConfig.defaults()),1.0,"Brief panic has no trading penalty");
        });
        h.runAfterDelay(1000,()->{
            h.assertTrue(s.stressFraction(WelfareConfig.defaults())<1,"Sustained natural panic restricts trade");
            h.assertTrue(WelfareMessages.reason(s).contains("Verängstigt"),"Fear reason shown");
            zombie.discard();
        });
        h.runAfterDelay(3000,()->{
            h.assertValueEqual(s.stressFraction(WelfareConfig.defaults()),1.0,"Calm recovers after threat removal");
            h.succeed();
        });
    }
    @GameTest(environment="ethical-trading-test:persistence", maxTicks=1600)
    public void realChunkUnloadReloadAndServerRestart(GameTestHelper h) throws Exception {
        var level=h.getLevel(); var clock=level.dimensionType().defaultClock().orElseThrow();
        level.clockManager().setTotalTicks(clock,6000); level.clockManager().setPaused(clock,true);
        level.environmentAttributes().invalidateTickCache();
        var marker=java.nio.file.Path.of("restart-fixture.txt");
        boolean read=System.getProperty("ethicalTrading.restartPhase", "write").equals("read");
        level.setChunkForced(64,64,true); level.getChunk(64,64);
        if (read) {
            var id=java.util.UUID.fromString(java.nio.file.Files.readString(marker).trim());
            h.runAfterDelay(40,()->{
                var loaded=level.getEntityInAnyDimension(id);
                h.assertTrue(loaded instanceof WelfareCarrier,"Saved villager loaded from chunk in NEW server JVM");
                h.assertValueEqual(((WelfareCarrier)loaded).ethicalTrading$state().snapshot(),new WelfareState.Snapshot(36000,0,0),"Restart neither resets nor increments welfare");
                h.assertValueEqual(((net.minecraft.world.entity.npc.villager.Villager)loaded).getOffers().get(0).getUses(),3,"Restart preserves real trade uses");
                System.out.println("[ETHICAL-PERSISTENCE] SECOND JVM read original villager " + id);
                loaded.discard(); level.setChunkForced(64,64,false); h.succeed();
            });
            return;
        }
        var v=new net.minecraft.world.entity.npc.villager.Villager(EntityTypes.VILLAGER,level);
        v.setPos(1025.5,-58,1025.5); v.setNoAi(true); v.setPersistenceRequired();
        level.setBlockAndUpdate(new net.minecraft.core.BlockPos(1025,-59,1025),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var s=((WelfareCarrier)v).ethicalTrading$state();
        for(int i=0;i<36000;i++) s.tick(true,false,false,WelfareConfig.defaults());
        var offers=new MerchantOffers(); var offer=new MerchantOffer(new ItemCost(Items.EMERALD,1),new ItemStack(Items.BREAD),10,1,0);
        for(int i=0;i<3;i++) offer.increaseUses(); offers.add(offer); v.setOffers(offers);
        level.addFreshEntity(v); var id=v.getUUID();
        java.nio.file.Files.writeString(marker,id.toString());
        int[] phase={0}; int[] ticks={0};
        h.onEachTick(()->{
            ticks[0]++;
            if (phase[0]==0 && ticks[0]>=40) { level.setChunkForced(64,64,false); phase[0]=1; }
            else if(phase[0]==1 && level.getEntityInAnyDimension(id)==null && v.isRemoved()) {
                h.assertValueEqual(v.getRemovalReason(),net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK,
                        "Wait for completed chunk unload, not only loss of ticking visibility");
                System.out.println("[ETHICAL-PERSISTENCE] Original entity unloaded at tick " + ticks[0]);
                level.clockManager().addTicks(clock,24000*20);
                level.setChunkForced(64,64,true); level.getChunk(64,64); phase[0]=2;
            } else if(phase[0]==2 && level.getEntityInAnyDimension(id) instanceof WelfareCarrier loaded) {
                h.assertTrue(loaded != (Object)v,"Reload created a new entity instance");
                h.assertValueEqual(loaded.ethicalTrading$state().snapshot(),new WelfareState.Snapshot(36000,0,0),"Unloaded chunk and 20 skipped clock days add no debt");
                System.out.println("[ETHICAL-PERSISTENCE] Chunk reloaded unchanged; retaining fixture for restart " + id);
                level.setChunkForced(64,64,false); phase[0]=3; h.succeed();
            }
        });
    }
    @GameTest public void entityNbtRoundTripPreservesWelfareAndOriginalOffer(GameTestHelper h) {
        var v = h.spawn(EntityTypes.VILLAGER, 1, 1, 1);
        var s = ((WelfareCarrier)v).ethicalTrading$state();
        for (int i=0; i<36000; i++) s.tick(true, false, true, WelfareConfig.defaults());
        var offer = new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD), 10, 1, 0);
        offer.increaseUses();
        var offers = new MerchantOffers(); offers.add(offer); v.setOffers(offers); v.getOffers();
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        v.saveWithoutId(out);
        var tag = out.buildResult();
        h.assertTrue(tag.contains("ethical_trading"), "Welfare is in actual entity NBT");
        var copy = new net.minecraft.world.entity.npc.villager.Villager(EntityTypes.VILLAGER, h.getLevel());
        copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
        h.assertValueEqual(((WelfareCarrier)copy).ethicalTrading$state().snapshot(), s.snapshot(), "Persistence exact");
        h.assertValueEqual(copy.getOffers().get(0).getMaxUses(), 5, "Limited after load");
        var restored = ((WelfareCarrier)copy).ethicalTrading$state();
        for (int i=0; i<2400; i++) restored.tick(false, true, false, WelfareConfig.defaults());
        h.assertValueEqual(copy.getOffers().get(0).getMaxUses(), 10, "NBT saved ORIGINAL maxUses");
        h.assertValueEqual(copy.getOffers().get(0).getUses(), 1, "NBT retained uses");
        h.succeed();
    }
    @GameTest public void villagerReceivesPersistentStateAndReversibleOfferLimits(GameTestHelper h) {
        var v = h.spawn(EntityTypes.VILLAGER, 1, 1, 1);
        h.assertTrue(v instanceof WelfareCarrier, "Villager must carry welfare state");
        var s = ((WelfareCarrier)v).ethicalTrading$state();
        var offer = new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD), 10, 1, 0);
        var offers = new MerchantOffers(); offers.add(offer); v.setOffers(offers);
        h.assertTrue(v.getOffers().get(0) == offer, "Offer identity unchanged");
        for (int i=0; i<24000; i++) s.tick(true, false, false, WelfareConfig.defaults());
        h.assertValueEqual(offer.getMaxUses(), 8, "Reduced capacity");
        for (int i=0; i<8; i++) offer.increaseUses();
        h.assertTrue(offer.isOutOfStock(), "Limit enforced");
        for (int i=0; i<200; i++) s.tick(true, true, false, WelfareConfig.defaults());
        h.assertValueEqual(offer.getMaxUses(), 10, "Original capacity restored");
        h.assertValueEqual(offer.getUses(), 8, "Real trade uses preserved");
        h.assertTrue(!offer.isOutOfStock(), "Recovery reopens remaining uses");
        h.succeed();
    }
}
