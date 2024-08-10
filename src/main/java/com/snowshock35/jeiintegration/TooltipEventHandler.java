/*
 * MIT License
 *
 * Copyright (c) 2020 SnowShock35
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.snowshock35.jeiintegration;

import com.mojang.blaze3d.platform.InputConstants;
import com.snowshock35.jeiintegration.config.Config;
import com.snowshock35.jeiintegration.config.OptionState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;

import static net.minecraftforge.common.ForgeHooks.getBurnTime;

public class TooltipEventHandler {

    private static final Config.Client config = Config.CLIENT;

    private static boolean isDebugMode() {
        return Minecraft.getInstance().options.advancedItemTooltips;
    }

    private static boolean isShiftKeyDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
            InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static boolean isEnabled(ConfigValue<OptionState> configValue) {
        OptionState configOption = configValue.get();
        boolean isEnabled = false;

        if (configOption == OptionState.ENABLED) {
            isEnabled = true;
        } else if (configOption == OptionState.ON_SHIFT && isShiftKeyDown()) {
            isEnabled = true;
        } else if (configOption == OptionState.ON_DEBUG && isDebugMode()) {
            isEnabled = true;
        } else if (configOption == OptionState.ON_SHIFT_AND_DEBUG && isShiftKeyDown() && isDebugMode()) {
            isEnabled = true;
        }
        return isEnabled;
    }

    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(() -> {
        // Set number formatting to display large numbers more clearly
        DecimalFormat format = new DecimalFormat("#.##");
        format.setGroupingUsed(true);
        format.setGroupingSize(3);
        return format;
    });

    private static final List<Pair<ConfigValue<OptionState>, Consumer<ItemTooltipEvent>>> MODULES =
        new ArrayList<>();

    static {
        MODULES.add(Pair.of(config.burnTimeTooltipMode, TooltipEventHandler::burnTime));
        MODULES.add(Pair.of(config.durabilityTooltipMode, TooltipEventHandler::durability));
        MODULES.add(Pair.of(config.enchantabilityTooltipMode, TooltipEventHandler::enchantability));
        MODULES.add(Pair.of(config.foodTooltipMode, TooltipEventHandler::hunger));
        MODULES.add(Pair.of(config.nbtTooltipMode, TooltipEventHandler::nbtData));
        MODULES.add(Pair.of(config.registryNameTooltipMode, TooltipEventHandler::registryName));
        MODULES.add(Pair.of(config.maxStackSizeTooltipMode, TooltipEventHandler::maxStackSize));
        MODULES.add(Pair.of(config.tagsTooltipMode, TooltipEventHandler::tags));
        MODULES.add(Pair.of(config.translationKeyTooltipMode, TooltipEventHandler::translationKey));
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent e) {
        if (e.getItemStack().isEmpty()) {
            return;
        }

        for (Pair<ConfigValue<OptionState>, Consumer<ItemTooltipEvent>> module : MODULES) {
            if (isEnabled(module.getLeft())) {
                module.getRight().accept(e);
            }
        }
    }

    private static void burnTime(ItemTooltipEvent e) {
        int burnTime = 0;
        try {
            burnTime = getBurnTime(e.getItemStack(), RecipeType.SMELTING);
        } catch (Exception ex) {
            JEIIntegration.logger.log(Level.WARN, "):\n\nSomething went wrong!");
        }

        if (burnTime > 0) {
            Component burnTooltip = Component.translatable("tooltip.jeiintegration.burnTime")
                                             .append(Component.literal(" " + DECIMAL_FORMAT.get()
                                                                                           .format(burnTime) + " "))
                                             .append(Component.translatable("tooltip.jeiintegration.burnTime.suffix"))
                                             .withStyle(ChatFormatting.DARK_GRAY);
            e.getToolTip().add(burnTooltip);
        }
    }

    private static void durability(ItemTooltipEvent e) {
        ItemStack itemStack = e.getItemStack();
        int maxDamage = itemStack.getMaxDamage();
        int currentDamage = maxDamage - itemStack.getDamageValue();
        if (maxDamage > 0) {
            Component durabilityTooltip = Component.translatable("tooltip.jeiintegration.durability")
                                                   .append(Component.literal(" " + currentDamage + "/" + maxDamage))
                                                   .withStyle(ChatFormatting.DARK_GRAY);
            e.getToolTip().add(durabilityTooltip);
        }
    }

    private static void enchantability(ItemTooltipEvent e) {
        int enchantability = e.getItemStack().getEnchantmentValue();
        if (enchantability > 0) {
            Component enchantabilityTooltip = Component.translatable("tooltip.jeiintegration.enchantability")
                                                       .append(Component.literal(" " + enchantability))
                                                       .withStyle(ChatFormatting.DARK_GRAY);
            e.getToolTip().add(enchantabilityTooltip);
        }
    }

    private static void hunger(ItemTooltipEvent e) {
        FoodProperties foodProperties = e.getItemStack().getFoodProperties(Minecraft.getInstance().player);
        if (e.getItemStack().getItem().isEdible() && foodProperties != null) {
            int healVal = foodProperties.getNutrition();
            float satVal = healVal * (foodProperties.getSaturationModifier() * 2);

            Component foodTooltip = Component.translatable("tooltip.jeiintegration.hunger")
                                             .append(Component.literal(" " + healVal + " "))
                                             .append(Component.translatable("tooltip.jeiintegration.saturation"))
                                             .append(Component.literal(" " + DECIMAL_FORMAT.get().format(satVal)))
                                             .withStyle(ChatFormatting.DARK_GRAY);
            e.getToolTip().add(foodTooltip);
        }
    }

    private static void nbtData(ItemTooltipEvent e) {
        CompoundTag nbtData = e.getItemStack().getShareTag();
        if (nbtData != null) {
            Component nbtTooltip = Component.translatable("tooltip.jeiintegration.nbtTagData")
                                            .append(Component.literal(" " + nbtData))
                                            .withStyle(ChatFormatting.DARK_GRAY);
            e.getToolTip().add(nbtTooltip);
        }
    }

    private static void registryName(ItemTooltipEvent e) {
        Component registryTooltip = Component.translatable("tooltip.jeiintegration.registryName")
                                             .append(Component.literal(" " + ForgeRegistries.ITEMS.getKey(e.getItemStack().getItem())))
                                             .withStyle(ChatFormatting.DARK_GRAY);
        e.getToolTip().add(registryTooltip);
    }

    private static void maxStackSize(ItemTooltipEvent e) {
        int stackSize = e.getItemStack().getMaxStackSize();
        if (stackSize > 0) {
            Component stackSizeTooltip = Component.translatable("tooltip.jeiintegration.maxStackSize")
                                                  .append(Component.literal(" " + stackSize))
                                                  .withStyle(ChatFormatting.DARK_GRAY);
            e.getToolTip().add(stackSizeTooltip);
        }
    }

    private static void tags(ItemTooltipEvent e) {
        ItemStack itemStack = e.getItemStack();
        if (!itemStack.getTags().toList().isEmpty()) {
            Component tagsTooltip = Component.translatable("tooltip.jeiintegration.tags")
                                             .withStyle(ChatFormatting.DARK_GRAY);

            Set<Component> tags = new HashSet<>();

            for (ResourceLocation tag : itemStack.getTags().map(TagKey::location).toList()) {
                tags.add(Component.literal("    " + tag).withStyle(ChatFormatting.DARK_GRAY));
            }

            e.getToolTip().add(tagsTooltip);
            e.getToolTip().addAll(tags);
        }
    }

    private static void translationKey(ItemTooltipEvent e) {
        Component translationKeyTooltip = Component.translatable("tooltip.jeiintegration.translationKey")
                                                   .append(Component.literal(" " + e.getItemStack().getDescriptionId()))
                                                   .withStyle(ChatFormatting.DARK_GRAY);
        e.getToolTip().add(translationKeyTooltip);
    }
}
