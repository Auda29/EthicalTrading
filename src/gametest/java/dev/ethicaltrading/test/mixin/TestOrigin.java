package dev.ethicaltrading.test.mixin;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Test-only: repeatable origin instead of vanilla's +/-15-million-block lottery. */
@Mixin(GameTestServer.class)
public abstract class TestOrigin {
    @Redirect(method="startTests", at=@At(value="INVOKE",target="Lnet/minecraft/util/RandomSource;nextIntBetweenInclusive(II)I"))
    private int origin(RandomSource random,int min,int max) { return Integer.getInteger("ethicalTrading.testOrigin",0); }
}
