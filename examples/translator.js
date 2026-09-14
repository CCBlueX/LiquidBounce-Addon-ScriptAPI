// printing bugs out with latin alfabet. because minecraft is wierd and doesnt "allow" it

const script = registerScript({
  name: "Translator",
  version: "2.0.0",
  authors: ["Trikaes", "1zuna", "MukjepScarlet"],
});

script.registerCommand({
  name: "gtranslate",
  aliases: ["gtr"],
  parameters: [
    {
      name: "sourceLanguage",
      type: "string",
      required: true,
      description: "The source language code (en, fr, de, etc...)",
    },
    {
      name: "targetLanguage",
      type: "string",
      required: true,
      description: "The target language code (en, fr, de, etc...)",
    },
    {
      name: "text",
      type: "string",
      required: true,
      vararg: true,
      description: "The text to translate",
    },
  ],
  onExecute: async (sourceLanguage, targetLanguage, text) => {
    try {
      const response = await AsyncUtil.request((builder) => {
        builder.url(`https://translate.googleapis.com/translate_a/single?client=gtx&sl=${sourceLanguage}&tl=${targetLanguage}&dt=t&q=${encodeURIComponent(
          text
        )}`);
      });

      if (response.code() === 200) {
        const jsonResponse = JSON.parse(response.body().string());
        const translatedText = jsonResponse[0][0][0];

        Client.displayChatMessage(
            `§cTranslation §7(§c${sourceLanguage} §7to §c${targetLanguage}§7): §c${text} §7-> §c${translatedText}`
        ); // cool colors
      } else {
        Client.displayChatMessage(
            `§4failed to translate text '${text}'. response code: ${response.code()}`
        ); // hope i dont get this
      }
    } catch (error) {
      Client.displayChatMessage(`§4error translating text '${text}': ${error}`); // hope i dont get this
    }
  },
});
