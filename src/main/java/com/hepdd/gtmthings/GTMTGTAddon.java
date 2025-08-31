package com.hepdd.gtmthings;

import com.hepdd.gtmthings.common.block.BlockMap;
import com.hepdd.gtmthings.common.registry.GTMTRegistration;
import com.hepdd.gtmthings.data.GTMTRecipe;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

@GTAddon
public class GTMTGTAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return GTMTRegistration.GTMTHINGS_REGISTRATE;
    }

    @Override
    public void initializeAddon() {
        BlockMap.init();
    }

    @Override
    public String addonModId() {
        return GTMThings.MOD_ID;
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        GTMTRecipe.init(provider);
    }
}
