package dev.ethicaltrading.core;
import dev.ethicaltrading.ConfigFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class ConfigTest {
    @TempDir Path dir;
    @Test void missingConfigIsCreatedAndPartialOverridesKeepDefaults() throws Exception {
        Path file = dir.resolve("ethical-trading.json");
        assertEquals(WelfareConfig.defaults(), ConfigFile.load(file));
        Files.writeString(file, "{\"sleepRequiredTicks\":400}");
        assertEquals(400, ConfigFile.load(file).sleepRequiredTicks());
        assertEquals(6, ConfigFile.load(file).strikeNight());
    }
    @Test void invalidConfigFailsClearlyRatherThanResettingFile() throws Exception {
        Path file = dir.resolve("bad.json");
        for (String bad : new String[]{"{\"nightTicks\":0}", "{\"strikeNight\":1}",
                "{\"maximumStressPenalty\":2}", "{\"unknown\":true}", "{\"sleepEnabled\":null}", "[]", "{broken"}) {
            Files.writeString(file, bad);
            assertThrows(Exception.class, () -> ConfigFile.load(file), bad);
            assertEquals(bad, Files.readString(file));
        }
    }
}
