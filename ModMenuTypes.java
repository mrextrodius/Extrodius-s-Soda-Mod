package com.extrodius.sodamod;

import com.extrodius.sodamod.menu.CarbonatorMenu;
import com.extrodius.sodamod.menu.GasifierMenu;
import com.extrodius.sodamod.menu.SodaMixerMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Menu type registration for Extrodius's Soda Mod.
 *
 * The factory reads the extra BlockPos written by the block when the screen is
 * opened, so the client menu can find its block entity.
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, ExtrodiusSoda.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SodaMixerMenu>> SODA_MIXER =
            MENU_TYPES.register("soda_mixer", () -> IMenuTypeExtension.create(
                    (IContainerFactory<SodaMixerMenu>) (containerId, playerInventory, buf) ->
                            new SodaMixerMenu(containerId, playerInventory, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<GasifierMenu>> GASIFIER =
            MENU_TYPES.register("gasifier", () -> IMenuTypeExtension.create(
                    (IContainerFactory<GasifierMenu>) (containerId, playerInventory, buf) ->
                            new GasifierMenu(containerId, playerInventory, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<CarbonatorMenu>> CARBONATOR =
            MENU_TYPES.register("carbonator", () -> IMenuTypeExtension.create(
                    (IContainerFactory<CarbonatorMenu>) (containerId, playerInventory, buf) ->
                            new CarbonatorMenu(containerId, playerInventory, buf)));
}
