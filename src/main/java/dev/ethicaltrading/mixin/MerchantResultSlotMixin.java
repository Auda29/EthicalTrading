package dev.ethicaltrading.mixin;
import dev.ethicaltrading.WelfareCarrier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.*;
@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin extends Slot {
    @Shadow @Final private MerchantContainer slots;
    @Shadow @Final private Merchant merchant;
    protected MerchantResultSlotMixin(Container c, int i, int x, int y) { super(c,i,x,y); }
    @Override public boolean mayPickup(Player player) {
        if (!(merchant instanceof WelfareCarrier)) return super.mayPickup(player);
        var offer = slots.getActiveOffer();
        return offer != null && !offer.isOutOfStock() && super.mayPickup(player);
    }
}
