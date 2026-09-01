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
package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.features.addon.LiquidBounceAddon
import net.ccbluex.liquidbounce.script.command.CommandScript

/**
 * Entry point of the add-on.
 */
class ScriptApiAddon : LiquidBounceAddon() {

    override fun onInitialize() {
        ScriptManager.initializeEngine()
        registerCommand(CommandScript)
        ScriptManager.loadAll()
    }

    /**
     * [ScriptManager.closeAll] and not `unloadAll`: this runs before the client writes its configs,
     * so unregistering the script modules here would drop their settings from `modules.json`.
     */
    override fun onShutdown() {
        ScriptManager.closeAll()
    }

}
