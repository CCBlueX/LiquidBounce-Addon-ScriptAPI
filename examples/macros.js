/*
This script allows binding client and server commands as well as chat messages to a key.

Usage:
    .macros add <key> <command...>
    .macros remove <key>
    .macros list

Examples:
    .macros add k .help
    .macros add o /gamerules doDaylightCycle false
    .macros add p Some chat message
*/
const script = registerScript({
    name: "Macros",
    version: "1.0.0",
    authors: ["Senk Ju"]
});

const Files = Java.type("java.nio.file.Files");
const Paths = Java.type("java.nio.file.Paths");
const JString = Java.type("java.lang.String");

function printFormattedText(message) {
    Client.displayChatMessage(`§8[§9§lMacros§8] §3 ${message}`);
}

function readMacros() {
    try {
        const file = new JString(Files.readAllBytes(Paths.get("macros.json")));
        return JSON.parse(file);
    } catch (e) {
        return {};
    }
}

function writeMacros() {
    Files.write(Paths.get("macros.json"), JSON.stringify(macros).getBytes());
}

const clientCommandPrefix = "."; // TODO: Get from client
const macros = readMacros();

script.registerModule({
    name: "KeyMacros",
    description: "Allows you to bind commands to keys.",
    category: "Misc"
}, (module) => {
    module.on("key", (event) => {
        if (event.getAction() !== 1) {
            return;
        }
        
        const key = event.getKey().getName().split(".").pop();

        const command = macros[key];

        if (!command) {
            return;
        }

        switch (command[0]) {
            case clientCommandPrefix:
                Client.commandManager.execute(command.substring(1));
                break;
            case "/":
                NetworkUtil.sendCommand(command.substring(1));
                break;
            default:
                NetworkUtil.sendChatMessage(command);
        }
    });
});


script.registerCommand({
    name: "macros",
    aliases: ["macro"],
    hub: true,
    subcommands: [
        {
            name: "add",
            parameters: [
                {
                    name: "key",
                    required: true,
                },
                {
                    name: "command",
                    required: true,
                    vararg: true
                }
            ],
            onExecute(key, command) {
                macros[key] = command;

                writeMacros();
                printFormattedText(`Added macro '${command}' to '${key}'.`);
            }
        },
        {
            name: "remove",
            parameters: [
                {
                    name: "key",
                    required: true
                }
            ],
            onExecute(key) {
                delete macros[key];

                writeMacros();
                printFormattedText(`Removed macro from '${key}'.`);
            }
        },
        {
            name: "list",
            onExecute() {
                for (const key in macros) {
                    printFormattedText(`${key} -> '${macros[key]}'`);
                }
            }
        }
    ]
});