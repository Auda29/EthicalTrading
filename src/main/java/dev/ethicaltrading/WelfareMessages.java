package dev.ethicaltrading;
import dev.ethicaltrading.core.WelfareState;
import net.minecraft.network.chat.Component;
public final class WelfareMessages {
    private WelfareMessages() {}
    public static String reason(WelfareState state) {
        var c = EthicalTrading.config();
        boolean tired = state.sleepFraction(c) < 1, afraid = state.stressFraction(c) < 1;
        return tired && afraid ? "Übermüdet / Verängstigt" : tired ? "Übermüdet" : afraid ? "Verängstigt" : "";
    }
    public static Component title(Component original, WelfareState state) {
        String reason = reason(state);
        return reason.isEmpty() ? original : original.copy().append(" – " + reason);
    }
    public static Component explanation(WelfareState state) {
        var c = EthicalTrading.config();
        int percent = state.limit(100, c);
        if (percent == 100) return Component.literal("Ethical Trading: Erholt – normale Handelsmenge.");
        String hint = state.sleepFraction(c) < 1 ? " Ein erreichbares Bett und tatsächlicher Schlaf helfen." : " Eine ruhige Umgebung hilft.";
        return Component.literal("Ethical Trading: " + (percent == 0 ? "Handelsstreik – " : "") + reason(state)
                + " (Handelsmenge " + percent + " %)." + hint);
    }
}
