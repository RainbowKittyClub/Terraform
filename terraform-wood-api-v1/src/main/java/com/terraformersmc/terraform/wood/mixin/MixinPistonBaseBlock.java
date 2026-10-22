package com.terraformersmc.terraform.wood.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.terraformersmc.terraform.wood.api.block.SmallLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(PistonBaseBlock.class)
public class MixinPistonBaseBlock {
	// A moved leafy log loses its leaves the way a piston breaks leaves, before the move copies its state;
	// both sides do it, as both run moveBlocks, and like vanilla the client plays the break itself
	@WrapOperation(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToPush()Ljava/util/List;"))
	private List<BlockPos> terraformWood$breakMovedLeaves(PistonStructureResolver resolver, Operation<List<BlockPos>> original, @Local(argsOnly = true) Level level) {
		List<BlockPos> toPush = original.call(resolver);
		for (BlockPos pos : toPush) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof SmallLogBlock log && state.getValue(SmallLogBlock.HAS_LEAVES)) {
				BlockState leaves = log.leavesAt(level, pos).defaultBlockState();
				if (level.isClientSide()) {
					level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(leaves));
				} else {
					Block.dropResources(leaves, level, pos);
				}

				level.setBlock(pos, state.setValue(SmallLogBlock.HAS_LEAVES, false), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			}
		}

		return toPush;
	}
}
