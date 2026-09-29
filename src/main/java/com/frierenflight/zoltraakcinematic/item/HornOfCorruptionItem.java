package com.frierenflight.zoltraakcinematic.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 📜 Horn of Corruption (เขาของมหาจอมเวทควาล).
 * The severed demonic horn of Qual, heavily saturated with the primordial mana of Zoltraak.
 * Acts as a legendary catalyst for dark staves and high-tier eldritch artifacts.
 */
public class HornOfCorruptionItem extends Item {

    public HornOfCorruptionItem() {
        super(new Item.Properties()
                .stacksTo(16)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.zoltraak_cinematic.horn_of_corruption.desc")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
