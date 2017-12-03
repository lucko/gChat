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

import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class GChatCommand extends Command {
    private static final TextComponent PREFIX = TextComponent.of("[").color(TextColor.GRAY).decoration(TextDecoration.BOLD, true)
            .append(TextComponent.of("gChat").color(TextColor.DARK_RED).decoration(TextDecoration.BOLD, true))
            .append(TextComponent.of("]").color(TextColor.GRAY).decoration(TextDecoration.BOLD, true))
            .append(TextComponent.of(" ").decoration(TextDecoration.BOLD, false));

    private final GChatPlugin plugin;

    public GChatCommand(GChatPlugin plugin) {
        super("gchat", null, "globalchat");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            TextComponent versionMsg = PREFIX.append(TextComponent.of("Running gChat ").color(TextColor.RED).decoration(TextDecoration.BOLD, false))
                    .append(TextComponent.of("v" + plugin.getDescription().getVersion()).color(TextColor.WHITE).decoration(TextDecoration.BOLD, false))
                    .append(TextComponent.of(".").color(TextColor.RED).decoration(TextDecoration.BOLD, false));

            sender.sendMessage(GChatPlugin.convertText(versionMsg));
            return;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload") && sender.hasPermission("gchat.command.reload")) {
            boolean result = plugin.reloadConfig();

            TextComponent reloadMsg;
            if (result) {
                reloadMsg = PREFIX.append(TextComponent.of("Reload successful.").color(TextColor.GREEN).decoration(TextDecoration.BOLD, false));
            } else {
                reloadMsg = PREFIX.append(TextComponent.of("Reload failed. Check the console for errors").color(TextColor.RED).decoration(TextDecoration.BOLD, false));
            }

            sender.sendMessage(GChatPlugin.convertText(reloadMsg));
            return;
        }

        TextComponent unknownCommand = PREFIX.append(TextComponent.of("Unknown sub command.").color(TextColor.WHITE).decoration(TextDecoration.BOLD, false));
        sender.sendMessage(GChatPlugin.convertText(unknownCommand));
    }
}
