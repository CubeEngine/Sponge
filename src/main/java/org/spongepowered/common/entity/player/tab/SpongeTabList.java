/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.entity.player.tab;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import net.kyori.adventure.text.Component;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerListHeaderFooterPacket;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.GameType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.player.tab.TabList;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.common.accessor.network.play.server.SPlayerListHeaderFooterPacketAccessor;
import org.spongepowered.common.accessor.network.play.server.SPlayerListItemPacketAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.profile.SpongeGameProfile;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SpongeTabList implements TabList {

    private static final ITextComponent EMPTY_COMPONENT = new StringTextComponent("");
    private final ServerPlayerEntity player;
    private @Nullable Component header;
    private @Nullable Component footer;
    private final Map<UUID, TabListEntry> entries = Maps.newHashMap();

    public SpongeTabList(final ServerPlayerEntity player) {
        this.player = player;
    }

    @Override
    public ServerPlayer getPlayer() {
        return (ServerPlayer) this.player;
    }

    @Override
    public Optional<Component> getHeader() {
        return Optional.ofNullable(this.header);
    }

    @Override
    public TabList setHeader(final @Nullable Component header) {
        this.header = header;

        this.refreshClientHeaderFooter();

        return this;
    }

    @Override
    public Optional<Component> getFooter() {
        return Optional.ofNullable(this.footer);
    }

    @Override
    public TabList setFooter(final @Nullable Component footer) {
        this.footer = footer;

        this.refreshClientHeaderFooter();

        return this;
    }

    @Override
    public TabList setHeaderAndFooter(@Nullable final Component header, @Nullable final Component footer) {
        // Do not call the methods, set directly
        this.header = header;
        this.footer = footer;

        this.refreshClientHeaderFooter();

        return this;
    }

    @SuppressWarnings("ConstantConditions")
    private void refreshClientHeaderFooter() {
        final SPlayerListHeaderFooterPacket packet = new SPlayerListHeaderFooterPacket();
        // MC-98180 - Sending null as header or footer will cause an exception on the client
        ((SPlayerListHeaderFooterPacketAccessor) packet).accessor$header(this.header == null ? SpongeTabList.EMPTY_COMPONENT : SpongeAdventure.asVanilla(this.header));
        ((SPlayerListHeaderFooterPacketAccessor) packet).accessor$footer(this.footer == null ? SpongeTabList.EMPTY_COMPONENT : SpongeAdventure.asVanilla(this.footer));
        this.player.connection.send(packet);
    }

    @Override
    public Collection<TabListEntry> getEntries() {
        return Collections.unmodifiableCollection(this.entries.values());
    }

    @Override
    public Optional<TabListEntry> getEntry(final UUID uniqueId) {
        checkNotNull(uniqueId, "unique id");
        return Optional.ofNullable(this.entries.get(uniqueId));
    }

    @Override
    public TabList addEntry(final TabListEntry entry) throws IllegalArgumentException {
        checkNotNull(entry, "builder");
        checkState(entry.getList().equals(this), "the provided tab list entry was not created for this tab list");

        this.addEntry(entry, true);

        return this;
    }

    private void addEntry(final SPlayerListItemPacket.AddPlayerData entry) {
        if (!this.entries.containsKey(entry.getProfile().getId())) {
            this.addEntry(new SpongeTabListEntry(
                    this,
                    SpongeGameProfile.of(entry.getProfile()),
                    entry.getDisplayName() == null ? null : SpongeAdventure.asAdventure(entry.getDisplayName()),
                    entry.getLatency(),
                    (GameMode) (Object) entry.getGameMode()
            ), false);
        }
    }

    private void addEntry(final TabListEntry entry, final boolean exceptionOnDuplicate) {
        final UUID uniqueId = entry.getProfile().getUniqueId();

        if (exceptionOnDuplicate) {
            checkArgument(!this.entries.containsKey(uniqueId), "cannot add duplicate entry");
        }

        if (!this.entries.containsKey(uniqueId)) {
            this.entries.put(uniqueId, entry);

            this.sendUpdate(entry, SPlayerListItemPacket.Action.ADD_PLAYER);
            entry.getDisplayName().ifPresent(text -> this.sendUpdate(entry, SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME));
            this.sendUpdate(entry, SPlayerListItemPacket.Action.UPDATE_LATENCY);
            this.sendUpdate(entry, SPlayerListItemPacket.Action.UPDATE_GAME_MODE);
        }
    }

    @Override
    public Optional<TabListEntry> removeEntry(final UUID uniqueId) {
        checkNotNull(uniqueId, "unique id");

        if (this.entries.containsKey(uniqueId)) {
            final TabListEntry entry = this.entries.remove(uniqueId);
            this.sendUpdate(entry, SPlayerListItemPacket.Action.REMOVE_PLAYER);
            return Optional.of(entry);
        }
        return Optional.empty();
    }

    /**
     * Send an entry update.
     *
     * @param entry The entry to update
     * @param action The update action to perform
     */
    @SuppressWarnings("ConstantConditions")
    void sendUpdate(final TabListEntry entry, final SPlayerListItemPacket.Action action) {
        final SPlayerListItemPacket packet = new SPlayerListItemPacket();
        ((SPlayerListItemPacketAccessor) packet).accessor$action(action);
        final SPlayerListItemPacket.AddPlayerData data = packet.new AddPlayerData(SpongeGameProfile.toMcProfile(entry.getProfile()),
            entry.getLatency(), (GameType) (Object) entry.getGameMode(),
            entry.getDisplayName().isPresent() ? SpongeAdventure.asVanilla(entry.getDisplayName().get()) : null);
        ((SPlayerListItemPacketAccessor) packet).accessor$entries().add(data);
        this.player.connection.send(packet);
    }

    /**
     * Update this tab list with data from the provided packet.
     *
     * <p>This method should not be called manually, it is automatically
     * called in the player's network connection when the packet is sent.</p>
     *
     * @param packet The packet to process
     */
    @SuppressWarnings("ConstantConditions")
    public void updateEntriesOnSend(final SPlayerListItemPacket packet) {
        for (final SPlayerListItemPacket.AddPlayerData data : ((SPlayerListItemPacketAccessor) packet).accessor$entries()) {
            final SPlayerListItemPacket.Action action = ((SPlayerListItemPacketAccessor) packet).accessor$action();
            if (action == SPlayerListItemPacket.Action.ADD_PLAYER) {
                // If an entry with the same id exists nothing will be done
                this.addEntry(data);
            } else if (action == SPlayerListItemPacket.Action.REMOVE_PLAYER) {
                this.removeEntry(data.getProfile().getId());
            } else {
                this.getEntry(data.getProfile().getId()).ifPresent(entry -> {
                    if (action == SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME) {
                        ((SpongeTabListEntry) entry).updateWithoutSend();
                        entry.setDisplayName(data.getDisplayName() == null ? null : SpongeAdventure.asAdventure(data.getDisplayName()));
                    } else if (action == SPlayerListItemPacket.Action.UPDATE_LATENCY) {
                        ((SpongeTabListEntry) entry).updateWithoutSend();
                        entry.setLatency(data.getLatency());
                    } else if (action == SPlayerListItemPacket.Action.UPDATE_GAME_MODE) {
                        ((SpongeTabListEntry) entry).updateWithoutSend();
                        entry.setGameMode((GameMode) (Object) data.getGameMode());
                    } else {
                        throw new IllegalArgumentException("unknown packet action: " + action);
                    }
                });
            }
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("player", this.player)
                .add("header", this.header)
                .add("footer", this.footer)
                .add("entries", this.entries)
                .toString();
    }

}
