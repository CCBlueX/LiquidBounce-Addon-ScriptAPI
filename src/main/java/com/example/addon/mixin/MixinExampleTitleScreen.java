package com.example.addon.mixin;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Proves an add-on can ship Mixins - the main thing the Script API could never do.
 *
 * Targets a vanilla class. Mixing into LiquidBounce itself also works, but needs a priority above
 * the client's own config (1337) and no remapping, since its classes are already in the target
 * namespace.
 */
@Mixin(TitleScreen.class)
public class MixinExampleTitleScreen {

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        System.out.println("[ExampleAddon] Title screen initialised");
    }

}
