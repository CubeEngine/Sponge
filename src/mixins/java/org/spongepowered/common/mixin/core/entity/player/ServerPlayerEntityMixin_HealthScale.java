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
package org.spongepowered.common.mixin.core.entity.player;

import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SEntityPropertiesPacket;
import net.minecraft.network.play.server.SUpdateHealthPacket;
import net.minecraft.util.FoodStats;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.entity.player.ServerPlayerEntityHealthScaleBridge;
import org.spongepowered.common.util.Constants;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin_HealthScale extends PlayerEntityMixin implements ServerPlayerEntityHealthScaleBridge {

    // @formatter:off
    @Shadow private float lastSentHealth;
    @Shadow public ServerPlayNetHandler connection;
    // @formatter:on

    private Double impl$healthScale = null;
    private float impl$cachedModifiedHealth = -1;

    @Override
    public void impl$writeToSpongeCompound(final CompoundNBT compound) {
        super.impl$writeToSpongeCompound(compound);
        if (this.bridge$isHealthScaled()) {
            compound.putDouble(Constants.Sponge.Entity.Player.HEALTH_SCALE, this.impl$healthScale);
        }
    }

    @Override
    public void impl$readFromSpongeCompound(final CompoundNBT compound) {
        super.impl$readFromSpongeCompound(compound);
        if (compound.contains(Constants.Sponge.Entity.Player.HEALTH_SCALE, Constants.NBT.TAG_DOUBLE)) {
            this.impl$healthScale = compound.getDouble(Constants.Sponge.Entity.Player.HEALTH_SCALE);
        }
    }

    @Inject(method = "doTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;getArmorValue()I", ordinal = 1))
    private void updateHealthPriorToArmor(final CallbackInfo ci) {
        this.bridge$refreshScaledHealth();
    }

    @Redirect(method = "doTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;getHealth()F"))
    public float impl$onGetHealth(final ServerPlayerEntity serverPlayerEntity) {
        return this.bridge$getInternalScaledHealth();
    }

    @Override
    public void bridge$setHealthScale(final Double scale) {
        this.impl$healthScale = scale;
        this.impl$cachedModifiedHealth = -1;
        this.lastSentHealth = -1.0F;

        if (scale == null) {
            if (this.data$hasSpongeData()) {
                this.data$getSpongeData().remove(Constants.Sponge.Entity.Player.HEALTH_SCALE);
            }
        } else {
            this.data$getSpongeData().putDouble(Constants.Sponge.Entity.Player.HEALTH_SCALE, scale);
        }
        this.bridge$refreshScaledHealth();
    }

    @Override
    public void bridge$refreshScaledHealth() {
        // We need to use the dirty instances to signify that the player needs to have it updated, instead
        // of modifying the attribute instances themselves, we bypass other potentially detrimental logic
        // that would otherwise break the actual health scaling.
        final Set<ModifiableAttributeInstance> dirtyInstances = this.shadow$getAttributes().getDirtyAttributes();
        this.bridge$injectScaledHealth(dirtyInstances);

        // Send the new information to the client.
        final FoodStats foodData = this.shadow$getFoodData();
        this.connection.send(new SUpdateHealthPacket(this.bridge$getInternalScaledHealth(), foodData.getFoodLevel(), foodData.getSaturationLevel()));
        this.connection.send(new SEntityPropertiesPacket(this.shadow$getId(), dirtyInstances));
        // Reset the dirty instances since they've now been manually updated on the client.
        dirtyInstances.clear();

    }

    @Override
    public void bridge$injectScaledHealth(final Collection<ModifiableAttributeInstance> set) {
        // We need to remove the existing attribute instance for max health, since it's not always going to be the
        // same as SharedMonsterAttributes.MAX_HEALTH
        @Nullable Collection<AttributeModifier> modifiers = null;
        boolean foundMax = false; // Sometimes the max health isn't modified and no longer dirty
        for (final Iterator<ModifiableAttributeInstance> iter = set.iterator(); iter.hasNext(); ) {
            final ModifiableAttributeInstance dirtyInstance = iter.next();
            if ("attribute.name.generic.maxHealth".equals(dirtyInstance.getAttribute().getDescriptionId())) {
                foundMax = true;
                modifiers = dirtyInstance.getModifiers();
                iter.remove();
                break;
            }
        }
        if (!foundMax) {
            // Means we didn't find the max health attribute and need to fetch the modifiers from
            // the cached map because it wasn't marked dirty for some reason
            modifiers = this.shadow$getAttribute(Attributes.MAX_HEALTH).getModifiers();
        }

        final ModifiableAttributeInstance attribute = new ModifiableAttributeInstance(Attributes.MAX_HEALTH, i -> {});
        if (this.bridge$isHealthScaled()) {
            attribute.setBaseValue(this.impl$healthScale);
        }

        if (!modifiers.isEmpty()) {
            modifiers.forEach(attribute::addTransientModifier);
        }
        set.add(attribute);
    }

    @Override
    public Double bridge$getHealthScale() {
        return this.impl$healthScale;
    }

    @Override
    public float bridge$getInternalScaledHealth() {
        if (!this.bridge$isHealthScaled()) {
            return this.shadow$getHealth();
        }
        if (this.impl$cachedModifiedHealth == -1) {
            // Because attribute modifiers from mods can add onto health and multiply health, we
            // need to replicate what the mod may be trying to represent, regardless whether the health scale
            // says to show only x hearts.
            final ModifiableAttributeInstance maxAttribute = this.shadow$getAttribute(Attributes.MAX_HEALTH);
            double modifiedScale = this.impl$healthScale;
            // Apply additive modifiers
            for (final AttributeModifier modifier : maxAttribute.getModifiers(AttributeModifier.Operation.ADDITION)) {
                modifiedScale += modifier.getAmount();
            }

            for (final AttributeModifier modifier : maxAttribute.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) {
                modifiedScale += modifiedScale * modifier.getAmount();
            }

            for (final AttributeModifier modifier : maxAttribute.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
                modifiedScale *= 1.0D + modifier.getAmount();
            }

            this.impl$cachedModifiedHealth = (float) modifiedScale;
        }
        return (this.shadow$getHealth() / this.shadow$getMaxHealth()) * this.impl$cachedModifiedHealth;
    }

    @Override
    public boolean bridge$isHealthScaled() {
        return this.impl$healthScale != null;
    }

}


