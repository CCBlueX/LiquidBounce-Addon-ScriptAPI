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

import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.NativeImage
import net.ccbluex.liquidbounce.script.ScriptManager
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import org.lwjgl.glfw.GLFW
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Runs every script in `examples` against a live client.
 */
class ExampleScriptsGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        val harness = GameTestHarness(context)
        harness.case("AutoJump.mjs") { autoJump("AutoJump.mjs") }
        harness.case("AutoJump-2.js") { autoJump("AutoJump-2.js") }
        harness.case("macros.js") { macros() }
        harness.case("translator.js") { translator() }
        harness.case("nes_emulator") { nesEmulator() }
        harness.runCases()
    }

}

private fun Case.airborneTicks(ticks: Int) = (1..ticks).count {
    context.waitTick()
    client { !it.player!!.onGround() }
}

private fun Case.autoJump(example: String): String {
    load(example)
    check(airborneTicks(20) == 0) { "player was airborne before the module was enabled" }

    val module = module("AutoJump")
    client { module.enabled = true }
    val enabled = airborneTicks(60)
    client { module.enabled = false }
    context.waitTicks(20)
    val jumpHeld = client { it.options.keyJump.isDown }
    val disabled = airborneTicks(40)

    check(enabled >= 20) { "airborne for only $enabled of 60 ticks while enabled" }
    check(!jumpHeld) { "jump key still held after disabling" }
    check(disabled == 0) { "airborne for $disabled of 40 ticks after disabling" }
    return "airborne $enabled/60 ticks while enabled, $disabled/40 after disabling"
}

private fun Case.macros(): String {
    // The script resolves macros.json against the working directory, which is the game directory.
    val store = client { File(it.gameDirectory, "macros.json") }
    store.delete()

    try {
        load("macros.js")
        val module = module("KeyMacros")
        client { module.enabled = true }
        val player = client { it.user.name }

        command("macros add j /me waves from a macro")
        command("macros add k hello from a macro")
        command("macros add u .macros list")

        context.input.pressKey(GLFW.GLFW_KEY_J)
        awaitChat(40) { it.endsWith("* $player waves from a macro") } ?: error("J did not run the server command")
        context.input.pressKey(GLFW.GLFW_KEY_K)
        awaitChat(40) { it.endsWith("<$player> hello from a macro") } ?: error("K did not send the chat message")
        context.input.pressKey(GLFW.GLFW_KEY_U)
        awaitChat(40) { it.endsWith("u -> '.macros list'") } ?: error("U did not run the client command")

        command("macros remove k")
        val saved = JsonParser.parseString(store.readText()).asJsonObject.keySet()
        check(saved == setOf("j", "u")) { "macros.json holds $saved after removing k" }

        return "server command, chat message and client command macros fired; macros.json holds $saved"
    } finally {
        store.delete()
    }
}

private val NETWORK_ERROR = Regex("response code|UnknownHost|ConnectException|SocketTimeout|SSL|timed out", RegexOption.IGNORE_CASE)

private fun Case.translator(): String {
    load("translator.js")
    command("gtranslate en de Hello world")

    val reply = awaitChat(400) { "Hello world ->" in it || "'Hello world'" in it }
        ?: error("no reply within 20 seconds")
    if ("Hello world ->" in reply) {
        return reply
    }
    if (NETWORK_ERROR in reply) {
        skip("Google Translate unreachable: $reply")
    }
    error("translation failed: $reply")
}

private const val NESTEST_URL = "https://raw.githubusercontent.com/christopherpow/nes-test-roms/" +
    "95d8f621ae55cee0d09b91519a8989ae0e64753b/other/nestest.nes"
private const val NESTEST_SHA256 = "f67d55fd6b3cf0bad1cc85f1df0d739c65b53e79cecb7fea8f77ec0eadab0004"

private fun Case.nesEmulator(): String {
    // main.js reads its ROMs relative to the game directory, so it has to sit where users install it.
    val installed = File(ScriptManager.root, "nes_emulator")
    try {
        example("nes_emulator").copyRecursively(installed, overwrite = true)
        val rom = File(installed, "roms/nestest.nes")
        download(rom)

        load(File(installed, "main.js"))
        val module = module("NESEmulator")
        client { module.settings.getValue("rom").setByString(rom.name) }

        val world = screenshot("world")
        client { module.enabled = true }
        context.waitTicks(100)
        val menu = screenshot("menu")
        context.input.holdKeyFor(GLFW.GLFW_KEY_V, 6)
        context.waitTicks(120)
        val results = screenshot("results")
        client { module.enabled = false }

        val covered = darkFraction(menu) - darkFraction(world)
        check(covered >= 0.4) { "no emulator frame on screen, dark area grew by only ${percent(covered)}" }
        val changed = changedFraction(menu, results)
        check(changed >= 0.002) { "Start did not reach the emulator, ${percent(changed)} of the frame changed" }

        return "frame covers ${percent(covered)} more of the screen, Start changed ${percent(changed)} of it"
    } finally {
        installed.deleteRecursively()
    }
}

private fun download(target: File) {
    val response = try {
        HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI(NESTEST_URL)).timeout(Duration.ofSeconds(30)).build(),
            HttpResponse.BodyHandlers.ofByteArray(),
        )
    } catch (e: IOException) {
        skip("could not download nestest.nes: $e")
    }
    if (response.statusCode() != 200) {
        skip("could not download nestest.nes: HTTP ${response.statusCode()}")
    }

    val bytes = response.body()
    val digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
    check(digest == NESTEST_SHA256) { "nestest.nes has SHA-256 $digest, expected $NESTEST_SHA256" }
    target.parentFile.mkdirs()
    target.writeBytes(bytes)
}

private fun percent(fraction: Double) = String.format(Locale.ROOT, "%.1f%%", fraction * 100)

private inline fun <T> Path.readImage(block: (NativeImage) -> T): T =
    Files.newInputStream(this).use(NativeImage::read).use(block)

/**
 * Share of pixels matching [predicate] in the middle of the screen, where the emulator draws.
 */
private inline fun NativeImage.fraction(predicate: (Int, Int) -> Boolean): Double {
    val xs = width / 4 until width * 3 / 4
    val ys = height / 10 until height * 3 / 4
    var hits = 0
    for (x in xs) {
        for (y in ys) {
            if (predicate(x, y)) {
                hits++
            }
        }
    }
    return hits.toDouble() / (xs.count() * ys.count())
}

private fun Int.channel(shift: Int) = this shr shift and 0xFF

private fun darkFraction(screenshot: Path) = screenshot.readImage { image ->
    image.fraction { x, y ->
        val pixel = image.getPixel(x, y)
        maxOf(pixel.channel(16), pixel.channel(8), pixel.channel(0)) < 32
    }
}

private fun changedFraction(before: Path, after: Path) = before.readImage { a ->
    after.readImage { b ->
        a.fraction { x, y ->
            val p = a.getPixel(x, y)
            val q = b.getPixel(x, y)
            listOf(16, 8, 0).any { shift -> abs(p.channel(shift) - q.channel(shift)) > 24 }
        }
    }
}
