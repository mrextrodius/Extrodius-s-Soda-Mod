package com.extrodius.sodamod.screen;

import com.extrodius.sodamod.menu.CarbonatorMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the Carbonator. Draws Zach's custom 176x166 GUI texture
 * and forwards valve wheel button clicks to the server via the menu button packet.
 */
public class CarbonatorScreen extends AbstractContainerScreen<CarbonatorMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("extrodius_soda", "textures/gui/carbonator.png");

    // Valve wheel button hitbox, measured from Zach's 176x166 artwork.
    private static final int BUTTON_X = 75;
    private static final int BUTTON_Y = 26;
    private static final int BUTTON_W = 32;
    private static final int BUTTON_H = 32;

    public CarbonatorScreen(CarbonatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;
        // NOTE: textureWidth/Height must be the texture's ACTUAL size (176x166),
        // not 256 -- passing 256 stretches the art ~1.45x (mixer lesson).
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 176, 166);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // No labels: the custom artwork has the title baked in.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && isOverValveButton(event.x(), event.y())
                && this.minecraft != null
                && this.minecraft.gameMode != null
                && this.minecraft.player != null
                && this.menu.hasValidRecipe()) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isOverValveButton(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        return x >= BUTTON_X && x < BUTTON_X + BUTTON_W && y >= BUTTON_Y && y < BUTTON_Y + BUTTON_H;
    }
}
