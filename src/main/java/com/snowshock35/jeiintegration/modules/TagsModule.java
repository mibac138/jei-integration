package com.snowshock35.jeiintegration.modules;

import com.snowshock35.jeiintegration.config.OptionState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.HashSet;
import java.util.Set;

public class TagsModule implements TooltipModule {
    @Override
    public ForgeConfigSpec.ConfigValue<OptionState> createConfigValue(ForgeConfigSpec.Builder builder) {
        return builder
            .comment(" Configure tooltip for tags. E.g. forge:ingot, minecraft:planks")
            .translation("config.jeiintegration.tooltips.tagsTooltipMode")
            .defineEnum("tagsTooltipMode", OptionState.DISABLED);
    }

    @Override
    public void apply(ItemTooltipEvent e) {
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
}
