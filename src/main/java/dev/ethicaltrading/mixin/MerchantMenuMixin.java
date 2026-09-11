package dev.ethicaltrading.mixin;
import dev.ethicaltrading.WelfareCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
    @Shadow @Final private MerchantContainer tradeContainer;
    @Shadow @Final private Merchant trader;
    @Inject(method="quickMoveStack", at=@At("HEAD"), cancellable=true)
    private void ethicalTrading$guardShiftClick(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index == 2 && trader instanceof WelfareCarrier) {
            var offer = tradeContainer.getActiveOffer();
            if (offer == null || offer.isOutOfStock()) cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
