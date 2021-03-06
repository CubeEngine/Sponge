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
package org.spongepowered.common.accessor.entity.player;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerEntityAccessor {

    @Accessor("lastSentHealth") void accessor$lastSentHealth(final float lastSentHealth);

    @Accessor("lastSentFood") void accessor$lastSentFood(final int lastSentFood);

    @Accessor("lastSentExp") void accessor$lastSentExp(final int lastSentExp);

    @Accessor("canChatColor") boolean accessor$canChatColor();

    @Accessor("isChangingDimension") void accessor$isChangingDimension(final boolean isChangingDimension);

    @Accessor("seenCredits") boolean accessor$seenCredits();

    @Accessor("seenCredits") void accessor$seenCredits(final boolean seenCredits);

    @Accessor("enteredNetherPosition") void accessor$enteredNetherPosition(final Vector3d enteredNetherPosition);

    @Invoker("triggerDimensionChangeTriggers") void invoker$triggerDimensionChangeTriggers(final ServerWorld world);

}
