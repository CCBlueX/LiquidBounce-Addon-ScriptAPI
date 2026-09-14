const script = registerScript({
  name: "AutoJump",
  version: "1.0.0",
  authors: ["1zuna"]
});

script.registerModule({
  name: "AutoJump",
  category: "Movement", // Movement, Combat, Render, ...
  description: "Jumps automatically for you."
}, (mod) => { 
  mod.on("movementInput", (event) => {
    event.setJump(true);
  });
});
