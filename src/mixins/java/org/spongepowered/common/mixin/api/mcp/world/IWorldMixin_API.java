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
package org.spongepowered.common.mixin.api.mcp.world;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.ProtoWorld;
import org.spongepowered.api.world.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.accessor.world.biome.BiomeContainerAccessor;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.plugin.PluginPhase;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.world.SpongeBlockChangeFlag;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Mixin(IWorld.class)
@Implements(@Interface(iface = ProtoWorld.class, prefix = "protoWorld$"))
public interface IWorldMixin_API {

    //@formatter:off
    @Shadow boolean shadow$hasChunk(int p_217354_1_, int p_217354_2_);
    @Shadow Random shadow$getRandom();
    @Shadow AbstractChunkProvider shadow$getChunkSource();
    //@formatter:on

    // MutableBiomeVolume

    @SuppressWarnings({"rawtypes", "ConstantConditions"})
    default boolean protoWorld$setBiome(final int x, final int y, final int z, final org.spongepowered.api.world.biome.Biome biome) {
        final IChunk iChunk = ((IWorldReader) (Object) this).getChunk(x >> 4, z >> 4, ChunkStatus.BIOMES, true);
        if (iChunk == null) {
            return false;
        }
        if (iChunk instanceof ProtoChunk) {
            return ((ProtoChunk) (Object) iChunk).setBiome(x, y, z, biome);
        } else {
            final Biome[] biomes = ((BiomeContainerAccessor) iChunk.getBiomes()).accessor$biomes();

            final int maskedX = x & BiomeContainer.HORIZONTAL_MASK;
            final int maskedY = MathHelper.clamp(y, 0, BiomeContainer.VERTICAL_MASK);
            final int maskedZ = z & BiomeContainer.HORIZONTAL_MASK;

            final int WIDTH_BITS = BiomeContainerAccessor.accessor$WIDTH_BITS();
            final int posKey = maskedY << WIDTH_BITS + WIDTH_BITS | maskedZ << WIDTH_BITS | maskedX;
            biomes[posKey] = (Biome) (Object) biome;

            return true;
        }
    }

    // Volume

    default Vector3i protoWorld$getBlockMin() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IWorld that isn't part of Sponge API: " + this.getClass());
    }

    default Vector3i protoWorld$getBlockMax() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IWorld that isn't part of Sponge API: " + this.getClass());
    }

    default Vector3i protoWorld$getBlockSize() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of IWorld that isn't part of Sponge API: " + this.getClass());
    }

    default boolean protoWorld$containsBlock(final int x, final int y, final int z) {
        return this.shadow$hasChunk(x >> 4, z >> 4);
    }

    default boolean protoWorld$isAreaAvailable(final int x, final int y, final int z) {
        return this.shadow$hasChunk(x >> 4, z >> 4);
    }

    // EntityVolume

    default Optional<Entity> protoWorld$getEntity(final UUID uuid) {
        return Optional.empty();
    }

    // RandomProvider

    @Intrinsic
    default Random protoWorld$getRandom() {
        return this.shadow$getRandom();
    }

    // ProtoWorld

    default Collection<Entity> protoWorld$spawnEntities(final Iterable<? extends Entity> entities) {
        final List<Entity> entitiesToSpawn = NonNullList.create();
        entities.forEach(entitiesToSpawn::add);
        final SpawnEntityEvent.Custom event = SpongeEventFactory
                .createSpawnEntityEventCustom(PhaseTracker.getCauseStackManager().getCurrentCause(), entitiesToSpawn);
        if (Sponge.getEventManager().post(event)) {
            return ImmutableList.of();
        }
        for (final Entity entity : event.getEntities()) {
            EntityUtil.processEntitySpawn(entity, Optional::empty);
        }

        final ImmutableList.Builder<Entity> builder = ImmutableList.builder();
        for (final Entity entity : event.getEntities()) {
            builder.add(entity);
        }
        return builder.build();
    }

    default boolean protoWorld$spawnEntity(final Entity entity) {
        Objects.requireNonNull(entity, "entity");

        return ((IWorld) this).addFreshEntity((net.minecraft.entity.Entity) entity);
    }

    // MutableBlockVolume

    default boolean protoWorld$setBlock(
        final int x, final int y, final int z, final org.spongepowered.api.block.BlockState blockState, final BlockChangeFlag flag) {

        if (!World.isInWorldBounds(new BlockPos(x, y, z))) {
            throw new PositionOutOfBoundsException(new Vector3i(x, y, z), Constants.World.BLOCK_MIN, Constants.World.BLOCK_MAX);
        }
        try (final @Nullable PhaseContext<@NonNull ?> context = PluginPhase.State.BLOCK_WORKER.switchIfNecessary(PhaseTracker.SERVER)) {
            if (context != null) {
                context.buildAndSwitch();
            }
            return ((IWorld) this).setBlock(new BlockPos(x, y, z), (BlockState) blockState, ((SpongeBlockChangeFlag) flag).getRawFlag());
        }
    }

}
