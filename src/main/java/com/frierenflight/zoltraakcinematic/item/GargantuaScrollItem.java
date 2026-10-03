package com.frierenflight.zoltraakcinematic.item;

import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * 🌌 GargantuaScrollItem
 * Legendary Endgame spell scroll bound to the Gargantua black hole singularity spell.
 * Can be used directly or inscribed into any Legendary-tier spellbook via the Inscription Table.
 */
public class GargantuaScrollItem extends Scroll {
    private final Supplier<? extends AbstractSpell> spell;
    private final int spellLevel;

    public GargantuaScrollItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant());
        this.spell = ModCinematicSpells.GARGANTUA;
        this.spellLevel = 1;
    }

    private void ensureBound(ItemStack stack) {
        if (!stack.isEmpty() && !ISpellContainer.isSpellContainer(stack)) {
            ISpellContainer.createScrollContainer(spell.get(), spellLevel, stack);
        }
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        ensureBound(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        ensureBound(stack);
        super.onCraftedBy(stack, level, player);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        ensureBound(stack);
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ensureBound(stack);
        return super.use(level, player, hand);
    }

    @Override
    public Component getName(ItemStack stack) {
        ensureBound(stack);
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ensureBound(stack);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
