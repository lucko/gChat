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

import lombok.Getter;
import lombok.NonNull;

import com.google.common.collect.ImmutableSet;

import me.lucko.gchat.api.ChatFormat;
import me.lucko.gchat.api.GChatApi;
import me.lucko.gchat.api.Placeholder;
import me.lucko.gchat.config.GChatConfig;
import me.lucko.gchat.hooks.LuckPermsHook;
import me.lucko.gchat.placeholder.StandardPlaceholders;

import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GChatPlugin extends Plugin implements GChatApi {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^\\{\\}]+)\\}");

    public static BaseComponent[] convertText(String text) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', text));
    }

    public static BaseComponent[] convertText(Component component) {
        return net.md_5.bungee.chat.ComponentSerializer.parse(ComponentSerializers.JSON.serialize(component));
    }

    @Getter
    private GChatConfig config;

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
        return new GChatConfig(configuration);
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
}
