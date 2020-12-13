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
package org.spongepowered.common.mixin.core.client;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.GameConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.listener.ChainedChunkStatusListener;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.world.chunk.listener.TrackingChunkStatusListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.server.MinecraftServerAccessor;
import org.spongepowered.common.bridge.client.MinecraftBridge;
import org.spongepowered.common.client.SpongeClient;
import org.spongepowered.common.entity.player.ClientType;
import org.spongepowered.common.event.tracking.PhaseTracker;

import java.net.Proxy;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements MinecraftBridge, SpongeClient {

    @Shadow private Thread gameThread;
    @Shadow @Nullable private IntegratedServer singleplayerServer;
    @Shadow @Final private AtomicReference<TrackingChunkStatusListener> progressListener;
    @Shadow @Final private Queue<Runnable> progressTasks;
    
    private IntegratedServer impl$temporaryIntegratedServer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void impl$setClientOnGame(final GameConfiguration gameConfig, final CallbackInfo ci) {
        SpongeCommon.getGame().setClient(this);
    }

    @Inject(method = "run", at = @At("HEAD"))
    private void impl$setThreadOnClientPhaseTracker(final CallbackInfo ci) {
        try {
            PhaseTracker.CLIENT.setThread(this.gameThread);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException("Could not initialize the client PhaseTracker!");
        }
    }

    @Inject(method = "runGameLoop", at = @At("TAIL"))
    private void impl$tickClientScheduler(boolean renderWorldIn, CallbackInfo ci) {
        this.getScheduler().tick();
    }

    // Note: worldSettingsIn is never null here, it is null checked and assigned before this point in the method.
    @Redirect(method = "doLoadLevel",
        at = @At(value = "NEW",
            target = "com/mojang/authlib/yggdrasil/YggdrasilAuthenticationService",
            remap = false
        )
    )
    private YggdrasilAuthenticationService impl$createServerBeforeCache(final Proxy proxy, final String clientToken, final String folderName,
            final String worldName, final WorldSettings worldSettingsIn) {

        final YggdrasilAuthenticationService service = new YggdrasilAuthenticationService(proxy, clientToken);
        this.singleplayerServer = new IntegratedServer((Minecraft) (Object) this, folderName, worldName, worldSettingsIn,
                service, service.createMinecraftSessionService(), service.createProfileRepository(), null, (p_213246_1_) -> {
            final TrackingChunkStatusListener trackingchunkstatuslistener = new TrackingChunkStatusListener(p_213246_1_ + 0);
            trackingchunkstatuslistener.start();
            this.progressListener.set(trackingchunkstatuslistener);
            return new ChainedChunkStatusListener(trackingchunkstatuslistener, this.progressTasks::add);
        });
        return service;
    }

    @Redirect(method = "doLoadLevel",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;createMinecraftSessionService()Lcom/mojang/authlib/minecraft/MinecraftSessionService;",
            remap = false
        )
    )
    private MinecraftSessionService impl$useServerMinecraftSessionService(final YggdrasilAuthenticationService yggdrasilAuthenticationService) {
        return ((MinecraftServerAccessor) this.singleplayerServer).accessor$sessionService();
    }

    @Redirect(method = "doLoadLevel",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;createProfileRepository()Lcom/mojang/authlib/GameProfileRepository;",
            remap = false
        )
    )
    private GameProfileRepository impl$useServerGameProfileRepository(final YggdrasilAuthenticationService yggdrasilAuthenticationService) {
        return ((MinecraftServerAccessor) this.singleplayerServer).accessor$profileRepository();
    }

    @Redirect(method = "doLoadLevel", at = @At(value = "NEW", target = "net/minecraft/server/integrated/IntegratedServer"))
    private IntegratedServer impl$setCacheOnServer(final Minecraft mcIn, final String worldName, final String p_i50895_3_,
            final WorldSettings worldSettingsIn, final YggdrasilAuthenticationService p_i50895_5_, final MinecraftSessionService p_i50895_6_,
            final GameProfileRepository p_i50895_7_, final PlayerProfileCache p_i50895_8_, final IChunkStatusListenerFactory p_i50895_9_) {
        ((MinecraftServerAccessor) this.singleplayerServer).accessor$profileCache(p_i50895_8_);
        return this.singleplayerServer;
    }

    @Override
    public IntegratedServer bridge$getTemporaryIntegratedServer() {
        return this.impl$temporaryIntegratedServer;
    }

    @Override
    public void bridge$setTemporaryIntegratedServer(final IntegratedServer server) {
        this.impl$temporaryIntegratedServer = server;
    }

    @Override
    public ClientType bridge$getClientType() {
        return ClientType.SPONGE_VANILLA;
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;shutdownExecutors()V"))
    private void impl$shutdownAsyncScheduler(final CallbackInfo ci) {
        SpongeCommon.getGame().getAsyncScheduler().close();
    }
}