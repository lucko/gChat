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

package me.lucko.gchat.api;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * API for gChat
 */
public interface GChatApi {

    /**
     * Registers a placeholder with gChat
     *
     * @param placeholder the placeholder
     * @return true if the placeholder wasn't already registered
     */
    boolean registerPlaceholder(Placeholder placeholder);

    /**
     * Unregisters a placeholder with gChat
     *
     * @param placeholder the placeholder
     * @return true if the placeholder was previously registered
     */
    boolean unregisterPlaceholder(Placeholder placeholder);

    /**
     * Gets an immutable set of the placeholders registered
     *
     * @return a set of placeholders
     */
    Set<Placeholder> getPlaceholders();

    /**
     * Gets an immutable list of formats registered
     *
     * <p>The list is sorted in order of {@link ChatFormat#getPriority()}, with the highest priority
     * format coming first in the list.</p>
     *
     * @return a list of formats
     */
    List<ChatFormat> getFormats();

    /**
     * Performs a placeholder replacement on the given message
     *
     * @param player the player to replace in the context of
     * @param text the text containing the placeholders to be replaced
     * @return the replaced text
     */
    String replacePlaceholders(ProxiedPlayer player, String text);

    /**
     * Gets the most applicable chat format for a given player
     *
     * @param player the player
     * @return a chat format for the player, if any
     */
    Optional<ChatFormat> getFormat(ProxiedPlayer player);

    /**
     * Reloads the plugin from the config file
     *
     * @return true if the operation was successful
     */
    boolean reloadConfig();

}
