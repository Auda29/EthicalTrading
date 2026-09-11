package dev.ethicaltrading.mixin;
import dev.ethicaltrading.WelfareCarrier;
import dev.ethicaltrading.EthicalTrading;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethicaltrading.core.WelfareState;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(Villager.class)
public abstract class VillagerMixin implements WelfareCarrier {
    @Unique private WelfareState ethicalTrading$welfare = new WelfareState();
    @Inject(method = "tick", at = @At("HEAD"))
    private void ethicalTrading$tick(CallbackInfo ci) {
        var v = (Villager)(Object)this;
        if (!(v.level() instanceof net.minecraft.server.level.ServerLevel level) || v.isBaby()) return;
        int previousCapacity = ethicalTrading$welfare.limit(1000000, EthicalTrading.config());
        if (((dev.ethicaltrading.SleepSkip)level).ethicalTrading$skippedThisTick())
            ethicalTrading$welfare.onSkippedNight(v.isSleeping(), EthicalTrading.config());
        boolean rest = level.canSleepThroughNights() && level.environmentAttributes().getValue(
                net.minecraft.world.attribute.EnvironmentAttributes.VILLAGER_ACTIVITY, v.position()) == net.minecraft.world.entity.schedule.Activity.REST;
        boolean panic = v.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.PANIC);
        ethicalTrading$welfare.tick(rest, v.isSleeping(), panic, EthicalTrading.config());
        if (previousCapacity != ethicalTrading$welfare.limit(1000000, EthicalTrading.config())
                && v.getTradingPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
            player.closeContainer();
            player.sendOverlayMessage(dev.ethicaltrading.WelfareMessages.explanation(ethicalTrading$welfare));
        }
    }
    @Inject(method="startTrading", at=@At("HEAD"), cancellable=true)
    private void ethicalTrading$explain(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (ethicalTrading$welfare.limit(100, EthicalTrading.config()) < 100 && player instanceof net.minecraft.server.level.ServerPlayer sp)
            sp.sendOverlayMessage(dev.ethicaltrading.WelfareMessages.explanation(ethicalTrading$welfare));
        if (ethicalTrading$welfare.limit(100, EthicalTrading.config()) == 0) ci.cancel();
    }
    @ModifyArg(method="startTrading", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/npc/villager/Villager;openTradingScreen(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;I)V"), index=1)
    private net.minecraft.network.chat.Component ethicalTrading$title(net.minecraft.network.chat.Component title) {
        return dev.ethicaltrading.WelfareMessages.title(title, ethicalTrading$welfare);
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ethicalTrading$save(ValueOutput output, CallbackInfo ci) {
        var tag = output.child("ethical_trading");
        var s = ethicalTrading$welfare.snapshot();
        tag.putInt("version", 1);
        tag.putLong("missed_sleep_ticks", s.missedSleepTicks());
        tag.putInt("stress_ticks", s.stressTicks());
        tag.putInt("sleep_streak", s.sleepStreak());
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ethicalTrading$load(ValueInput input, CallbackInfo ci) {
        var tag = input.childOrEmpty("ethical_trading");
        ethicalTrading$welfare = WelfareState.restore(new WelfareState.Snapshot(
                tag.getLongOr("missed_sleep_ticks", 0), tag.getIntOr("stress_ticks", 0),
                tag.getIntOr("sleep_streak", 0)), EthicalTrading.config());
    }
    @Override public WelfareState ethicalTrading$state() { return ethicalTrading$welfare; }
}
