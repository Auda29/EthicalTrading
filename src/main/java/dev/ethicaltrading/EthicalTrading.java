package dev.ethicaltrading;
import dev.ethicaltrading.core.WelfareConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
public final class EthicalTrading implements ModInitializer {
    private static WelfareConfig config = WelfareConfig.defaults();
    public static WelfareConfig config() { return config; }
    @Override public void onInitialize() {
        try { config = ConfigFile.load(FabricLoader.getInstance().getConfigDir().resolve("ethical-trading.json")); }
        catch (java.io.IOException e) { throw new IllegalStateException("Ethical Trading configuration could not be loaded", e); }
        LoggerFactory.getLogger("ethical_trading").info("Ethical Trading enabled: real sleep, persistent stress, reversible trade limits");
    }
}
