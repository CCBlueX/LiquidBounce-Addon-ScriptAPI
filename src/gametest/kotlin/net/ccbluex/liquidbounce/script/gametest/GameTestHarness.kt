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
package net.ccbluex.liquidbounce.script.gametest

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.script.PolyglotScript
import net.ccbluex.liquidbounce.script.ScriptManager
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.multiplayer.chat.GuiMessage
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.file.Path

enum class Status { PASS, FAIL, SKIP }

class CaseResult(
    val name: String,
    val status: Status,
    val detail: String,
    val durationMs: Long,
    val chat: List<String>,
    val screenshots: List<String>,
)

private class Report(
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val versions: Map<String, String>,
    val cases: List<CaseResult>,
)

/**
 * Ends a case as [Status.SKIP]: something outside the script, like the network, got in the way.
 */
class Skipped(reason: String) : RuntimeException(reason)

fun skip(reason: String): Nothing = throw Skipped(reason)

fun <T> ClientGameTestContext.client(block: (Minecraft) -> T): T =
    computeOnClient<T, RuntimeException> { block(it) }

// ChatComponent offers no read access to its history, not even through storeState().
private val allMessages = ChatComponent::class.java.getDeclaredField("allMessages").apply { isAccessible = true }

/**
 * Chat history in the order it arrived, formatting stripped.
 */
fun ClientGameTestContext.chatLines(): List<String> = client { mc ->
    @Suppress("UNCHECKED_CAST")
    val messages = allMessages.get(mc.gui.hud.chat) as List<GuiMessage>
    messages.asReversed().map { ChatFormatting.stripFormatting(it.content().string).orEmpty() }
}

/**
 * One test case. Scripts loaded through it are unloaded again when the case ends.
 */
class Case(val name: String, val context: ClientGameTestContext, private val examples: File) {

    private val scripts = mutableListOf<PolyglotScript>()
    internal val screenshots = mutableListOf<Path>()

    fun <T> client(block: (Minecraft) -> T): T = context.client(block)

    fun example(path: String) = File(examples, path)

    /**
     * Same as `.script load`.
     */
    fun load(file: File): PolyglotScript = client {
        ScriptManager.loadScript(file).also { script ->
            scripts += script
            script.enable()
        }
    }

    fun load(example: String) = load(example(example))

    fun module(name: String): ClientModule = client { ModuleManager[name] } ?: error("module $name is not registered")

    fun command(command: String) = client { CommandManager.execute(command) }

    fun awaitChat(ticks: Int, predicate: (String) -> Boolean): String? {
        repeat(ticks) {
            context.chatLines().firstOrNull(predicate)?.let { return it }
            context.waitTick()
        }
        return context.chatLines().firstOrNull(predicate)
    }

    fun screenshot(label: String): Path =
        context.takeScreenshot("${name.substringBefore('.')}-$label").toAbsolutePath().also(screenshots::add)

    internal fun close() = client { scripts.asReversed().forEach(ScriptManager::unloadScript) }

}

/**
 * Runs its cases one after another in a fresh world, then writes `scriptapi-gametest.json` into the
 * game directory and fails the run if any case failed.
 */
class GameTestHarness(private val context: ClientGameTestContext) {

    private val logger = LogManager.getLogger("ScriptAPI/GameTest")
    private val examples = File(System.getProperty(EXAMPLES_PROPERTY) ?: error("$EXAMPLES_PROPERTY is not set"))
    private val selected = System.getProperty(ONLY_PROPERTY)
        ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.toSet()

    private val cases = linkedMapOf<String, Case.() -> String>()
    private val results = mutableListOf<CaseResult>()

    fun case(name: String, block: Case.() -> String) {
        cases[name] = block
    }

    fun runCases() {
        val unknown = selected.orEmpty() - cases.keys
        check(unknown.isEmpty()) { "$ONLY_PROPERTY names unknown cases $unknown, known are ${cases.keys}" }

        context.worldBuilder().create().use { world ->
            world.clientLevel.waitForChunksRender()
            context.waitFor { it.player?.onGround() == true }

            for ((name, block) in cases) {
                if (selected == null || name in selected) {
                    results += runCase(name, block)
                }
            }
        }

        report()
    }

    private fun runCase(name: String, block: Case.() -> String): CaseResult {
        logger.info("RUN {}", name)
        context.client { it.gui.hud.chat.clearMessages(false) }
        val case = Case(name, context, examples)
        val started = System.nanoTime()

        var (status, detail) = try {
            Status.PASS to case.block()
        } catch (e: Skipped) {
            Status.SKIP to e.message.orEmpty()
        } catch (e: Exception) {
            logger.error("{} failed", name, e)
            Status.FAIL to (e.message ?: e.toString())
        } catch (e: AssertionError) {
            logger.error("{} failed", name, e)
            Status.FAIL to (e.message ?: e.toString())
        }

        runCatching(case::close).onFailure { e ->
            logger.error("{} could not unload its scripts", name, e)
            if (status != Status.FAIL) {
                status = Status.FAIL
                detail = "unloading failed: $e"
            }
        }

        val chat = context.chatLines()
        chat.firstOrNull { SCRIPT_ERROR in it }?.let { error ->
            detail = if (status == Status.FAIL) "$detail; script reported: $error" else "script reported: $error"
            status = Status.FAIL
        }

        val result = CaseResult(
            name,
            status,
            detail,
            (System.nanoTime() - started) / 1_000_000,
            chat,
            case.screenshots.map(Path::toString),
        )
        logger.info("{} {} ({} ms): {}", status, name, result.durationMs, detail)
        return result
    }

    private fun report() {
        val report = context.client { File(it.gameDirectory, REPORT_FILE) }.absoluteFile
        val versions = listOf("minecraft", "liquidbounce", "liquidbounce-scriptapi", "fabric-api").associateWith { id ->
            FabricLoader.getInstance().getModContainer(id).map { it.metadata.version.friendlyString }.orElse("absent")
        }
        val counts = Status.entries.associateWith { status -> results.count { it.status == status } }

        report.writeText(
            GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(
                Report(counts.getValue(Status.PASS), counts.getValue(Status.FAIL), counts.getValue(Status.SKIP), versions, results)
            )
        )
        logger.info(
            "{} passed, {} failed, {} skipped. Report: {}",
            counts[Status.PASS], counts[Status.FAIL], counts[Status.SKIP], report
        )

        val failed = results.filter { it.status == Status.FAIL }
        if (failed.isNotEmpty()) {
            throw AssertionError(
                "${failed.size} of ${results.size} cases failed: ${failed.joinToString { it.name }}. Report: $report"
            )
        }
    }

    companion object {
        private const val EXAMPLES_PROPERTY = "scriptapi.gametest.examples"
        private const val ONLY_PROPERTY = "scriptapi.gametest.only"
        private const val REPORT_FILE = "scriptapi-gametest.json"

        // How ScriptModule reports an event handler that threw.
        private const val SCRIPT_ERROR = " threw ["
    }

}
