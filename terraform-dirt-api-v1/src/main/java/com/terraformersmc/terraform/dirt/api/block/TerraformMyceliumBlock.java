package com.terraformersmc.terraform.dirt.api.block;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MyceliumBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * A custom mycelium block that allows one to define their own soil types.
 */
@SuppressWarnings("unused")
public class TerraformMyceliumBlock extends MyceliumBlock {
	private final Block dirt;
	private final Map<Block, Block> spreadsTo;
	/**
	 * Maps dirt blocks to the mycelium they turn into when <em>vanilla</em> mycelium spreads to them.
	 */
	public static final Map<Block, Block> MYCELIUM_SPREADS_TO = new HashMap<>();

	public TerraformMyceliumBlock(Block dirt, BlockBehaviour.Properties properties) {
		this(dirt, properties, ImmutableMap.of(Blocks.DIRT, Blocks.MYCELIUM));
	}

	/**
	 * @param dirt      The dirt block that this block turns back to when it loses its mycelium
	 * @param spreadsTo Maps dirt blocks to the mycelium they turn into when this mycelium spreads to them.
	 */
	public TerraformMyceliumBlock(Block dirt, BlockBehaviour.Properties properties, Map<Block, Block> spreadsTo) {
		this(dirt, properties, spreadsTo, true);
	}

	/**
	 * @param dirt              The dirt block that this block turns back to when it loses its mycelium
	 * @param spreadsTo         Maps dirt blocks to the mycelium they turn into when this mycelium spreads to them.
	 * @param myceliumSpreadsTo If true, vanilla mycelium will spread to the block specified in the 'dirt' parameter,
	 *                          turning into this block
	 */
	public TerraformMyceliumBlock(Block dirt, BlockBehaviour.Properties properties, Map<Block, Block> spreadsTo, boolean myceliumSpreadsTo) {
		super(properties);

		this.dirt = dirt;
		this.spreadsTo = spreadsTo;
		if (myceliumSpreadsTo) {
			MYCELIUM_SPREADS_TO.put(dirt, this);
		}
	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!TerraformGrassBlock.canBeGrass(state, level, pos)) {
			level.setBlockAndUpdate(pos, dirt.defaultBlockState());
		} else if (level.getBrightness(LightLayer.SKY, pos.above()) >= 9) {
			BlockState defaultState = this.defaultBlockState();

			for (int i = 0; i < 4; ++i) {
				BlockPos spreadingPos = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);

				Block spreadTarget = level.getBlockState(spreadingPos).getBlock();
				Block spreadBlock = spreadTarget == dirt ? this : spreadsTo.get(spreadTarget);
				if (spreadBlock != null && TerraformGrassBlock.canPropagate(defaultState, level, spreadingPos)) {
					BlockState spreadState = spreadBlock.defaultBlockState();
					if (spreadBlock instanceof SnowyBlock) {
						spreadState = spreadState.setValue(SNOWY, level.getBlockState(spreadingPos.above()).getBlock() == Blocks.SNOW);
					}
					level.setBlockAndUpdate(spreadingPos, spreadState);
				}
			}
		}
	}
}
