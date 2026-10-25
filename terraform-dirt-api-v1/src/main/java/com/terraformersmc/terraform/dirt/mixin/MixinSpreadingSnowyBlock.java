package com.terraformersmc.terraform.dirt.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.terraformersmc.terraform.dirt.api.block.TerraformGrassBlock;
import com.terraformersmc.terraform.dirt.api.block.TerraformMyceliumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MyceliumBlock;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SpreadingSnowyBlock.class)
public abstract class MixinSpreadingSnowyBlock {
	@Inject(method = "randomTick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z")
	)
	private void terraformDirt$customSpreads(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo info, @Local(ordinal = 1) BlockPos spreadingPos) {
		// Mycelium must only turn custom dirt into custom mycelium, never into custom grass.
		Map<Block, Block> spreadsTo = state.getBlock() instanceof MyceliumBlock
				? TerraformMyceliumBlock.MYCELIUM_SPREADS_TO
				: TerraformGrassBlock.GRASS_SPREADS_TO;

		Block spreadBlock = spreadsTo.get(level.getBlockState(spreadingPos).getBlock());
		if (spreadBlock != null) {
			BlockState spreadDefaultState = spreadBlock.defaultBlockState();
			if (TerraformGrassBlock.canPropagate(spreadDefaultState, level, spreadingPos)) {
				level.setBlockAndUpdate(spreadingPos, spreadDefaultState.setValue(SpreadingSnowyBlock.SNOWY, level.getBlockState(spreadingPos.above()).getBlock() == Blocks.SNOW));
			}
		}
	}
}
