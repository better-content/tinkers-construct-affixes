package com.bettercontent.tinkersconstructaffixes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class FontExclusionLootModifier extends LootModifier {
    public static final Codec<FontExclusionLootModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, FontExclusionLootModifier::new));
    private static final Set<String> EXCLUDED_DIMENSIONS = Set.of("the_bumblezone:the_bumblezone", "rats:ratlantis");

    public FontExclusionLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!EXCLUDED_DIMENSIONS.contains(context.getLevel().dimension().location().toString())) return loot;
        loot.removeIf(stack -> {
            var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return (id != null && id.getNamespace().equals("tinkers_construct_affixes"))
                || (stack.hasTag() && stack.getTag().contains("tinkers_construct_affixes_affixes"));
        });
        return loot;
    }

    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
