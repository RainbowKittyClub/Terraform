package com.terraformersmc.terraform.wood.mixin;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LeavesBlock.class)
@SuppressWarnings("unused")
public interface AccessorLeavesBlock {
	@Accessor
	AmbientLeavesBlockSoundPlayer getAmbientLeavesBlockSoundPlayer();
}
