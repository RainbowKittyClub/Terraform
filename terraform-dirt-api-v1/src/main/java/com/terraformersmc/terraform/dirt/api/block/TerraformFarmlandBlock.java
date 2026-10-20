package com.terraformersmc.terraform.dirt.api.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A custom farm block for new farmland.
 * Mixins are required to allow seeds to be planted.
 */
@SuppressWarnings("unused")
public class TerraformFarmlandBlock extends FarmlandBlock {
	/**
	 * @param baseBlock The dirt block that this block turns back to when trampled or dried out
	 */
	public TerraformFarmlandBlock(Block baseBlock, BlockBehaviour.Properties properties) {
		super(baseBlock, properties);
	}
}
