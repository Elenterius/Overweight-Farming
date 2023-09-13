package net.orcinus.overweightfarming.common.registry;


import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.orcinus.overweightfarming.OverweightFarming;

public class OFItemTags {

    public static final TagKey<Item> OVERWEIGHT_HARVESTABLES = TagKey.of(RegistryKeys.ITEM, new Identifier(OverweightFarming.MODID, "overweight_harvestables"));

}