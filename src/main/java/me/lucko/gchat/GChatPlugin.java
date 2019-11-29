/*
 * This file is part of gChat, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.gchat;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.gchat.api.ChatFormat;
import me.lucko.gchat.api.GChatApi;
import me.lucko.gchat.api.Placeholder;
import me.lucko.gchat.config.GChatConfig;
import me.lucko.gchat.hooks.LuckPermsHook;
import me.lucko.gchat.placeholder.StandardPlaceholders;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GChatPlugin extends Plugin implements GChatApi {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^\\{\\}]+)\\}");

    @Getter
    private GChatConfig config;

    @Getter
    private Logger chatLogger;

    private final Set<Placeholder> placeholders = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        getLogger().info("Enabling gChat v" + getDescription().getVersion());

        // load configuration
        try {
            this.config = loadConfig();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }

        // init placeholder hooks
        placeholders.add(new StandardPlaceholders());

        // hook with luckperms
        if (getProxy().getPluginManager().getPlugin("LuckPerms") != null) {
            placeholders.add(new LuckPermsHook());
        }

        // register chat listener
        getProxy().getPluginManager().registerListener(this, new GChatListener(this));

        // register command
        getProxy().getPluginManager().registerCommand(this, new GChatCommand(this));

        // init api singleton
        GChat.setApi(this);
    }

    @Override
    public void onDisable() {
        // null the api singleton
        GChat.setApi(null);
    }

    @Override
    public boolean registerPlaceholder(@NonNull Placeholder placeholder) {
        return placeholders.add(placeholder);
    }

    @Override
    public boolean unregisterPlaceholder(@NonNull Placeholder placeholder) {
        return placeholders.remove(placeholder);
    }

    @Override
    public ImmutableSet<Placeholder> getPlaceholders() {
        return ImmutableSet.copyOf(placeholders);
    }

    @Override
    public List<ChatFormat> getFormats() {
        return config.getFormats();
    }

    @Override
    public String replacePlaceholders(ProxiedPlayer player, String text) {
        if (text == null || text.isEmpty() || placeholders.isEmpty()) {
            return text;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String definition = matcher.group(1);
            String replacement = null;

            for (Placeholder placeholder : placeholders) {
                replacement = placeholder.getReplacement(player, definition);
                if (replacement != null) {
                    break;
                }
            }

            if (replacement != null) {
                text = text.replace("{" + definition + "}", replacement);
            }
        }

        return text;
    }

    @Override
    public Optional<ChatFormat> getFormat(ProxiedPlayer player) {
        return config.getFormats().stream()
                .filter(f -> f.canUse(player))
                .findFirst();
    }

    @Override
    public boolean reloadConfig() {
        try {
            config = loadConfig();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private GChatConfig loadConfig() throws Exception {
        Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getBundledFile("config.yml"));
        GChatConfig gChatConfig = new GChatConfig(configuration);
        chatLogger = loadLogger(gChatConfig);
        return gChatConfig;
    }

    private File getBundledFile(String name) {
        File file = new File(getDataFolder(), name);

        if (!file.exists()) {
            getDataFolder().mkdir();
            try (InputStream in = getResourceAsStream(name)) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    public Logger loadLogger(GChatConfig config) {
        try {
            Logger logger = Logger.getLogger("gChat");
            logger.setUseParentHandlers(false);

            final Formatter formatter = new Formatter() {
                private final DateFormat date = new SimpleDateFormat("HH:mm:ss");

                @Override
                public String format(LogRecord record) {
                    StringBuilder formatted = new StringBuilder();

                    formatted.append(date.format(record.getMillis()));
                    formatted.append(" [");
                    formatted.append(record.getLevel().getLocalizedName());
                    formatted.append("] ");
                    formatted.append(formatMessage(record));
                    formatted.append('\n');

                    if (record.getThrown() != null) {
                        StringWriter writer = new StringWriter();
                        record.getThrown().printStackTrace(new PrintWriter(writer));
                        formatted.append(writer);
                    }

                    return formatted.toString();
                }
            };

            if (config.isLogChat()) {
                FileHandler logFile = new FileHandler(config.getLogFile(), true);
                logFile.setFormatter(formatter);
                logger.addHandler(logFile);
                getLogger().info("Logging chat to " + config.getLogFile());
            }

            if (config.isLogChatGlobal()) {
                logger.setParent(getLogger());
                logger.setUseParentHandlers(true);
                getLogger().info("Logging chat to console: " + getLogger().toString());
            }

            return logger;
        } catch (Exception e) {
            e.printStackTrace();
            return getLogger();
        }
    }
}
