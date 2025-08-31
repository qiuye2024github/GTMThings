package com.hepdd.gtmthings.common.block;

import com.gregtechceu.gtceu.api.GTCEuAPI;

import net.minecraft.world.level.block.Block;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;

public interface BlockMap {

    Object2ObjectOpenHashMap<String, Block[]> MAP = new Object2ObjectOpenHashMap<>();

    static void init() {}

    String namePrefix = "gtmtings.adv_terminal.block_map";

    String heating_coils = "heating_coils";

    static void build() {
        var coils = new ArrayList<>(GTCEuAPI.HEATING_COILS.entrySet());
        coils.sort(Comparator.comparingInt(entry -> entry.getKey().getTier()));
        // 线圈
        MAP.put(heating_coils, coils.stream().map(Map.Entry::getValue).map(Supplier::get).toArray(Block[]::new));
    }
}
