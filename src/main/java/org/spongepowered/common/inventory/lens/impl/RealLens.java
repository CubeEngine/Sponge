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
package org.spongepowered.common.inventory.lens.impl;

import net.minecraft.inventory.container.Container;
import net.minecraft.tileentity.LockableTileEntity;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.common.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.inventory.fabric.Fabric;

/**
 * Lenses for real Inventories like {@link LockableTileEntity} and {@link Container}.
 *
 * <p>When possible this lens will return the real {@link InventoryAdapter} as opposed to some kind of Wrapper Adapter</p>
 */
@SuppressWarnings("rawtypes")
public abstract class RealLens extends AbstractLens {

    public RealLens(int base, int size, Class<? extends Inventory> adapterType) {
        super(base, size, adapterType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Inventory getAdapter(Fabric fabric, Inventory parent) {
        return (Inventory) fabric.fabric$get(this.base).bridge$getAdapter();
    }
}
