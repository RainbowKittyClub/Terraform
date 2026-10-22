package com.terraformersmc.terraform.wood.mixin;

import com.terraformersmc.terraform.wood.api.block.SmallLogBlock;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockTransformer.class)
public class MixinBlockTransformer {
	// A leafy log is a leaves block on the outside, which no axe, shovel or hoe transforms; here, not in
	// useItemOn, so sneaking can't skip it and the off hand still gets its turn
	@Inject(method = "transformBlock", at = @At("HEAD"), cancellable = true)
	private void terraformWood$skipLeafyLogs(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		if (state.getBlock() instanceof SmallLogBlock && state.getValue(SmallLogBlock.HAS_LEAVES)) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}
}
