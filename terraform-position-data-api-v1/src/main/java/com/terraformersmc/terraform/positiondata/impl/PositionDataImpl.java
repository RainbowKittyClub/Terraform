package com.terraformersmc.terraform.positiondata.impl;

import com.terraformersmc.terraform.positiondata.api.PositionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PositionDataImpl {
	// An array, read on every block change, and replaced on the rare register
	private static volatile PositionData<?>[] kinds = new PositionData<?>[0];

	private PositionDataImpl() {
	}

	public static synchronized void register(PositionData<?> data) {
		PositionData<?>[] next = Arrays.copyOf(kinds, kinds.length + 1);
		next[kinds.length] = data;
		kinds = next;
	}

	/**
	 * Drops every value at pos unless both the old and the new state hold its kind, so a stale value never
	 * reaches a new holder; a block landing from a moving piston keeps the values placed for it.
	 */
	public static void onBlockChanged(LevelChunk chunk, BlockPos pos, BlockState old, BlockState state) {
		boolean landed = old.is(Blocks.MOVING_PISTON);
		for (PositionData<?> kind : kinds) {
			if (!kind.holds(state) || !landed && !kind.holds(old)) {
				kind.put(chunk, pos, null);
			}
		}
	}

	/** Removes the values of every block a piston is about to move, keyed by where each block lands. */
	public static Carried take(Level level, List<BlockPos> toPush, Direction direction) {
		Carried carried = new Carried();
		for (BlockPos pos : toPush) {
			LevelChunk chunk = level.getChunkAt(pos);
			for (PositionData<?> kind : kinds) {
				Object value = kind.getRaw(chunk, pos);
				if (value != null) {
					kind.put(chunk, pos, null);
					carried.values.computeIfAbsent(pos.relative(direction), target -> new HashMap<>()).put(kind, value);
				}
			}
		}

		return carried;
	}

	/** Values taken from moving blocks, waiting for their destinations. */
	public static final class Carried {
		private final Map<BlockPos, Map<PositionData<?>, Object>> values = new HashMap<>();

		/** Stores the values that land at pos, for each kind the moved block holds. */
		public void place(Level level, BlockPos pos, BlockState moved) {
			Map<PositionData<?>, Object> landing = this.values.remove(pos);
			if (landing == null) {
				return;
			}

			LevelChunk chunk = level.getChunkAt(pos);
			landing.forEach((kind, value) -> {
				if (kind.holds(moved)) {
					put(kind, chunk, pos, value);
				}
			});
		}

		@SuppressWarnings("unchecked")
		private static <T> void put(PositionData<T> kind, LevelChunk chunk, BlockPos pos, Object value) {
			kind.put(chunk, pos, (T) value);
		}
	}
}
