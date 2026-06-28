package io.github.isquyet.entropybreath.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class MessageService {
    private static final String FALLBACK_LOCALE = "en_US";
    private static final List<String> BUNDLED_LOCALES = List.of(FALLBACK_LOCALE, "zh_CN");

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, YamlConfiguration> translations = new HashMap<>();

    public MessageService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        reload();
    }

    public void reload() {
        translations.clear();
        for (String locale : BUNDLED_LOCALES) {
            YamlConfiguration translation = loadBundledTranslation(locale);
            if (translation != null) {
                translations.put(locale, translation);
            }
        }
    }

    public Component component(CommandSender sender, String key, Object... args) {
        return component(localeKey(sender), key, args);
    }

    public void send(CommandSender sender, String key, Object... args) {
        sender.sendMessage(component(sender, key, args));
    }

    public Component prefix(CommandSender sender) {
        return component(sender, "prefix");
    }

    private Component component(String locale, String key, Object... args) {
        String template = translation(locale).getString(key);
        if (template == null) {
            template = translation(FALLBACK_LOCALE).getString(key, "<red>Missing message: " + key + "</red>");
        }
        return render(template, args);
    }

    private Component render(String template, Object... args) {
        String rendered = template;
        for (int index = 0; index < args.length; index++) {
            Object argument = args[index];
            String replacement = argument instanceof Component component
                    ? miniMessage.serialize(component)
                    : miniMessage.escapeTags(String.valueOf(argument));
            rendered = rendered.replace("{" + index + "}", replacement);
        }
        return miniMessage.deserialize(rendered);
    }

    private YamlConfiguration translation(String locale) {
        YamlConfiguration translation = translations.get(locale);
        if (translation != null) {
            return translation;
        }
        return translations.getOrDefault(FALLBACK_LOCALE, new YamlConfiguration());
    }

    private String localeKey(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return FALLBACK_LOCALE;
        }
        Locale locale = player.locale();
        String exact = locale.toString();
        if (translations.containsKey(exact)) {
            return exact;
        }
        if ("zh".equalsIgnoreCase(locale.getLanguage()) && translations.containsKey("zh_CN")) {
            return "zh_CN";
        }
        return FALLBACK_LOCALE;
    }

    private YamlConfiguration loadBundledTranslation(String locale) {
        String resourcePath = "lang/" + locale + ".yml";
        try (InputStream input = plugin.getResource(resourcePath)) {
            if (input == null) {
                plugin.getLogger().warning("Missing bundled language resource: " + resourcePath);
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not load bundled language resource " + resourcePath + ": " + exception.getMessage());
            return null;
        }
    }
}
