package com.frierenflight.zoltraakcinematic.item;

import com.frierenflight.zoltraakcinematic.registry.ModCinematicAttributes;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
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
 * ⏳ Chronos Dilatation Watch (นาฬิกาพกยืดเวลาสัมพัทธภาพ).
 * An ancient pocket watch whose ticks dilate near intense gravitational fields.
 * Curios Charm slot item: +15% Cooldown Reduction, +35% Black Hole Spell Power.
 */
public class ChronosDilatationWatchItem extends CurioBaseItem {
    public static final String CHARM_SLOT = "charm";

    public ChronosDilatationWatchItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
        withAttributes(CHARM_SLOT,
                new AttributeContainer(AttributeRegistry.COOLDOWN_REDUCTION, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new AttributeContainer(ModCinematicAttributes.BLACK_HOLE_SPELL_POWER, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.zoltraak_cinematic.chronos_dilatation_watch.desc")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
