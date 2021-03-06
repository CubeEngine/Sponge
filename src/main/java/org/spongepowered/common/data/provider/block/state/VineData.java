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
package org.spongepowered.common.data.provider.block.state;

import net.minecraft.block.BlockState;
import net.minecraft.block.VineBlock;
import org.spongepowered.api.data.Keys;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.DirectionalUtil;

public final class VineData {

    private VineData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asImmutable(BlockState.class)
                    .create(Keys.CONNECTED_DIRECTIONS)
                        .get(h -> DirectionalUtil.getHorizontalUpFrom(h, VineBlock.EAST, VineBlock.WEST, VineBlock.NORTH, VineBlock.SOUTH,
                                VineBlock.UP))
                        .set((h, v) -> DirectionalUtil.setHorizontalUpFor(h, v, VineBlock.EAST, VineBlock.WEST, VineBlock.NORTH, VineBlock.SOUTH,
                                VineBlock.UP))
                        .supports(h -> h.getBlock() instanceof VineBlock)
                    .create(Keys.IS_CONNECTED_EAST)
                        .get(h -> h.getValue(VineBlock.EAST))
                        .set((h, v) -> h.setValue(VineBlock.EAST, v))
                        .supports(h -> h.getBlock() instanceof VineBlock)
                    .create(Keys.IS_CONNECTED_NORTH)
                        .get(h -> h.getValue(VineBlock.NORTH))
                        .set((h, v) -> h.setValue(VineBlock.NORTH, v))
                        .supports(h -> h.getBlock() instanceof VineBlock)
                    .create(Keys.IS_CONNECTED_SOUTH)
                        .get(h -> h.getValue(VineBlock.SOUTH))
                        .set((h, v) -> h.setValue(VineBlock.SOUTH, v))
                        .supports(h -> h.getBlock() instanceof VineBlock)
                    .create(Keys.IS_CONNECTED_UP)
                        .get(h -> h.getValue(VineBlock.UP))
                        .set((h, v) -> h.setValue(VineBlock.UP, v))
                        .supports(h -> h.getBlock() instanceof VineBlock)
                    .create(Keys.IS_CONNECTED_WEST)
                        .get(h -> h.getValue(VineBlock.WEST))
                        .set((h, v) -> h.setValue(VineBlock.WEST, v))
                        .supports(h -> h.getBlock() instanceof VineBlock);
    }
    // @formatter:on
}
