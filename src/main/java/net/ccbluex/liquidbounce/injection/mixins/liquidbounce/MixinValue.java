/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.injection.mixins.liquidbounce;

import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.script.bindings.features.ScriptSettingKt;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Value.class, remap = false)
public abstract class MixinValue {

    public void setValue(org.graalvm.polyglot.Value value) {
        ScriptSettingKt.setByPolyglot((Value<?>) (Object) this, value);
    }

}
