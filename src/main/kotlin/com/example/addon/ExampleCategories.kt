package com.example.addon

import net.ccbluex.liquidbounce.features.addon.LiquidBounceAddon
import net.ccbluex.liquidbounce.features.module.ModuleCategory

/**
 * Custom module categories.
 *
 * Registered from [LiquidBounceAddon.onRegisterCategories] rather than at class-init, so the add-on
 * manager can withdraw them again if the add-on is disabled.
 */
object ExampleCategories {

    lateinit var EXAMPLE: ModuleCategory
        private set

    internal fun register(addon: LiquidBounceAddon) {
        EXAMPLE = addon.registerCategory("Example")
    }

}
