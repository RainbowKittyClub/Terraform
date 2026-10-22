package com.terraformersmc.terraform.positiondata.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.terraformersmc.terraform.positiondata.impl.PositionDataImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
public class MixinLevelChunk {
	// The state replaced, or null when nothing changed; no callback object on this hot path
	@ModifyReturnValue(method = "setBlockState", at = @At("RETURN"))
	private @Nullable BlockState terraformPositionData$dropUnheld(@Nullable BlockState old, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState state) {
		if (old != null) {
			PositionDataImpl.onBlockChanged((LevelChunk) (Object) this, pos, old, state);
		}

		return old;
	}
}
