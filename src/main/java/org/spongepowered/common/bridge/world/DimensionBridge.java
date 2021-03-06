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
package org.spongepowered.common.bridge.world;

import net.kyori.adventure.text.Component;
import net.minecraft.util.ResourceLocation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.common.world.server.SpongeWorldTemplate;

import java.util.UUID;

public interface DimensionBridge {

    @Nullable
    Component bridge$displayName();

    ResourceLocation bridge$gameMode();

    @Nullable
    ResourceLocation bridge$difficulty();

    SerializationBehavior bridge$serializationBehavior();

    @Nullable
    UUID bridge$uniqueId();

    @Nullable
    Integer bridge$viewDistance();

    boolean bridge$enabled();

    boolean bridge$keepLoaded();

    boolean bridge$loadOnStartup();

    boolean bridge$keepSpawnLoaded();

    boolean bridge$generateSpawnOnLoad();

    boolean bridge$hardcore();

    boolean bridge$commands();

    boolean bridge$pvp();

    void bridge$populateFromData(SpongeWorldTemplate.SpongeDataSection spongeData);

    void bridge$populateFromTemplate(SpongeWorldTemplate s);

    SpongeWorldTemplate bridge$asTemplate();
}
