package com.extrodius.sodamod.screen;

import com.extrodius.sodamod.menu.SodaMixerMenu;
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
 * Client screen for the Soda Mixer, in both its moods. Draws Zach's custom
 * 176x166 GUI textures, reveals the arrow sprite left-to-right as the mix
 * button is pressed, and forwards green-button clicks to the server via the
 * menu button packet.
 *
 * The secret: triple-clicking the word "Mixer" in the title toggles the
 * Graveyard Mixer. Zach pixel-aligned the word and the green button between
 * both artworks, so both hitboxes carry over unchanged.
 */
public class SodaMixerScreen extends AbstractContainerScreen<SodaMixerMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("extrodius_soda", "textures/gui/soda_mixer.png");
    private static final Identifier TEXTURE_GRAVEYARD = Identifier.fromNamespaceAndPath("extrodius_soda", "textures/gui/graveyard_mixer.png");
    private static final Identifier ARROW_FULL = Identifier.fromNamespaceAndPath("extrodius_soda", "textures/gui/soda_mixer_arrow_full.png");
    private static final Identifier ARROW_FULL_GRAVEYARD = Identifier.fromNamespaceAndPath("extrodius_soda", "textures/gui/graveyard_mixer_arrow_full.png");

    // Green mix button hitbox, measured from Zach's fixed 176x166 artwork.
    // Pixel-identical in both UIs.
    private static final int BUTTON_X = 119;
    private static final int BUTTON_Y = 53;
    private static final int BUTTON_W = 26;
    private static final int BUTTON_H = 22;

    // Arrow position/size in the GUI; revealed left-to-right with progress.
    private static final int ARROW_X = 90;
    private static final int ARROW_Y = 21;
    private static final int ARROW_W = 22;
    private static final int ARROW_H = 15;

    // Graveyard arrow, measured from the Graveyard Mixer artwork.
    private static final int GY_ARROW_X = 114;
    private static final int GY_ARROW_Y = 21;
    private static final int GY_ARROW_W = 22;
    private static final int GY_ARROW_H = 15;

    // The word "Mixer" in the title, the secret toggle. Covers it in both
    // artworks ("Soda Mixer" and "Graveyard Mixer").
    private static final int MIXER_X = 50;
    private static final int MIXER_Y = 6;
    private static final int MIXER_W = 30;
    private static final int MIXER_H = 10;

    /** Triple-click window for the secret toggle, in milliseconds. */
    private static final long TRIPLE_CLICK_WINDOW_MS = 1500;

    private int mixerClickCount = 0;
    private long lastMixerClickTime = 0;

    public SodaMixerScreen(SodaMixerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;
        boolean graveyard = this.menu.getGraveyardMode();
        Identifier texture = graveyard ? TEXTURE_GRAVEYARD : TEXTURE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 176, 166);

        int arrowX = graveyard ? GY_ARROW_X : ARROW_X;
        int arrowY = graveyard ? GY_ARROW_Y : ARROW_Y;
        int arrowW = graveyard ? GY_ARROW_W : ARROW_W;
        int arrowH = graveyard ? GY_ARROW_H : ARROW_H;
        Identifier arrowFull = graveyard ? ARROW_FULL_GRAVEYARD : ARROW_FULL;
        int filled = (int) Math.ceil(this.menu.getProgress() / (double) SodaMixerMenu.MIXES_REQUIRED * arrowW);
        if (filled > 0) {
            if (filled > arrowW) {
                filled = arrowW;
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, arrowFull,
                    xo + arrowX, yo + arrowY, 0.0F, 0.0F, filled, arrowH, filled, arrowH, arrowW, arrowH);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // No labels: the custom artwork has no label space.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && this.minecraft != null
                && this.minecraft.gameMode != null
                && this.minecraft.player != null) {
            if (isOverMixButton(event.x(), event.y()) && this.menu.hasValidRecipe()) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
                return true;
            }
            if (isOverMixerWord(event.x(), event.y())) {
                long now = System.currentTimeMillis();
                if (now - lastMixerClickTime < TRIPLE_CLICK_WINDOW_MS) {
                    mixerClickCount++;
                } else {
                    mixerClickCount = 1;
                }
                lastMixerClickTime = now;
                if (mixerClickCount >= 3) {
                    mixerClickCount = 0;
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isOverMixButton(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        return x >= BUTTON_X && x < BUTTON_X + BUTTON_W && y >= BUTTON_Y && y < BUTTON_Y + BUTTON_H;
    }

    private boolean isOverMixerWord(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        return x >= MIXER_X && x < MIXER_X + MIXER_W && y >= MIXER_Y && y < MIXER_Y + MIXER_H;
    }
}
