package com.frierenflight.zoltraakcinematic.item;

import com.frierenflight.zoltraakcinematic.registry.ModCinematicAttributes;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.item.curios.CurioBaseItem;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 📜 Qual's Core of Corruption (แก่นแท้แห่งการเน่าเปื่อย).
 * A pulsating dark core harvested from Qual's chest cavity.
 * Curios Necklace:
 * - +35% Zoltraak Spell Power
 * - +350 Max Mana
 * - +2.5 Mana Regen
 */
public class CorruptionCoreItem extends CurioBaseItem {

    public CorruptionCoreItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
        withAttributes(Curios.NECKLACE_SLOT,
                new AttributeContainer(ModCinematicAttributes.ZOLTRAAK_SPELL_POWER, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new AttributeContainer(AttributeRegistry.MAX_MANA, 350.0, AttributeModifier.Operation.ADD_VALUE),
                new AttributeContainer(AttributeRegistry.MANA_REGEN, 2.5, AttributeModifier.Operation.ADD_VALUE)
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.zoltraak_cinematic.corruption_core.desc")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
