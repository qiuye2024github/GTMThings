package com.hepdd.gtmthings.common.item;

import com.hepdd.gtmthings.api.gui.widget.AlignLabelWidget;
import com.hepdd.gtmthings.api.gui.widget.BlockMapSelector;
import com.hepdd.gtmthings.api.gui.widget.TerminalInputWidget;
import com.hepdd.gtmthings.api.misc.Hatch;
import com.hepdd.gtmthings.common.block.BlockMap;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.common.block.CoilBlock;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Stream;

import static com.hepdd.gtmthings.api.gui.widget.AlignLabelWidget.ALIGN_CENTER;
import static com.hepdd.gtmthings.api.pattern.AdvancedBlockPattern.getAdvancedBlockPattern;

public class AdvancedTerminalBehavior implements IItemUIFactory {

    private static final String TIER = "gtmtings.auto_build.tier";

    public AdvancedTerminalBehavior() {}

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();
            if (context.getPlayer() != null && !level.isClientSide() &&
                    MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller) {
                AutoBuildSetting autoBuildSetting = getAutoBuildSetting(context.getPlayer().getMainHandItem());

                if (!controller.isFormed()) {
                    getAdvancedBlockPattern(controller.getPattern()).autoBuild(context.getPlayer(), controller.getMultiblockState(), autoBuildSetting);
                } else if (MetaMachine.getMachine(level, blockPos) instanceof WorkableMultiblockMachine workableMultiblockMachine && autoBuildSetting.isreplaceMode()) {
                    getAdvancedBlockPattern(controller.getPattern()).autoBuild(context.getPlayer(), controller.getMultiblockState(), autoBuildSetting);
                    workableMultiblockMachine.onPartUnload();
                }

            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private AutoBuildSetting getAutoBuildSetting(ItemStack itemStack) {
        AutoBuildSetting autoBuildSetting = new AutoBuildSetting();
        var tag = itemStack.getTag();
        if (tag != null && !tag.isEmpty()) {
            autoBuildSetting.Tier = tag.getInt("tier");
            autoBuildSetting.repeatCount = tag.getInt("repeatCount");
            autoBuildSetting.noHatchMode = tag.getBoolean("noHatchMode");
            autoBuildSetting.replaceMode = tag.getBoolean("replaceMode");
            autoBuildSetting.isUseAE = tag.getBoolean("isUseAE");
            autoBuildSetting.IsUseMirror = tag.getBoolean("IsUseMirror");
            autoBuildSetting.module = tag.getBoolean("module");
            var block = tag.getString("block");
            if (!block.isEmpty()) {
                autoBuildSetting.tierBlock = BlockMap.MAP.get(block);
                autoBuildSetting.blocks = new ReferenceOpenHashSet<>(autoBuildSetting.tierBlock);
            }
        }
        return autoBuildSetting;
    }

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player entityPlayer) {
        return new ModularUI(176, 166, holder, entityPlayer).widget(createWidget(entityPlayer));
    }

    private Widget createWidget(Player entityPlayer) {
        ItemStack handItem = entityPlayer.getMainHandItem();
        var group = new WidgetGroup(0, 0, 182 + 8, 117 + 8);
        int rowIndex = 1;
        List<Component> lines = new ArrayList<>(List.of());
        lines.add(Component.translatable("item.gtmthings.advanced_terminal.setting.1.tooltip"));
        GTCEuAPI.HEATING_COILS.entrySet().stream()
                .sorted(Comparator.comparingInt(value -> value.getKey().getTier()))
                .forEach(coil -> lines.add(Component.literal(String.valueOf(coil.getKey().getTier() + 1))
                        .append(":").append(coil.getValue().get().getName())));
        group.addWidget(
                new DraggableScrollableWidgetGroup(4, 4, 182, 117)
                        .setBackground(GuiTextures.DISPLAY)
                        .setYScrollBarWidth(2)
                        .setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1))
                        .addWidget(new AlignLabelWidget(89, 5, "item.gtmthings.advanced_terminal.setting.title")
                                .setTextAlign(ALIGN_CENTER))
                        .addWidget(new LabelWidget(4, 5 + 16 * rowIndex, () -> {
                            var category = BlockMap.MAP.getOrDefault(handItem.getOrCreateTag().getString("block"), new Block[0]);
                            var tier0 = handItem.getOrCreateTag().getInt("tier");
                            if (category.length == 0 || tier0 <= 0 || tier0 > category.length) return Component.translatable(TIER).getString();
                            return Component.translatable(TIER)
                                    .append("(")
                                    .append(Stream.of(category).map(Block::getName).toList().get(tier0 - 1))
                                    .append(")")
                                    .getString();
                        }) {

                            @OnlyIn(Dist.CLIENT)
                            protected void drawTooltipTexts(int mouseX, int mouseY) {
                                if (this.isMouseOverElement(mouseX, mouseY) && this.getHoverElement(mouseX, mouseY) == this && this.gui != null && this.gui.getModularUIGui() != null) {
                                    List<Component> lines = new ArrayList<>(List.of());
                                    int i = 0;
                                    for (var block : BlockMap.MAP.getOrDefault(handItem.getOrCreateTag().getString("block"), new Block[0])) {
                                        i++;
                                        lines.add(Component.literal(String.valueOf(i)).append(":").append(block.getName()));
                                    }
                                    this.gui.getModularUIGui().setHoverTooltip(lines, ItemStack.EMPTY, null, null);
                                }
                            }
                        })
                        .addWidget(new BlockMapSelector(96, 4, 76, 12, (category, tier0) -> {
                            if (category != null && tier0 != null) {
                                var tag = handItem.getOrCreateTag();
                                tag.putString("block", category);
                                tag.putInt("tier", tier0);
                                handItem.setTag(tag);
                            }
                        }))
                        .addWidget(new TerminalInputWidget(140, 5 + 16 * rowIndex++, 20, 16, () -> getTier(handItem),
                                (v) -> setTier(v, handItem))
                                .setMin(0).setMax(100))
                        .addWidget(new LabelWidget(4, 5 + 16 * rowIndex, "item.gtmthings.advanced_terminal.setting.2")
                                .setHoverTooltips(Component.translatable("item.gtmthings.advanced_terminal.setting.2.tooltip")))
                        .addWidget(new TerminalInputWidget(140, 5 + 16 * rowIndex++, 25, 16, () -> getRepeatCount(handItem),
                                (v) -> setRepeatCount(v, handItem))
                                .setMin(0).setMax(1000))
                        .addWidget(new LabelWidget(4, 5 + 16 * rowIndex, "item.gtmthings.advanced_terminal.setting.3")
                                .setHoverTooltips("item.gtmthings.advanced_terminal.setting.3.tooltip"))
                        .addWidget(new SwitchWidget(140, 5 + 16 * rowIndex++, 25, 16,
                                (c, v) -> setIsBuildHatches(!getIsBuildHatches(handItem), handItem))
                                .setPressed(getIsBuildHatches(handItem))
                                .setTexture(new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("OFF")),
                                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("ON"))))
                        .addWidget(new LabelWidget(4, 5 + 16 * rowIndex, "item.gtmthings.advanced_terminal.setting.4")
                                .setHoverTooltips("item.gtmthings.advanced_terminal.setting.4.tooltip"))
                        .addWidget(new SwitchWidget(140, 5 + 16 * rowIndex++, 25, 16,
                                (c, v) -> setReplaceMode(!getReplaceMode(handItem), handItem))
                                .setPressed(getReplaceMode(handItem))
                                .setTexture(new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("OFF")),
                                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("ON"))))
                        .addWidget(new LabelWidget(4, 5 + 16 * rowIndex, "item.gtmthings.advanced_terminal.setting.5")
                                .setHoverTooltips("item.gtmthings.advanced_terminal.setting.5.tooltip"))
                        .addWidget(new SwitchWidget(140, 5 + 16 * rowIndex++, 25, 16,
                                (c, v) -> setIsUseAE(!getIsUseAE(handItem), handItem))
                                .setPressed(getIsUseAE(handItem))
                                .setTexture(new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("OFF")),
                                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("ON"))))
                        .addWidget(new LabelWidget(4, 5 + 16 * rowIndex, "item.gtmthings.advanced_terminal.setting.6")
                                .setHoverTooltips("item.gtmthings.advanced_terminal.setting.6.tooltip"))
                        .addWidget(new SwitchWidget(140, 5 + 16 * rowIndex++, 25, 16,
                                (c, v) -> setIsUseMirror(!getIsUseMirror(handItem), handItem))
                                .setPressed(getIsUseMirror(handItem))
                                .setTexture(new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("OFF")),
                                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("ON"))))
                        .addWidget(new LabelWidget(4, 5 + 16 * rowIndex, "item.gtmthings.advanced_terminal.setting.7")
                                .setHoverTooltips("item.gtmthings.advanced_terminal.setting.7.tooltip"))
                        .addWidget(new SwitchWidget(140, 5 + 16 * rowIndex++, 25, 16,
                                (c, v) -> setModule(!getModule(handItem), handItem))
                                .setPressed(getModule(handItem))
                                .setTexture(new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("OFF")),
                                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("ON")))));
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    private int getTier(ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag != null && !tag.isEmpty() && tag.contains("CoilTier")) {
            return tag.getInt("CoilTier");
        } else {
            return 0;
        }
    }

    private void setTier(int coilTier, ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putInt("CoilTier", coilTier);
        itemStack.setTag(tag);
    }

    private int getRepeatCount(ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag != null && !tag.isEmpty() && tag.contains("RepeatCount")) {
            return tag.getInt("RepeatCount");
        } else {
            return 0;
        }
    }

    private void setRepeatCount(int repeatCount, ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putInt("RepeatCount", repeatCount);
        itemStack.setTag(tag);
    }

    private boolean getIsBuildHatches(ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag != null && !tag.isEmpty() && tag.contains("NoHatchMode")) {
            return tag.getBoolean("NoHatchMode");
        } else {
            return true;
        }
    }

    private void setIsBuildHatches(boolean isBuildHatches, ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putBoolean("NoHatchMode", isBuildHatches);
        itemStack.setTag(tag);
    }

    private boolean getReplaceMode(ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag != null && !tag.isEmpty() && tag.contains("ReplaceCoilMode")) {
            return tag.getBoolean("ReplaceCoilMode");
        } else {
            return false;
        }
    }

    private void setReplaceMode(boolean isReplaceCoil, ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putBoolean("ReplaceCoilMode", isReplaceCoil);
        itemStack.setTag(tag);
    }

    private boolean getIsUseAE(ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag != null && !tag.isEmpty() && tag.contains("IsUseAE")) {
            return tag.getBoolean("IsUseAE");
        } else {
            return false;
        }
    }

    private void setIsUseAE(boolean isUseAE, ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putBoolean("IsUseAE", isUseAE);
        itemStack.setTag(tag);
    }

    private boolean getIsUseMirror(ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag != null && !tag.isEmpty() && tag.contains("IsUseMirror")) {
            return tag.getBoolean("IsUseMirror");
        } else {
            return false; // 默认值为 0 (否)
        }
    }

    private void setIsUseMirror(boolean isUseMirror, ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putBoolean("IsUseMirror", isUseMirror);
        itemStack.setTag(tag);
    }

    private boolean getModule(ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag != null && !tag.isEmpty()) {
            return tag.getBoolean("module");
        } else {
            return false;
        }
    }

    private void setModule(boolean isFlip, ItemStack itemStack) {
        var tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putBoolean("module", isFlip);
        itemStack.setTag(tag);
    }

    @Setter
    @Getter
    public static class AutoBuildSetting {

        Block[] tierBlock;
        Set<Block> blocks = Collections.emptySet();

        private int Tier, repeatCount;

        private boolean noHatchMode, replaceMode, isUseAE, IsUseMirror, module;

        public AutoBuildSetting() {
            this.Tier = 0;
            this.repeatCount = 0;
            this.replaceMode = false;
            this.noHatchMode = true;
            this.isUseAE = false;
            this.IsUseMirror = false;
            this.module = false;
        }

        public List<ItemStack> apply(BlockInfo[] blockInfos) {
            List<ItemStack> candidates = new ArrayList<>();
            if (blockInfos != null) {
                if (Arrays.stream(blockInfos).anyMatch(
                        info -> info.getBlockState().getBlock() instanceof CoilBlock)) {
                    var tier = Math.min(Tier - 1, blockInfos.length - 1);
                    if (tier == -1) {
                        for (int i = 0; i < blockInfos.length - 1; i++) {
                            candidates.add(blockInfos[i].getItemStackForm());
                        }
                    } else {
                        candidates.add(blockInfos[tier].getItemStackForm());
                    }
                    return candidates;
                }
                for (BlockInfo info : blockInfos) {
                    if (info.getBlockState().getBlock() != Blocks.AIR) candidates.add(info.getItemStackForm());
                }
            }
            return candidates;
        }

        public boolean isPlaceHatch(BlockInfo[] blockInfos) {
            if (!this.noHatchMode) return true;
            if (blockInfos != null && blockInfos.length > 0) {
                var blockInfo = blockInfos[0];
                return !(blockInfo.getBlockState().getBlock() instanceof MetaMachineBlock machineBlock) ||
                        !Hatch.Set.contains(machineBlock);
            }
            return true;
        }

        public boolean isreplaceMode() {
            return replaceMode;
        }

        public boolean isUseMirror() {
            return IsUseMirror;
        }
    }
}
