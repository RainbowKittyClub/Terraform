package com.terraformersmc.terraform.dirt.api.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.terraformersmc.terraform.dirt.impl.registry.TillableBlockRegistryImpl;

/**
 * Allows the addition of custom tillable block mappings. You probably don't need to use this directly if you're using
 * {@link TerraformDirtRegistry}.
 */
@SuppressWarnings("unused")
public final class TillableBlockRegistry {
	@SuppressWarnings("UnnecessaryReturnStatement")
	private TillableBlockRegistry() {
		return;
	}

	/**
	 * Adds a custom tillable block mapping to the hoe's block transformer. The block is tilled only with air above it.
	 * <p/>
	 * The block must not be in {@code #minecraft:turns_into_farmland}, since vanilla's own hoe entry is checked first.
	 * <p/>
	 * Note that you don't need to call this yourself if you're already using {@link TerraformDirtRegistry}.
	 *
	 * @param block the block being tilled
	 * @param state the block to be replaced with
	 */
	public static void add(Block block, BlockState state) {
		TillableBlockRegistryImpl.add(block, state);
	}
}
