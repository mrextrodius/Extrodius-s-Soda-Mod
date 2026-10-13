package com.extrodius.sodamod.screen;

import com.extrodius.sodamod.menu.GasifierMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the Gasifier. Draws Zach's custom 176x166 GUI texture,
 * reveals the arrow sprite left-to-right with cook progress, and draws the
 * flame wisps bottom-up with remaining fuel burn time.
 */
public class GasifierScreen extends AbstractContainerScreen<GasifierMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("extrodius_soda", "textures/gui/gasifier.png");
    private static final Identifier ARROW_FULL = Identifier.fromNamespaceAndPath("extrodius_soda", "textures/gui/gasifier_arrow_full.png");
    private static final Identifier FLAME_FULL = Identifier.fromNamespaceAndPath("extrodius_soda", "textures/gui/gasifier_flame_full.png");

    // Arrow position/size in the GUI; revealed left-to-right with cook progress.
    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 21;
    private static final int ARROW_W = 22;
    private static final int ARROW_H = 15;

    // Flame position/size in the GUI; revealed bottom-up with burn progress.
    private static final int FLAME_X = 57;
    private static final int FLAME_Y = 37;
    private static final int FLAME_W = 13;
    private static final int FLAME_H = 13;

    public GasifierScreen(GasifierMenu menu, Inventory playerInventory, Component title) {
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

        int filled = (int) Math.ceil(this.menu.getCookProgress() * ARROW_W);
        if (filled > 0) {
            if (filled > ARROW_W) {
                filled = ARROW_W;
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW_FULL,
                    xo + ARROW_X, yo + ARROW_Y, 0.0F, 0.0F, filled, ARROW_H, filled, ARROW_H, ARROW_W, ARROW_H);
        }

        int flameH = (int) Math.ceil(this.menu.getBurnProgress() * FLAME_H);
        if (flameH > 0) {
            if (flameH > FLAME_H) {
                flameH = FLAME_H;
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, FLAME_FULL,
                    xo + FLAME_X, yo + FLAME_Y + FLAME_H - flameH,
                    0.0F, (float) (FLAME_H - flameH), FLAME_W, flameH, FLAME_W, flameH, FLAME_W, FLAME_H);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // No labels: the custom artwork has the title baked in.
    }
}
