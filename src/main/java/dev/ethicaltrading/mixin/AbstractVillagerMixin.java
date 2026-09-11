package dev.ethicaltrading.mixin;
import dev.ethicaltrading.*;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
    @Inject(method = "getOffers", at = @At("RETURN"))
    private void ethicalTrading$bindOffers(CallbackInfoReturnable<MerchantOffers> cir) {
        if ((Object)this instanceof WelfareCarrier carrier)
            for (var offer : cir.getReturnValue()) ((OfferWelfare)offer).ethicalTrading$bind(carrier);
    }
}
