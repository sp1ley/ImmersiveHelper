package org.andbobiq.imh_mod.registry;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import org.andbobiq.imh_mod.IMHModFabric;

public class ModItems {

    public static void register() {

    }

    private static Item register(String id, Item item) {
        return Registry.register(
                Registries.ITEM,
                IMHModFabric.id(id),
                item
        );
    }
}