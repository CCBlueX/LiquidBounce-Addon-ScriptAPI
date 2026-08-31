package com.example.addon

import com.example.addon.commands.CommandExample
import com.example.addon.modules.ModuleExample
import net.ccbluex.liquidbounce.features.addon.LiquidBounceAddon

/**
 * Entry point of the add-on, named under the `liquidbounce` entrypoint in `fabric.mod.json`.
 *
 * Name, version, authors and description are read from that same file - never declare them twice.
 */
class ExampleAddon : LiquidBounceAddon() {

    /**
     * Runs before any add-on registers modules, so a module may reference a category declared here
     * by another add-on.
     */
    override fun onRegisterCategories() {
        ExampleCategories.register(this)
    }

    override fun onInitialize() {
        logger.info("Example add-on starting up")

        registerModules(ModuleExample)
        registerCommand(CommandExample)
    }

    /**
     * Settings have been restored from disk by this point.
     */
    override fun onConfigsLoaded() {
        logger.info("Example module is ${if (ModuleExample.enabled) "enabled" else "disabled"}")
    }

    override fun onShutdown() {
        logger.info("Example add-on shutting down")
    }

}
