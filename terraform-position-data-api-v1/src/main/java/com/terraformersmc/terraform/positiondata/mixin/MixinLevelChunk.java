package com.terraformersmc.terraform.positiondata.mixin;

import com.terraformersmc.terraform.positiondata.impl.PositionDataImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class MixinLevelChunk {
	// Null means nothing changed
	@Inject(method = "setBlockState", at = @At("RETURN"))
	private void terraformPositionData$dropUnheld(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
		if (cir.getReturnValue() != null) {
			PositionDataImpl.onBlockChanged((LevelChunk) (Object) this, pos, state);
		}
	}
}
