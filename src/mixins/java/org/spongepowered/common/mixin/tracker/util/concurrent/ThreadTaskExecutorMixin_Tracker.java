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
package org.spongepowered.common.mixin.tracker.util.concurrent;

import net.minecraft.util.concurrent.ThreadTaskExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.plugin.BasicPluginContext;
import org.spongepowered.common.event.tracking.phase.plugin.PluginPhase;
import org.spongepowered.common.hooks.PlatformHooks;

@Mixin(ThreadTaskExecutor.class)
public abstract class ThreadTaskExecutorMixin_Tracker<R extends Runnable> {

    @Shadow protected void shadow$doRunTask(final R taskIn) {} // Shadow

    @Redirect(method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Runnable;run()V",
                    remap = false))
    private void tracker$callOnMainThreadWithPhaseState(final Runnable runnable) {
        // This method can be called async while server is stopping
        if (this.tracker$isServerAndIsServerStopped() && !PlatformHooks.INSTANCE.getGeneralHooks().onServerThread()) {
            runnable.run();
            return;
        }

        try (final BasicPluginContext context = PluginPhase.State.SCHEDULED_TASK.createPhaseContext(PhaseTracker.getInstance())
                .source(runnable)) {
            context.buildAndSwitch();
            runnable.run();
        }
    }

    protected boolean tracker$isServerAndIsServerStopped() {
        return false;
    }
}
