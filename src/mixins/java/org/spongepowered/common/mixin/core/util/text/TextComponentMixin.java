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
package org.spongepowered.common.mixin.core.util.text;

import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.KeybindTextComponent;
import net.minecraft.util.text.NBTTextComponent;
import net.minecraft.util.text.ScoreTextComponent;
import net.minecraft.util.text.SelectorTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.util.text.StyleBridge;
import org.spongepowered.common.bridge.util.text.TextComponentBridge;

import java.util.ArrayList;
import java.util.List;

@Mixin(TextComponent.class)
public class TextComponentMixin implements TextComponentBridge {
    private Component bridge$adventureComponent;

    @Override
    @SuppressWarnings("ConstantConditions")
    public Component bridge$asAdventureComponent() {
        if (this.bridge$adventureComponent == null) {
            ComponentBuilder<?, ?> builder = null;
            if ((Object) this instanceof StringTextComponent) {
                builder = Component.text().content(((StringTextComponent) (Object) this).getText());
            } else if ((Object) this instanceof TranslationTextComponent) {
                final TranslationTextComponent $this = (TranslationTextComponent) (Object) this;
                final List<Component> with = new ArrayList<>($this.getArgs().length);
                for (final Object arg : $this.getArgs()) {
                    if (arg instanceof ITextComponent) {
                        with.add(SpongeAdventure.asAdventure((ITextComponent) arg));
                    } else {
                        with.add(Component.text(arg.toString()));
                    }
                }
                builder = Component.translatable().key($this.getKey()).args(with.toArray(new Component[0]));
            } else if ((Object) this instanceof KeybindTextComponent) {
                builder = Component.keybind().keybind(((KeybindTextComponent) (Object) this).getName());
            } else if ((Object) this instanceof ScoreTextComponent) {
                final ScoreTextComponent $this = (ScoreTextComponent) (Object) this;
                builder = Component.score().name($this.getName()).objective($this.getObjective());
            } else if ((Object) this instanceof SelectorTextComponent) {
                builder = Component.selector().pattern(((SelectorTextComponent) (Object) this).getPattern());
            } else if ((Object) this instanceof NBTTextComponent) {
                if ((Object) this instanceof NBTTextComponent.Block) {
                    final NBTTextComponent.Block $this = (NBTTextComponent.Block) (Object) this;
                    builder = Component.blockNBT().pos(BlockNBTComponent.Pos.fromString($this.getPos())).nbtPath($this.getNbtPath()).interpret($this.isInterpreting());
                } else if ((Object) this instanceof NBTTextComponent.Entity) {
                    final NBTTextComponent.Entity $this = (NBTTextComponent.Entity) (Object) this;
                    builder = Component.entityNBT().nbtPath($this.getNbtPath()).interpret($this.isInterpreting()).selector($this.getSelector());
                } else if ((Object) this instanceof NBTTextComponent.Storage) {
                    final NBTTextComponent.Storage $this = (NBTTextComponent.Storage) (Object) this;
                    builder = Component.storageNBT().nbtPath($this.getNbtPath()).interpret($this.isInterpreting()).storage(SpongeAdventure.asAdventure($this.getId()));
                }
            } else {
              throw new UnsupportedOperationException();
            }
            for (final ITextComponent child : ((ITextComponent) this).getSiblings()) {
                builder.append(SpongeAdventure.asAdventure(child));
            }
            builder.style(((StyleBridge) ((ITextComponent) this).getStyle()).bridge$asAdventure());
            this.bridge$adventureComponent = builder.build();
        }
        return this.bridge$adventureComponent;
    }

    @Override
    public @Nullable Component bridge$adventureComponentIfPresent() {
        return this.bridge$adventureComponent;
    }
}
