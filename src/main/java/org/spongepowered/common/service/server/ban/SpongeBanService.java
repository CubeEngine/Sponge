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
package org.spongepowered.common.service.server.ban;

import com.google.inject.Singleton;
import net.minecraft.server.management.BanList;
import net.minecraft.server.management.IPBanEntry;
import net.minecraft.server.management.IPBanList;
import net.minecraft.server.management.ProfileBanEntry;
import net.minecraft.server.management.UserListEntry;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanTypes;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.server.management.UserListAccessor;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.profile.SpongeGameProfile;
import org.spongepowered.common.util.UserListUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * The default implementation of {@link BanService}.
 *
 * <p>Many of the methods here are copied from {@link BanList}
 * or {@link IPBanList}, while the original methods have been changed
 * to delegate to the registered {@link BanService}. This allows bans to
 * function normally when the default {@link BanService} has not been replaced,
 * while allowing plugin-provided {@link BanService}s to be used for all aspects
 * of Vanilla bans.</p>
 */
@Singleton
public final class SpongeBanService implements BanService {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Collection<Ban> getBans() {
        final Collection<? extends Ban> bans = this.getProfileBans();
        bans.addAll((Collection) this.getIpBans());
        return (Collection<Ban>) bans;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Ban.Profile> getProfileBans() {
        final UserListAccessor<com.mojang.authlib.GameProfile, ProfileBanEntry> accessor =
            (UserListAccessor<com.mojang.authlib.GameProfile, ProfileBanEntry>) this.getUserBanList();
        accessor.invoker$removeExpired();
        return new ArrayList<>((Collection<Ban.Profile>) (Object) accessor.accessor$map().values());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Ban.IP> getIpBans() {
        final UserListAccessor<com.mojang.authlib.GameProfile, ProfileBanEntry> accessor = ((UserListAccessor<com.mojang.authlib.GameProfile, ProfileBanEntry>) this.getIPBanList());
        accessor.invoker$removeExpired();
        return new ArrayList<>((Collection<Ban.IP>) (Object) accessor.accessor$map().values());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<Ban.Profile> getBanFor(final GameProfile profile) {
        final UserListAccessor<com.mojang.authlib.GameProfile, ProfileBanEntry> accessor =
            (UserListAccessor<com.mojang.authlib.GameProfile, ProfileBanEntry>) this.getUserBanList();
        accessor.invoker$removeExpired();
        return Optional.ofNullable((Ban.Profile) accessor.accessor$map().get(accessor.invoker$getKeyForUser(SpongeGameProfile.toMcProfile(profile))));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<Ban.IP> getBanFor(final InetAddress address) {
        final UserListAccessor<String, IPBanEntry> accessor = ((UserListAccessor<String, IPBanEntry>) this.getIPBanList());

        accessor.invoker$removeExpired();
        return Optional.ofNullable((Ban.IP) accessor.accessor$map().get(accessor.invoker$getKeyForUser(((IPBanList) accessor).getIpFromAddress(new InetSocketAddress(address, 0)))));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isBanned(final GameProfile profile) {
        final UserListAccessor<com.mojang.authlib.GameProfile, ProfileBanEntry> accessor =
            (UserListAccessor<com.mojang.authlib.GameProfile, ProfileBanEntry>) this.getUserBanList();

        accessor.invoker$removeExpired();
        return accessor.accessor$map().containsKey(accessor.invoker$getKeyForUser(SpongeGameProfile.toMcProfile(profile)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isBanned(final InetAddress address) {
        final UserListAccessor<String, IPBanEntry> accessor = ((UserListAccessor<String, IPBanEntry>) this.getIPBanList());

        accessor.invoker$removeExpired();
        return accessor.accessor$map().containsKey(accessor.invoker$getKeyForUser(((IPBanList) accessor).getIpFromAddress(new InetSocketAddress(address, 0))));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean pardon(final GameProfile profile) {
        final Optional<Ban.Profile> ban = this.getBanFor(profile);
        final UserListAccessor<com.mojang.authlib.GameProfile, ProfileBanEntry> accessor =
            (UserListAccessor<com.mojang.authlib.GameProfile, ProfileBanEntry>) this.getUserBanList();
        accessor.invoker$removeExpired();
        return ban.isPresent() && this.removeBan(ban.get());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean pardon(final InetAddress address) {
        final Optional<Ban.IP> ban = this.getBanFor(address);
        final UserListAccessor<String, IPBanEntry> accessor = ((UserListAccessor<String, IPBanEntry>) this.getIPBanList());
        accessor.invoker$removeExpired();
        return ban.isPresent() && this.removeBan(ban.get());
    }

    @Override
    public boolean removeBan(final Ban ban) {
        if (!this.hasBan(ban)) {
            return false;
        }
        if (ban.getType().equals(BanTypes.PROFILE.get())) {
            final User user = Sponge.getServer().getUserManager().getOrCreate(((Ban.Profile) ban).getProfile());
            Sponge.getEventManager().post(SpongeEventFactory.createPardonUserEvent(PhaseTracker.getCauseStackManager().getCurrentCause(), (Ban.Profile) ban, user));

            UserListUtil.removeEntry(this.getUserBanList(), ((Ban.Profile) ban).getProfile());
            return true;
        } else if (ban.getType().equals(BanTypes.IP.get())) {
            Sponge.getEventManager().post(SpongeEventFactory.createPardonIpEvent(PhaseTracker.getCauseStackManager().getCurrentCause(), (Ban.IP) ban));

            final InetSocketAddress inetSocketAddress = new InetSocketAddress(((Ban.IP) ban).getAddress(), 0);
            UserListUtil.removeEntry(this.getIPBanList(), this.getIPBanList().getIpFromAddress(inetSocketAddress));
            return true;
        }
        throw new IllegalArgumentException(String.format("Ban %s had unrecognized BanType %s!", ban, ban.getType()));
    }

    @Override
    public Optional<? extends Ban> addBan(final Ban ban) {
        final Optional<? extends Ban> prevBan;

        if (ban.getType().equals(BanTypes.PROFILE.get())) {
            prevBan = this.getBanFor(((Ban.Profile) ban).getProfile());

            final User user = Sponge.getServer().getUserManager().getOrCreate(((Ban.Profile) ban).getProfile());
            Sponge.getEventManager().post(SpongeEventFactory.createBanUserEvent(PhaseTracker.getCauseStackManager().getCurrentCause(), (Ban.Profile) ban, user));

            UserListUtil.addEntry(this.getUserBanList(), (UserListEntry<?>) ban);
        } else if (ban.getType().equals(BanTypes.IP.get())) {
            prevBan = this.getBanFor(((Ban.IP) ban).getAddress());

            Sponge.getEventManager().post(SpongeEventFactory.createBanIpEvent(PhaseTracker.getCauseStackManager().getCurrentCause(), (Ban.IP) ban));

            UserListUtil.addEntry(this.getIPBanList(), (UserListEntry<?>) ban);
        } else {
            throw new IllegalArgumentException(String.format("Ban %s had unrecognized BanType %s!", ban, ban.getType()));
        }
        return prevBan;
    }

    @Override
    public boolean hasBan(final Ban ban) {
        if (ban.getType().equals(BanTypes.PROFILE.get())) {
            return this.isBanned(((Ban.Profile) ban).getProfile());
        } else if (ban.getType().equals(BanTypes.IP.get())) {
            return this.isBanned(((Ban.IP) ban).getAddress());
        }
        throw new IllegalArgumentException(String.format("Ban %s had unrecognized BanType %s!", ban, ban.getType()));
    }

    private BanList getUserBanList() {
        return SpongeCommon.getServer().getPlayerList().getBans();
    }

    private IPBanList getIPBanList() {
        return SpongeCommon.getServer().getPlayerList().getIpBans();
    }

}
