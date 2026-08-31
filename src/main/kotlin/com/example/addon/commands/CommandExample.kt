package com.example.addon.commands

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.utils.client.chat

/**
 * Adds `.example` to the client's command tree.
 */
object CommandExample : CommandRegistrar {

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("example") {
            exec {
                chat("The example add-on is loaded.")
                1
            }
        }
    }

}
