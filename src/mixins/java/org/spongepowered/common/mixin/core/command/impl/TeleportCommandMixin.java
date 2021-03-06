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
package org.spongepowered.common.mixin.core.command.impl;

import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.TeleportCommand;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RotateEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;

import java.util.Set;

import javax.annotation.Nullable;

@Mixin(TeleportCommand.class)
public abstract class TeleportCommandMixin {

    /**
     * @author Zidane
     * @reason Have the teleport command respect our events
     */
    @Overwrite
    private static void performTeleport(CommandSource source, Entity entityIn, ServerWorld worldIn, double x, double y, double z,
            Set<SPlayerPositionLookPacket.Flags> relativeList, float yaw, float pitch, @Nullable TeleportCommand.Facing facing) {

        double actualX = x;
        double actualY = y;
        double actualZ = z;
        double actualYaw = yaw;
        double actualPitch = pitch;

        if (!(entityIn instanceof ServerPlayerEntity)) {
            actualYaw = MathHelper.wrapDegrees(yaw);
            actualPitch = MathHelper.wrapDegrees(pitch);
            actualPitch = MathHelper.clamp(actualPitch, -90.0F, 90.0F);
        }

        if (worldIn == entityIn.level) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.COMMAND);

                // TODO Should honor the relative list before the event..

                if (ShouldFire.MOVE_ENTITY_EVENT) {
                    final MoveEntityEvent posEvent = SpongeEventFactory.createMoveEntityEvent(frame.getCurrentCause(),
                            (org.spongepowered.api.entity.Entity) entityIn, VecHelper.toVector3d(entityIn.position()),
                            new Vector3d(x, y, z), new Vector3d(x, y, z));

                    if (SpongeCommon.postEvent(posEvent)) {
                        return;
                    }

                    actualX = posEvent.getDestinationPosition().getX();
                    actualY = posEvent.getDestinationPosition().getY();
                    actualZ = posEvent.getDestinationPosition().getZ();
                }

                if (ShouldFire.ROTATE_ENTITY_EVENT) {
                    final RotateEntityEvent rotateEvent = SpongeEventFactory.createRotateEntityEvent(frame.getCurrentCause(),
                            (org.spongepowered.api.entity.Entity) entityIn, new Vector3d(actualPitch, actualYaw, 0),
                            new Vector3d(pitch, yaw, 0));

                    SpongeCommon.postEvent(rotateEvent);
                    actualYaw = rotateEvent.isCancelled() ? entityIn.yRot : rotateEvent.getToRotation().getY();
                    actualPitch = rotateEvent.isCancelled() ? entityIn.xRot : rotateEvent.getToRotation().getX();
                }

                if (entityIn instanceof ServerPlayerEntity) {

                    ChunkPos chunkpos = new ChunkPos(new BlockPos(actualX, actualY, actualZ));
                    worldIn.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 1, entityIn.getId());

                    entityIn.stopRiding();

                    if (((ServerPlayerEntity)entityIn).isSleeping()) {
                        ((ServerPlayerEntity)entityIn).stopSleepInBed(true, true);
                    }

                    ((ServerPlayerEntity)entityIn).connection.teleport(actualX, actualY, actualZ, (float) actualYaw, (float) actualPitch, relativeList);
                } else {
                    entityIn.moveTo(actualX, actualY, actualZ, (float) actualYaw, (float) actualPitch);
                }

                entityIn.setYHeadRot((float) actualYaw);
            }
        } else {
            if (entityIn instanceof ServerPlayerEntity) {
                // To ensure mod code is caught, handling the world change for players happens in teleport
                // Teleport will create a frame but we want to ensure it'll be the command movement type
                // TODO check if this is still correct
                PhaseTracker.getCauseStackManager().addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.COMMAND);
                ((ServerPlayerEntity) entityIn).teleportTo(worldIn, x, y, z, yaw, pitch);
                PhaseTracker.getCauseStackManager().removeContext(EventContextKeys.MOVEMENT_TYPE);
            } else {
                try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
                    frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.COMMAND);

                    final ServerWorld fromWorld = (ServerWorld) entityIn.getCommandSenderWorld();

                    final ChangeEntityWorldEvent.Pre preEvent = PlatformHooks.INSTANCE.getEventHooks().callChangeEntityWorldEventPre(entityIn,
                            worldIn);
                    if (SpongeCommon.postEvent(preEvent)) {
                        return;
                    }

                    final ChangeEntityWorldEvent.Reposition posEvent =
                            SpongeEventFactory.createChangeEntityWorldEventReposition(frame.getCurrentCause(),
                                    (org.spongepowered.api.entity.Entity) entityIn,
                                    (org.spongepowered.api.world.server.ServerWorld) entityIn.getCommandSenderWorld(),
                                    VecHelper.toVector3d(entityIn.position()), new Vector3d(x, y, z), preEvent.getOriginalDestinationWorld(),
                                    new Vector3d(x, y, z), preEvent.getDestinationWorld());

                    if (SpongeCommon.postEvent(posEvent)) {
                        return;
                    }

                    entityIn.unRide();
                    final Entity result = entityIn.getType().create(worldIn);
                    if (result == null) {
                        return;
                    }

                    if (ShouldFire.ROTATE_ENTITY_EVENT) {
                        final RotateEntityEvent rotateEvent = SpongeEventFactory.createRotateEntityEvent(frame.getCurrentCause(),
                                (org.spongepowered.api.entity.Entity) entityIn, new Vector3d(entityIn.xRot, entityIn.yRot, 0),
                                new Vector3d(actualPitch, actualYaw, 0));

                        if (!SpongeCommon.postEvent(rotateEvent)) {
                            actualYaw = MathHelper.wrapDegrees(rotateEvent.getToRotation().getY());
                            actualPitch = MathHelper.wrapDegrees(rotateEvent.getToRotation().getX());
                            actualPitch = MathHelper.clamp(actualPitch, -90.0F, 90.0F);
                        } else {
                            actualYaw = entityIn.yRot;
                            actualPitch = entityIn.xRot;
                        }
                    }

                    result.restoreFrom(entityIn);
                    result.moveTo(posEvent.getDestinationPosition().getX(), posEvent.getDestinationPosition().getY(),
                            posEvent.getDestinationPosition().getZ(), (float) actualYaw, (float) actualPitch);
                    result.setYHeadRot((float) actualYaw);
                    worldIn.addFromAnotherDimension(result);
                    entityIn.removed = true;

                    PlatformHooks.INSTANCE.getEventHooks().callChangeEntityWorldEventPost(result, fromWorld,
                            (ServerWorld) preEvent.getOriginalDestinationWorld());
                }
            }
        }

        if (facing != null) {
            facing.perform(source, entityIn);
        }

        if (!(entityIn instanceof LivingEntity) || !((LivingEntity)entityIn).isFallFlying()) {
            entityIn.setDeltaMovement(entityIn.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
            entityIn.setOnGround(true);
        }

        if (entityIn instanceof CreatureEntity) {
            ((CreatureEntity)entityIn).getNavigation().stop();
        }
    }
}
