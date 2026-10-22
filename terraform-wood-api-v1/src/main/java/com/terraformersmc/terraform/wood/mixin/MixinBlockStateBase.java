package com.terraformersmc.terraform.wood.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.terraformersmc.terraform.wood.api.block.SmallLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinBlockStateBase {
	// A leafy small log's map colour is its leaves', and those can differ per position;
	// some callers pass null for both, which vanilla always ignored
	@ModifyReturnValue(method = "getMapColor", at = @At("RETURN"))
	private MapColor terraformWood$storedLeavesMapColor(MapColor original, @Local(argsOnly = true) BlockGetter level, @Local(argsOnly = true) BlockPos pos) {
		BlockState state = (BlockState) (Object) this;
		if (level != null && pos != null && state.getBlock() instanceof SmallLogBlock && state.getValue(SmallLogBlock.HAS_LEAVES)) {
			Block stored = SmallLogBlock.LEAVES.get(level, pos);
			if (stored != null) {
				return stored.defaultMapColor();
			}
		}

		return original;
	}
}
