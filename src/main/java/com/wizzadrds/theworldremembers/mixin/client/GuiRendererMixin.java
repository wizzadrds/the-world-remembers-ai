package com.wizzadrds.theworldremembers.mixin.client;

import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void theWorldRemembers$skipInvalidScissor(ScreenRect scissorArea, RenderPass pass, CallbackInfo ci) {
        if (scissorArea.width() <= 0 || scissorArea.height() <= 0) {
            ci.cancel();
        }
    }
}
