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
package org.spongepowered.common.mixin.inventory.impl;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.minecart.ContainerMinecartEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.MerchantInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.tileentity.LockableTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.bridge.inventory.InventoryBridge;
import org.spongepowered.common.bridge.inventory.LensGeneratorBridge;
import org.spongepowered.common.entity.player.SpongeUserInventory;
import org.spongepowered.common.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.inventory.custom.CustomInventory;
import org.spongepowered.common.inventory.fabric.Fabric;
import org.spongepowered.common.inventory.lens.Lens;
import org.spongepowered.common.inventory.lens.impl.LensRegistrar;
import org.spongepowered.common.inventory.lens.impl.slot.SlotLensProvider;

import javax.annotation.Nullable;

/**
 * Mixin into all known vanilla {@link IInventory} and {@link Container}
 */
@Mixin(value = {
        Slot.class,
        PlayerInventory.class,
        DoubleSidedInventory.class,
        LockableTileEntity.class,
        CustomInventory.class,
        Inventory.class,
        SpongeUserInventory.class,
        CraftingInventory.class,
        CraftResultInventory.class,
        ContainerMinecartEntity.class,
        ArmorStandEntity.class,
        MobEntity.class,
        MerchantInventory.class
}, targets = "net.minecraft.tileentity.LecternTileEntity$1", priority = 999)
public abstract class TraitMixin_InventoryBridge_Inventory implements InventoryAdapter, InventoryBridge {

    @Nullable private SlotLensProvider impl$provider;
    @Nullable private Lens impl$lens;

    @Override
    public Fabric inventoryAdapter$getFabric() {
        return (Fabric) this;
    }

    @Override
    public SlotLensProvider inventoryAdapter$getSlotLensProvider() {
        if (this.impl$provider == null) {
            if (this instanceof LensGeneratorBridge) {
                this.impl$provider = ((LensGeneratorBridge) this).lensGeneratorBridge$generateSlotLensProvider();
            } else {
                this.impl$provider = new LensRegistrar.BasicSlotLensProvider(this.inventoryAdapter$getFabric().fabric$getSize());
            }
        }
        return this.impl$provider;
    }

    @Override
    public Lens inventoryAdapter$getRootLens() {
        if (this.impl$lens == null) {
            if (this instanceof LensGeneratorBridge) {
                this.impl$lens = ((LensGeneratorBridge) this).lensGeneratorBridge$generateLens(this.inventoryAdapter$getSlotLensProvider());
            } else {
                this.impl$lens = LensRegistrar.getLens(this, this.inventoryAdapter$getSlotLensProvider(), this.inventoryAdapter$getFabric().fabric$getSize());
            }
        }
        return this.impl$lens;
    }

}
