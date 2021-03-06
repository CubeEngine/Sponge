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
package org.spongepowered.common.data.provider.item.stack;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.util.registry.Registry;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.util.weighted.ChanceTable;
import org.spongepowered.api.util.weighted.NestedTableEntry;
import org.spongepowered.api.util.weighted.WeightedTable;
import org.spongepowered.common.accessor.item.ToolItemAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.NBTCollectors;

import java.util.List;
import java.util.Set;

@SuppressWarnings({"unchecked", "UnstableApiUsage"})
public final class ItemStackData {

    private ItemStackData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(ItemStack.class)
                    .create(Keys.APPLICABLE_POTION_EFFECTS)
                        .get(h -> {
                            if (h.isEdible()) {
                                final List<Pair<EffectInstance,Float>> itemEffects = h.getItem().getFoodProperties().getEffects();
                                final WeightedTable<PotionEffect> effects = new WeightedTable<>();
                                final ChanceTable<PotionEffect> chance = new ChanceTable<>();
                                for (final Pair<EffectInstance,Float> effect : itemEffects) {
                                    chance.add((PotionEffect) effect.getFirst(), effect.getSecond());
                                }
                                effects.add(new NestedTableEntry<>(1, chance));
                                return effects;
                            }
                            return null;
                        })
                    .create(Keys.BURN_TIME)
                        .get(h -> {
                            final Integer burnTime = AbstractFurnaceTileEntity.getFuel().get(h.getItem());
                            if (burnTime != null && burnTime > 0) {
                                return burnTime;
                            }
                            return null;
                        })
                    .create(Keys.CAN_HARVEST)
                        .get(h -> {
                            final Item item = h.getItem();
                            if (item instanceof ToolItemAccessor && !(item instanceof PickaxeItem)) {
                                final Set<Block> blocks = ((ToolItemAccessor) item).accessor$blocks();
                                return ImmutableSet.copyOf((Set<BlockType>) (Object) blocks);
                            }

                            final Set<BlockType> blockTypes = Registry.BLOCK.stream()
                                    .filter(b -> item.isCorrectToolForDrops(b.defaultBlockState()))
                                    .map(BlockType.class::cast)
                                    .collect(ImmutableSet.toImmutableSet());
                            return blockTypes.isEmpty() ? null : blockTypes;
                        })
                    .create(Keys.CONTAINER_ITEM)
                        .get(h -> (ItemType) h.getItem().getCraftingRemainingItem())
                    .create(Keys.DISPLAY_NAME)
                        .get(h -> {
                            if (h.getItem() == Items.WRITTEN_BOOK) {
                                final CompoundNBT tag = h.getTag();
                                if (tag != null) {
                                    final String title = tag.getString(Constants.Item.Book.ITEM_BOOK_TITLE);
                                    return SpongeAdventure.legacySection(title);
                                }
                            }
                            return SpongeAdventure.asAdventure(h.getDisplayName());
                        })
                        .setAnd((h, v) -> {
                            if (h.getItem() == Items.WRITTEN_BOOK) {
                                final String legacy = SpongeAdventure.legacySection(v);
                                h.addTagElement(Constants.Item.Book.ITEM_BOOK_TITLE, StringNBT.valueOf(legacy));
                                return true;
                            }
                            return false;
                        })
                    .create(Keys.CUSTOM_MODEL_DATA)
                        .get(h -> {
                            final CompoundNBT tag = h.getTag();
                            if (tag == null || !tag.contains(Constants.Item.CUSTOM_MODEL_DATA, Constants.NBT.TAG_INT)) {
                                return null;
                            }
                            return tag.getInt(Constants.Item.CUSTOM_MODEL_DATA);
                        })
                        .set((h, v) -> {
                            final CompoundNBT tag = h.getOrCreateTag();
                            tag.putInt(Constants.Item.CUSTOM_MODEL_DATA, v);
                        })
                        .delete(h -> {
                            final CompoundNBT tag = h.getTag();
                            if (tag != null) {
                                tag.remove(Constants.Item.CUSTOM_MODEL_DATA);
                            }
                        })
                    .create(Keys.CUSTOM_NAME)
                        .get(h -> h.hasCustomHoverName() ? SpongeAdventure.asAdventure(h.getHoverName()) : null)
                        .set((h, v) -> h.setHoverName(SpongeAdventure.asVanilla(v)))
                        .delete(ItemStack::resetHoverName)
                    .create(Keys.IS_UNBREAKABLE)
                        .get(h -> {
                            final CompoundNBT tag = h.getTag();
                            if (tag == null || !tag.contains(Constants.Item.ITEM_UNBREAKABLE, Constants.NBT.TAG_BYTE)) {
                                return false;
                            }
                            return tag.getBoolean(Constants.Item.ITEM_UNBREAKABLE);
                        })
                        .set(ItemStackData::setIsUnbrekable)
                        .delete(h -> ItemStackData.setIsUnbrekable(h, false))
                    .create(Keys.LORE)
                        .get(h -> {
                            final CompoundNBT tag = h.getTag();
                            if (tag == null || tag.contains(Constants.Item.ITEM_DISPLAY)) {
                                return null;
                            }

                            final ListNBT list = tag.getList(Constants.Item.ITEM_LORE, Constants.NBT.TAG_STRING);
                            return list.isEmpty() ? null : SpongeAdventure.json(list.stream().collect(NBTCollectors.toStringList()));
                        })
                        .set((h, v) -> {
                            if (v.isEmpty()) {
                                ItemStackData.deleteLore(h);
                                return;
                            }
                            final ListNBT list = SpongeAdventure.listTagJson(v);
                            h.getOrCreateTagElement(Constants.Item.ITEM_DISPLAY).put(Constants.Item.ITEM_LORE, list);
                        })
                        .delete(ItemStackData::deleteLore)
                    .create(Keys.MAX_DURABILITY)
                        .get(h -> h.getItem().canBeDepleted() ? h.getItem().getMaxDamage() : null)
                        .supports(h -> h.getItem().canBeDepleted())
                    .create(Keys.ITEM_DURABILITY)
                        .get(stack -> stack.getMaxDamage() - stack.getDamageValue())
                        .set((stack, durability) -> stack.setDamageValue(stack.getMaxDamage() - durability))
                        .supports(h -> h.getItem().canBeDepleted())
                    .create(Keys.REPLENISHED_FOOD)
                        .get(h -> {
                            if (h.getItem().isEdible()) {
                                final Food food = h.getItem().getFoodProperties();
                                return food == null ? null : food.getNutrition();
                            }
                            return null;
                        })
                        .supports(h -> h.getItem().isEdible())
                    .create(Keys.REPLENISHED_SATURATION)
                        .get(h -> {
                            if (h.getItem().isEdible()) {
                                final Food food = h.getItem().getFoodProperties();
                                if (food != null) {
                                    // Translate's Minecraft's weird internal value to the actual saturation value
                                    return food.getSaturationModifier() * food.getNutrition() * 2.0;
                                }
                            }
                            return null;
                        })
                    .supports(h -> h.getItem().isEdible());
    }
    // @formatter:on

    private static void setIsUnbrekable(final ItemStack stack, final Boolean value) {
        if (value == null || (!value && !stack.hasTag())) {
            return;
        }
        final CompoundNBT tag = stack.getOrCreateTag();
        if (value) {
            tag.putBoolean(Constants.Item.ITEM_UNBREAKABLE, true);
        } else {
            tag.remove(Constants.Item.ITEM_UNBREAKABLE);
        }
    }

    private static void deleteLore(final ItemStack stack) {
        final CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains(Constants.Item.ITEM_DISPLAY)) {
            tag.getCompound(Constants.Item.ITEM_DISPLAY).remove(Constants.Item.ITEM_LORE);
        }
    }
}
