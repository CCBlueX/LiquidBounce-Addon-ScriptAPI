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

import net.ccbluex.liquidbounce.features.module.ClientModule;
import net.ccbluex.liquidbounce.features.module.ModuleCategories;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collection;

/**
 * Called by scripts only.
 */
@Mixin(value = ModuleManager.class, remap = false)
public abstract class MixinModuleManager {

    public String[] getCategories() {
        return ModuleCategories.getEntries().stream().map(ModuleCategory::getTag).toArray(String[]::new);
    }

    public Collection<ClientModule> getModules() {
        return (ModuleManager) (Object) this;
    }

    public ClientModule getModuleByName(String module) {
        return ((ModuleManager) (Object) this).get(module);
    }

}
