package com.terraformersmc.terraform.wood.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingParticlesLeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FallingParticlesLeavesBlock.class)
@SuppressWarnings("unused")
public interface InvokerFallingParticlesLeavesBlock {
	@Invoker
	void callMakeFallingLeavesParticles(Level level, BlockPos pos, RandomSource random);
}
