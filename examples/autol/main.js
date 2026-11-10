/*
AutoL writes a message to every player you kill, AutoGG says "gg" when a game ends.

AutoL listens to the playerKill event, which reads kills from death messages in chat and
needs a LiquidBounce build that has it. {name} in a message is the name of the killed player.

AutoGG matches chat lines and titles against regular expressions. The defaults cover the
usual end-of-game summaries ("1st Killer - ...", "Winner: ...") and titles ("VICTORY!").
*/
const script = registerScript({
    name: "AutoL",
    version: "1.0.0",
    authors: ["Tsikyng Kirisame", "CzechHek", "1zuna"]
});

function plain(text) {
    return String(text).replace(/§./g, "");
}

function pick(list) {
    const entries = Java.from(list).filter((entry) => entry.trim().length > 0);
    return entries[Math.floor(Math.random() * entries.length)];
}

function matchesAny(patterns, text) {
    return Java.from(patterns).some((pattern) => {
        try {
            return new RegExp(pattern, "i").test(text);
        } catch (e) {
            return false;
        }
    });
}

script.registerModule({
    name: "AutoL",
    category: "Misc",
    description: "Writes a message to the player you killed.",
    settings: {
        messages: Setting.textArray({
            name: "Messages",
            default: ["L {name}"]
        }),
        cooldown: Setting.int({
            name: "Cooldown",
            default: 1000,
            range: [0, 5000],
            suffix: "ms"
        })
    }
}, (mod) => {
    let lastSent = 0;

    mod.on("playerKill", (event) => {
        const now = Date.now();
        const message = pick(mod.settings.messages.value);
        if (!message || now - lastSent < mod.settings.cooldown.value) {
            return;
        }

        lastSent = now;
        NetworkUtil.sendChatMessage(message.replaceAll("{name}", event.getVictim()));
    });
});

script.registerModule({
    name: "AutoGG",
    category: "Misc",
    description: "Says gg when a game ends.",
    settings: {
        message: Setting.text({
            name: "Message",
            default: "gg"
        }),
        delay: Setting.int({
            name: "Delay",
            default: 20,
            range: [0, 100],
            suffix: "ticks"
        }),
        chatTriggers: Setting.textArray({
            name: "ChatTriggers",
            default: [
                "^ +1st Killer ?- ",
                "^ *1st (Place )?[-:] ",
                "^ +Winners?(: | - )",
                "^ +Winning Team ?[-:] "
            ]
        }),
        titleTriggers: Setting.textArray({
            name: "TitleTriggers",
            default: [
                "^(VICTORY|GAME OVER|GAME END|YOU WIN|YOU WON)!?$"
            ]
        })
    }
}, (mod) => {
    // One gg per game, the next world is the next game
    let said = false;

    async function gameEnded() {
        if (said) {
            return;
        }
        said = true;

        await AsyncUtil.ticks(mod.settings.delay.value);
        if (mod.enabled && mc.player) {
            NetworkUtil.sendChatMessage(mod.settings.message.value);
        }
    }

    mod.on("enable", () => {
        said = false;
    });

    mod.on("worldChange", () => {
        said = false;
    });

    mod.on("chatReceive", (event) => {
        if (matchesAny(mod.settings.chatTriggers.value, plain(event.getMessage()))) {
            gameEnded();
        }
    });

    mod.on("title", (event) => {
        const text = event.getText();
        if (text && matchesAny(mod.settings.titleTriggers.value, plain(text.getString()).trim())) {
            gameEnded();
        }
    });
});
