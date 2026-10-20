package com.terraformersmc.terraform.dirt.api.block;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PathBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Custom dirt path block used to apply dirt path fixes to modded dirt path blocks.
 */
@SuppressWarnings("unused")
public class TerraformDirtPathBlock extends PathBlock {
	private final Block baseBlock;

	/**
	 * @param baseBlock The dirt block that this block turns back to when covered
	 */
	public TerraformDirtPathBlock(Block baseBlock, Properties properties) {
		super(baseBlock, properties);
		this.baseBlock = baseBlock;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// PathBlock falls back to vanilla dirt here; fall back to our own base block instead.
		return !this.defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
				? Block.pushEntitiesUp(this.defaultBlockState(), this.baseBlock.defaultBlockState(), context.getLevel(), context.getClickedPos())
				: super.getStateForPlacement(context);
	}
}
