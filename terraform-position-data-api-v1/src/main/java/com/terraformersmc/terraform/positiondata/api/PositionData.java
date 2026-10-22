package com.terraformersmc.terraform.positiondata.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.terraformersmc.terraform.positiondata.impl.PositionDataImpl;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * One kind of data kept for single block positions, such as which leaves a small log carries.
 *
 * <p>Each kind is one persistent chunk attachment, so a value is saved and unloaded with its chunk, and
 * positions without a value cost nothing. A value lives only while the block at its position holds it:
 * changing that block to one this kind doesn't hold drops the value, and a piston carries it along with
 * its block. Values never reach clients.
 */
public final class PositionData<T> {
	private final Identifier id;
	private final Predicate<BlockState> holds;
	private final AttachmentType<Map<Integer, T>> type;

	private PositionData(Identifier id, Codec<T> codec, Predicate<BlockState> holds) {
		this.id = id;
		this.holds = holds;
		this.type = AttachmentRegistry.create(id, builder -> builder.persistent(mapCodec(id, codec)));
	}

	/**
	 * Registers a kind of position data. Call it during mod initialization, before any world loads.
	 *
	 * @param id the kind's id, also the id of its chunk attachment
	 * @param codec saves one value; a value that fails to load is dropped on its own
	 * @param holds the block states that keep a value at their position
	 */
	public static <T> PositionData<T> register(Identifier id, Codec<T> codec, Predicate<BlockState> holds) {
		PositionData<T> data = new PositionData<>(id, codec, holds);
		PositionDataImpl.register(data);
		return data;
	}

	public Identifier id() {
		return this.id;
	}

	/** Whether a block in this state keeps a value of this kind at its position. */
	public boolean holds(BlockState state) {
		return this.holds.test(state);
	}

	/**
	 * The value at this position, or null when there is none, its chunk isn't loaded, or the block there
	 * doesn't hold it. Never loads a chunk; off a server's main thread it always answers null.
	 */
	public @Nullable T get(BlockGetter level, BlockPos pos) {
		ChunkAccess chunk = chunkAt(level, pos);
		if (chunk == null) {
			return null;
		}

		T value = this.getRaw(chunk, pos);
		return value != null && this.holds.test(chunk.getBlockState(pos)) ? value : null;
	}

	/**
	 * Sets the value at this position, or removes it when the value is null. Loads the chunk if needed.
	 * Does nothing on a client, and stores nothing where the block doesn't hold this kind.
	 */
	public void set(Level level, BlockPos pos, @Nullable T value) {
		if (!level.isClientSide()) {
			ChunkAccess chunk = level.getChunkAt(pos);
			if (value == null || this.holds.test(chunk.getBlockState(pos))) {
				this.put(chunk, pos, value);
			}
		}
	}

	/** The stored value at this position, whether or not the block there holds it. */
	@ApiStatus.Internal
	public @Nullable T getRaw(ChunkAccess chunk, BlockPos pos) {
		Map<Integer, T> values = chunk.getAttached(this.type);
		return values == null ? null : values.get(pack(pos));
	}

	/** Stores or, for null, removes the value at this position. Values are replaced, never edited. */
	@ApiStatus.Internal
	public void put(ChunkAccess chunk, BlockPos pos, @Nullable T value) {
		Map<Integer, T> old = chunk.getAttached(this.type);
		int key = pack(pos);

		if (value == null) {
			if (old == null || !old.containsKey(key)) {
				return;
			}

			if (old.size() == 1) {
				chunk.removeAttached(this.type);
				return;
			}
		} else if (old != null && value.equals(old.get(key))) {
			return;
		}

		Map<Integer, T> next = old == null ? new HashMap<>() : new HashMap<>(old);
		if (value == null) {
			next.remove(key);
		} else {
			next.put(key, value);
		}

		chunk.setAttached(this.type, Map.copyOf(next));
	}

	// The position inside its chunk: x and z in the low byte, y above, sign kept by the shift
	private static int pack(BlockPos pos) {
		return (pos.getY() << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
	}

	private static @Nullable ChunkAccess chunkAt(BlockGetter level, BlockPos pos) {
		int x = SectionPos.blockToSectionCoord(pos.getX());
		int z = SectionPos.blockToSectionCoord(pos.getZ());

		if (level instanceof ChunkAccess chunk) {
			return chunk.getPos().x() == x && chunk.getPos().z() == z ? chunk : null;
		}

		if (level instanceof Level fullLevel) {
			return fullLevel.getChunkSource().getChunkNow(x, z);
		}

		if (level instanceof LevelReader reader) {
			return reader.getChunk(x, z, ChunkStatus.EMPTY, false);
		}

		return null;
	}

	private static <T> Codec<Map<Integer, T>> mapCodec(Identifier id, Codec<T> valueCodec) {
		Codec<Entry<T>> entry = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("pos").forGetter(Entry::pos),
				valueCodec.lenientOptionalFieldOf("value").forGetter(Entry::value)
		).apply(instance, Entry::new));

		return entry.listOf().flatXmap(entries -> toMap(id, entries), values -> DataResult.success(toEntries(values)));
	}

	// With no value left the attachment is skipped, so the chunk stops saving an empty one
	private static <T> DataResult<Map<Integer, T>> toMap(Identifier id, List<Entry<T>> entries) {
		Map<Integer, T> values = new HashMap<>();
		for (Entry<T> entry : entries) {
			entry.value().ifPresent(value -> values.put(entry.pos(), value));
		}

		return values.isEmpty() ? DataResult.error(() -> "No value of " + id + " left to load") : DataResult.success(Map.copyOf(values));
	}

	// Sorted by position, so the same values always save the same bytes
	private static <T> List<Entry<T>> toEntries(Map<Integer, T> values) {
		return values.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> new Entry<>(entry.getKey(), Optional.of(entry.getValue())))
				.toList();
	}

	private record Entry<T>(int pos, Optional<T> value) {
	}
}
