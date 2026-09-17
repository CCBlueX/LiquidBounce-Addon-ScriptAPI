const script = registerScript({
  name: "Clash",
  version: "1.0.0",
  authors: ["CCBlueX"]
});

// Registered before the clash, so a half enabled script would leave it behind.
script.registerModule({
  name: "ClashProbe",
  category: "Misc",
  description: "Must not survive the failed enable."
}, (mod) => {});

// Taken by the client's own module.
script.registerModule({
  name: "HighJump",
  category: "Movement",
  description: "Clashes with the client."
}, (mod) => {});
