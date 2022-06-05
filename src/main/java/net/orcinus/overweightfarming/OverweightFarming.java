package net.orcinus.overweightfarming;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;
import net.orcinus.overweightfarming.registry.OFObjects;
import net.orcinus.overweightfarming.util.EmeraldToItemOffer;
import net.orcinus.overweightfarming.util.OFUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;


public class OverweightFarming implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "overweight_farming";
    public static OFConfig config;



    @Override
    public void onInitialize() {
        AutoConfig.register(OFConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(OFConfig.class).getConfig();
        OFObjects.init();

        TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 5, factories -> {
            factories.add(new EmeraldToItemOffer(new ItemStack(OFObjects.STRAW_HAT), 20, 1, 12, 0.05F));
        });

        UseBlockCallback.EVENT.register(this::stripMelon);
        UseBlockCallback.EVENT.register(this::growBloodroot);
        UseEntityCallback.EVENT.register(this::interactPig);
    }

    private ActionResult interactPig(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult entityHitResult) {
        ItemStack stack = player.getStackInHand(hand);
        if (entity instanceof PigEntity pigEntity) {
            if (stack.getItem() == OFObjects.VEGETABLE_PEELS) {
                int i = pigEntity.getBreedingAge();
                if (!world.isClient() && i == 0 && pigEntity.isReadyToBreed()) {
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    pigEntity.lovePlayer(player);
                    pigEntity.emitGameEvent(GameEvent.MOB_INTERACT, new BlockPos(pigEntity.getEyePos()));
                    return ActionResult.SUCCESS;
                }
                if (pigEntity.isBaby()) {
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    pigEntity.growUp((int)((float)(-i / 20) * 0.1F), true);
                    pigEntity.emitGameEvent(GameEvent.MOB_INTERACT, new BlockPos(pigEntity.getEyePos()));
                    player.swingHand(hand);
                }
                if (world.isClient()) {
                    return ActionResult.PASS;
                }
            }
        }
        return ActionResult.PASS;
    }

    private ActionResult growBloodroot(PlayerEntity player, World world, Hand hand, BlockHitResult blockHitResult) {

        if(FabricLoader.getInstance().isModLoaded("bwplus") && Registry.BLOCK.containsId(new Identifier("bwplus","bloodroot"))){
            if(world.getBlockState(blockHitResult.getBlockPos()).isOf(Registry.BLOCK.get(new Identifier("bwplus","bloodroot")))){
                if(player.getMainHandStack().getItem() instanceof BoneMealItem){
                    player.getMainHandStack().decrement(1);
                    if (!world.isClient) {
                        world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, blockHitResult.getBlockPos(), 0);
                    }
                    if(world.getRandom().nextFloat() > 0.75F){
                        world.setBlockState(blockHitResult.getBlockPos(), OFObjects.OVERWEIGHT_BLOODROOT.getDefaultState(), 1);
                        if(world.getBlockState(blockHitResult.getBlockPos().up()).isOf(Blocks.AIR) ){
                            world.setBlockState(blockHitResult.getBlockPos().up(), OFObjects.OVERWEIGHT_BLOODROOT_STEM.getDefaultState(), 1);
                        }
                    }
                    return ActionResult.SUCCESS;

                }
            }

        }
        return ActionResult.PASS;
    }

    public ActionResult stripMelon(PlayerEntity player, World world, Hand hand, BlockHitResult blockHitResult) {
        ItemStack stack = player.getMainHandStack();
        BlockState state = world.getBlockState(blockHitResult.getBlockPos());
        BlockPos blockPos = blockHitResult.getBlockPos();

        if (stack.getItem() instanceof AxeItem) {
            for (Block block : OFUtils.WAX_OFF_BY_BLOCK.get().keySet()) {
                if (state.isOf(block)) {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        Criteria.ITEM_USED_ON_BLOCK.trigger(serverPlayer, blockPos, stack);
                    }
                    stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                    world.playSound(null, blockPos, SoundEvents.ITEM_AXE_SCRAPE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.setBlockState(blockPos, OFUtils.WAX_OFF_BY_BLOCK.get().get(block).getDefaultState());
                    world.syncWorldEvent(player, 3004, blockPos, 0);
                    player.swingHand(hand);
                }
            }


            for (Block block : OFUtils.PEELABLES.get().keySet()) {
                if (state.isOf(block)) {
                    stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                    world.playSound(null, blockPos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    Block.dropStack(world, blockPos, new ItemStack(OFObjects.VEGETABLE_PEELS));
                    world.setBlockState(blockPos, OFUtils.PEELABLES.get().get(block).getDefaultState());
                    player.swingHand(hand);
                }
            }
        }
        if (stack.getItem() == Items.HONEYCOMB) {
            for (Block block : OFUtils.WAXABLES.get().keySet()) {
                if (state.isOf(block)) {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        Criteria.ITEM_USED_ON_BLOCK.trigger(serverPlayer, blockPos, stack);
                    }
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    world.playSound(null, blockPos, SoundEvents.ITEM_HONEYCOMB_WAX_ON, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.setBlockState(blockPos, OFUtils.WAXABLES.get().get(block).getDefaultState());
                    world.syncWorldEvent(player, 3003, blockPos, 0);
                    return ActionResult.success(world.isClient());
                }
            }
        }

        if (stack.getItem() == OFObjects.VEGETABLE_PEELS) {
            for (Block block : OFUtils.UNPEELABLES.get().keySet()) {
                if (state.isOf(block)) {
                    if (!player.getAbilities().creativeMode){
                        stack.decrement(1);
                    }
                    world.playSound(null, blockPos, SoundEvents.ENTITY_GLOW_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.setBlockState(blockPos, OFUtils.UNPEELABLES.get().get(block).getDefaultState());
                    return ActionResult.success(true);
                }
            }
        }

        return ActionResult.PASS;
    }
}