package com.extrodius.sodamod;

import org.slf4j.Logger;

import com.extrodius.sodamod.effect.UndyingDeathHandler;
import com.extrodius.sodamod.item.CaffeineCrashHandler;
import com.extrodius.sodamod.network.UndyingSavePayload;
import com.extrodius.sodamod.menu.CarbonatorMenu;
import com.extrodius.sodamod.menu.GasifierMenu;
import com.extrodius.sodamod.menu.SodaMixerMenu;
import com.extrodius.sodamod.screen.CarbonatorScreen;
import com.extrodius.sodamod.screen.GasifierScreen;
import com.extrodius.sodamod.screen.SodaMixerScreen;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ExtrodiusSoda.MODID)
public class ExtrodiusSoda {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "extrodius_soda";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExtrodiusSoda(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.register(CaffeineCrashHandler.class);
        NeoForge.EVENT_BUS.register(UndyingDeathHandler.class);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(UndyingSavePayload.TYPE, UndyingSavePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // This body only ever runs on the client; the client class
                    // below is resolved lazily, so the server never loads it.
                    com.extrodius.sodamod.client.UndyingSaveClient.play();
                }));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.SODA_MIXER_ITEM);
            event.accept(ModBlocks.GASIFIER_ITEM);
            event.accept(ModBlocks.CARBONATOR_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModBlocks.SODA_CAN_EMPTY);
            event.accept(ModBlocks.CO2_CANISTER_EMPTY);
            event.accept(ModBlocks.CO2_CANISTER_FULL);
        }
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModBlocks.COCOA_COLA_FLAT);
            event.accept(ModBlocks.COCOA_COLA);
            event.accept(ModBlocks.DR_BLAZE_FLAT);
            event.accept(ModBlocks.DR_BLAZE);
            event.accept(ModBlocks.PHANTA_SLIME_FLAT);
            event.accept(ModBlocks.PHANTA_SLIME);
            event.accept(ModBlocks.MOUNTAIN_CREEPER_FLAT);
            event.accept(ModBlocks.MOUNTAIN_CREEPER);
            event.accept(ModBlocks.PHANTA_ORANGE_FLAT);
            event.accept(ModBlocks.PHANTA_ORANGE);
            event.accept(ModBlocks.SPRUCE_BEER_FLAT);
            event.accept(ModBlocks.SPRUCE_BEER);
            event.accept(ModBlocks.PHANTA_CREAM_FLAT);
            event.accept(ModBlocks.PHANTA_CREAM);
            event.accept(ModBlocks.PHANTA_GRAPE_FLAT);
            event.accept(ModBlocks.PHANTA_GRAPE);
            event.accept(ModBlocks.GRAVEYARD);
        }
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SODA_MIXER.get(), SodaMixerScreen::new);
        event.register(ModMenuTypes.GASIFIER.get(), GasifierScreen::new);
        event.register(ModMenuTypes.CARBONATOR.get(), CarbonatorScreen::new);
    }
}
