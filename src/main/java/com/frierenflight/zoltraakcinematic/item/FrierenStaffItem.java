package com.frierenflight.zoltraakcinematic.item;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * Frieren's Staff - Iconic magic weapon with powerful Iron's Spells attributes:
 * - Spell Power: +45%
 * - Max Mana: +500
 * - Cooldown Reduction: -20% (+0.20 reduction)
 */
public class FrierenStaffItem extends StaffItem {

    public FrierenStaffItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .attributes(createStaffAttributes()));
    }

    private static ItemAttributeModifiers createStaffAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(AttributeRegistry.SPELL_POWER,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "staff_spell_power"),
                                0.45, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        EquipmentSlotGroup.MAINHAND)
                .add(AttributeRegistry.MAX_MANA,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "staff_max_mana"),
                                500.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(AttributeRegistry.COOLDOWN_REDUCTION,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "staff_cooldown_reduction"),
                                0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.zoltraak_cinematic.frieren_staff.desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
