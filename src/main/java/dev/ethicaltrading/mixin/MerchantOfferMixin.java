package dev.ethicaltrading.mixin;
import dev.ethicaltrading.*;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin implements OfferWelfare {
    @Shadow @Final private int maxUses;
    @Shadow private int uses;
    @Unique private WelfareCarrier ethicalTrading$owner;
    @Override public void ethicalTrading$bind(WelfareCarrier carrier) { ethicalTrading$owner = carrier; }
    @Inject(method = "getMaxUses", at = @At("HEAD"), cancellable = true)
    private void ethicalTrading$capacity(CallbackInfoReturnable<Integer> cir) {
        if (ethicalTrading$owner != null)
            cir.setReturnValue(ethicalTrading$owner.ethicalTrading$state().limit(maxUses, EthicalTrading.config()));
    }
    @Inject(method = "isOutOfStock", at = @At("HEAD"), cancellable = true)
    private void ethicalTrading$stock(CallbackInfoReturnable<Boolean> cir) {
        if (ethicalTrading$owner != null && uses >= ethicalTrading$owner.ethicalTrading$state().limit(maxUses, EthicalTrading.config()))
            cir.setReturnValue(true);
    }
}
