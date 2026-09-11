package dev.ethicaltrading.mixin;
import dev.ethicaltrading.SleepSkip;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements SleepSkip {
    @Unique private long ethicalTrading$clockBefore;
    @Unique private long ethicalTrading$skipTick = Long.MIN_VALUE;
    @Inject(method="tick", at=@At("HEAD"))
    private void ethicalTrading$begin(java.util.function.BooleanSupplier timeLeft, CallbackInfo ci) {
        ethicalTrading$clockBefore = ((ServerLevel)(Object)this).getDefaultClockTime();
    }
    @Inject(method="wakeUpAllPlayers", at=@At("HEAD"))
    private void ethicalTrading$recordSkip(CallbackInfo ci) {
        var level = (ServerLevel)(Object)this;
        if (level.getDefaultClockTime() > ethicalTrading$clockBefore)
            // Vanilla increments gameTime after waking players, before entity ticks.
            ethicalTrading$skipTick = level.getGameTime() + 1;
    }
    @Override public boolean ethicalTrading$skippedThisTick() {
        return ((ServerLevel)(Object)this).getGameTime() == ethicalTrading$skipTick;
    }
}
