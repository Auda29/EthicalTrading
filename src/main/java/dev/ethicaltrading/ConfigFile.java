package dev.ethicaltrading;
import com.google.gson.*;
import dev.ethicaltrading.core.WelfareConfig;
import java.io.IOException;
import java.nio.file.*;
public final class ConfigFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private ConfigFile() {}
    public static WelfareConfig load(Path path) throws IOException {
        JsonObject defaults = GSON.toJsonTree(WelfareConfig.defaults()).getAsJsonObject();
        if (!Files.exists(path)) {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path, GSON.toJson(defaults) + "\n", StandardOpenOption.CREATE_NEW);
        }
        try {
            JsonObject input = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            for (var entry : input.entrySet()) {
                if (!defaults.has(entry.getKey()) || entry.getValue().isJsonNull())
                    throw new IllegalArgumentException("Unknown or null config key: " + entry.getKey());
                var expected = defaults.getAsJsonPrimitive(entry.getKey());
                if (!entry.getValue().isJsonPrimitive()) throw new IllegalArgumentException("Invalid value: " + entry.getKey());
                var value = entry.getValue().getAsJsonPrimitive();
                if (expected.isBoolean() ? !value.isBoolean() : !value.isNumber())
                    throw new IllegalArgumentException("Wrong config value type: " + entry.getKey());
                if (expected.isNumber() && !entry.getKey().equals("maximumStressPenalty")) value.getAsBigDecimal().intValueExact();
                defaults.add(entry.getKey(), value);
            }
            return GSON.fromJson(defaults, WelfareConfig.class);
        } catch (RuntimeException e) {
            throw new IOException("Invalid configuration " + path + ": " + e.getMessage(), e);
        }
    }
}
