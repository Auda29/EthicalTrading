package dev.ethicaltrading.test.mixin;
import dev.ethicaltrading.test.TradingGameTests;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayer.class)
public abstract class MenuCapture {
    @Inject(method="openMenu", at=@At("HEAD"))
    private void capture(MenuProvider provider, CallbackInfoReturnable<java.util.OptionalInt> cir) {
        TradingGameTests.lastTitle = provider.getDisplayName().getString();
    }
}
