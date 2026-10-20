package com.terraformersmc.terraform.dirt.impl.registry;

import net.fabricmc.fabric.api.item.v1.BlockTransformerHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class TillableBlockRegistryImpl {
	@SuppressWarnings("UnnecessaryReturnStatement")
	private TillableBlockRegistryImpl() {
		return;
	}

	public static void add(Block block, BlockState state) {
		BlockTransformerHelper.registerTilling(block, state);
	}
}
