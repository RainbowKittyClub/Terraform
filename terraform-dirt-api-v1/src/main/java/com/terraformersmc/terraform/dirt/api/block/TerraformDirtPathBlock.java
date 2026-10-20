package com.terraformersmc.terraform.dirt.api.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PathBlock;

/**
 * Custom dirt path block used to apply dirt path fixes to modded dirt path blocks.
 */
@SuppressWarnings("unused")
public class TerraformDirtPathBlock extends PathBlock {
	/**
	 * @param baseBlock The dirt block that this block turns back to when covered
	 */
	public TerraformDirtPathBlock(Block baseBlock, Properties properties) {
		super(baseBlock, properties);
	}
}
