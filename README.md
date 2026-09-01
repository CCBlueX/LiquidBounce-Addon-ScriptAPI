# LiquidBounce ScriptAPI

The JavaScript Script API for [LiquidBounce](https://github.com/CCBlueX/LiquidBounce), packaged as
an add-on.

It is built on [GraalJS](https://github.com/oracle/graaljs), an ECMAScript 2023 compliant JavaScript
implementation on [GraalVM](https://www.graalvm.org). GraalVM's polyglot support lets scripts reach
straight into the client's Java and Kotlin classes, so anyone familiar with Minecraft modding will
recognise most of it.

## Installing

Download `liquidbounce-scriptapi-*.jar` from the [releases](../../releases) and drop it into your
`mods/` folder next to LiquidBounce, then restart the game. `.addon list` should show it.

Scripts go in `.minecraft/LiquidBounce/scripts/`, either as a single `.js` file or as a directory
containing a `main.js`.

## Commands

| Command | Description |
|---|---|
| `.script list` | Lists every installed script |
| `.script browse` | Opens the scripts folder |
| `.script load <name>` | Loads a single script |
| `.script unload <name>` | Unloads a single script |
| `.script reload` | Reloads every script, picking up newly added ones |
| `.script edit <name>` | Opens a script in your default editor |
| `.script debug <name> [protocol] [suspendOnStart] [inspectInternals] [port]` | Runs a script with a debugger attached |

`debug` defaults to Chrome DevTools (`INSPECT`, port 4242); `DAP` uses port 4711 instead.

## Documentation

Full documentation lives at [liquidbounce.net](https://liquidbounce.net/docs/script-api/installation).

**Getting started**

- [Installation](https://liquidbounce.net/docs/script-api/installation)
- [Introduction](https://liquidbounce.net/docs/script-api/getting-started)
- [Using Java classes](https://liquidbounce.net/docs/script-api/using-java-classes)
- [Global instances](https://liquidbounce.net/docs/script-api/global-instances)
- [Debugging](https://liquidbounce.net/docs/script-api/debugging)

**Writing features**

- [Creating modules](https://liquidbounce.net/docs/script-api/creating-modules/overview)
- [Creating commands](https://liquidbounce.net/docs/script-api/creating-commands)

**Global classes**

| Class | Purpose |
|---|---|
| [Client](https://liquidbounce.net/docs/script-api/global-classes/client) | The client itself: managers, chat output |
| [Setting](https://liquidbounce.net/docs/script-api/global-classes/setting) | Module and command settings |
| [AsyncUtil](https://liquidbounce.net/docs/script-api/global-classes/asyncutil) | Tick scheduling, promises, HTTP requests |
| [BlockUtil](https://liquidbounce.net/docs/script-api/global-classes/blockutil) | Block positions and block state |
| [ItemUtil](https://liquidbounce.net/docs/script-api/global-classes/itemutil) | Item stacks |
| [InteractionUtil](https://liquidbounce.net/docs/script-api/global-classes/interactionutil) | Attacking and placing |
| [MovementUtil](https://liquidbounce.net/docs/script-api/global-classes/movementutil) | Player movement |
| [NetworkUtil](https://liquidbounce.net/docs/script-api/global-classes/networkutil) | Packets |
| [RotationUtil](https://liquidbounce.net/docs/script-api/global-classes/rotationutil) | Aiming |
| [ReflectionUtil](https://liquidbounce.net/docs/script-api/global-classes/reflectionutil) | Reaching non-public members |
| [ParameterValidator](https://liquidbounce.net/docs/script-api/global-classes/parametervalidator) | Command argument validation |
| [Primitives](https://liquidbounce.net/docs/script-api/global-classes/primitives) | Explicit JVM numeric coercion |

## Building

```
./gradlew build
```

Requires JDK 25. The add-on compiles against a published LiquidBounce build, set in
`gradle/libs.versions.toml`. To build against a local client, run `./gradlew publishToMavenLocal`
in a LiquidBounce checkout first.

## TypeScript definitions

`ts-defgen.js` is itself a LiquidBounce script that walks the client's classes and emits TypeScript
definitions, published to npm as `@ccbluex/liquidbounce-script-api`. The
`generate-definitions` workflow runs it.

## License

This project is subject to the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html). This
does only apply for source code located directly in this clean repository. During the development and compilation
process, additional source code may be used to which we have obtained no rights. Such code is not covered by the GPL
license.

For those who are unfamiliar with the license, here is a summary of its main points. This is by no means legal advice
nor legally binding.

*Actions that you are allowed to do:*

- Use
- Share
- Modify

*If you do decide to use ANY code from the source:*

- **You must disclose the source code of your modified work and the source code you took from this project. This means
  you are not allowed to use code from this project (even partially) in a closed-source (or even obfuscated)
  application.**
- **Your modified application must also be licensed under the GPL**
