package com.frierenflight.zoltraakcinematic.item;

import com.frierenflight.zoltraakcinematic.registry.ModCinematicAttributes;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.SpellBook;
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
 * 📜 Grimoire of the Elder Sage (บันทึกมหาเวทของควาล).
 * The ancient forbidden spellbook of Qual, containing the raw calculus of Killing Magic (Zoltraak).
 *
 * Stats:
 * - 12 Spell Inscription Slots
 * - +50% Zoltraak Spell Power
 * - +20% Overall Spell Power
 * - +1000 Max Mana
 * - +30% Cooldown Reduction
 * - +20% Cast Time Reduction
 */
public class ElderSageGrimoireItem extends SpellBook {

    public ElderSageGrimoireItem() {
        super(12, new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
        withSpellbookAttributes(
                new AttributeContainer(ModCinematicAttributes.ZOLTRAAK_SPELL_POWER, 0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new AttributeContainer(AttributeRegistry.SPELL_POWER, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new AttributeContainer(AttributeRegistry.MAX_MANA, 1000.0, AttributeModifier.Operation.ADD_VALUE),
                new AttributeContainer(AttributeRegistry.COOLDOWN_REDUCTION, 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new AttributeContainer(AttributeRegistry.CAST_TIME_REDUCTION, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.zoltraak_cinematic.elder_sage_grimoire.desc")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
