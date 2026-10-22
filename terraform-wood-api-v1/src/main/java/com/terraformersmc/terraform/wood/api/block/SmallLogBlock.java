package com.terraformersmc.terraform.wood.api.block;

import com.terraformersmc.terraform.positiondata.api.PositionData;
import com.terraformersmc.terraform.wood.mixin.AccessorLeavesBlock;
import com.terraformersmc.terraform.wood.mixin.InvokerFallingParticlesLeavesBlock;
import com.terraformersmc.terraform.wood.mixin.InvokerLeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingParticlesLeavesBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Rather complex: combine the function of leaves, logs, and cobblestone walls.

/**
 * A very complex smaller log block that can connect on all 6 axes, can be waterlogged, and can have leaves embedded.
 * Used for things like the Sakura tree.
 */
@SuppressWarnings("unused")
public class SmallLogBlock extends BareSmallLogBlock {
	public static final BooleanProperty HAS_LEAVES = BooleanProperty.create("has_leaves");

	/**
	 * The leaves a leafy small log carries when they aren't its own, for example birch leaves on a sakura log.
	 * They stay while the block remains a leafy small log; a piston breaks them off before it moves the log.
	 */
	public static final PositionData<Block> LEAVES = PositionData.register(
			Identifier.fromNamespaceAndPath("terraform", "small_log_leaves"),
			BuiltInRegistries.BLOCK.byNameCodec(),
			state -> state.getBlock() instanceof SmallLogBlock && state.getValue(HAS_LEAVES)
	);

	private final Block leaves;

	public SmallLogBlock(Block leaves, Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(AXIS, Direction.Axis.Y)
				.setValue(UP, false)
				.setValue(DOWN, false)
				.setValue(WEST, false)
				.setValue(EAST, false)
				.setValue(NORTH, false)
				.setValue(SOUTH, false)
				.setValue(WATERLOGGED, false)
				.setValue(HAS_LEAVES, false)
		);

		this.leaves = leaves;
	}

	/** This log's own leaves, the ones it grows with. */
	public Block getLeaves() {
		return this.leaves;
	}

	/** The leaves a leafy log at this position carries: the stored ones, else its own. */
	public Block leavesAt(BlockGetter level, BlockPos pos) {
		Block stored = LEAVES.get(level, pos);
		return stored != null ? stored : this.leaves;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (level.isClientSide() && state.getValue(HAS_LEAVES)) {
			/*
			 * Below here this is a copy of LeavesBlock.animateTick() and FallingParticlesLeavesBlock.animateTick() from vanilla.
			 * This is because some other mods crash when we call the vanilla method on a non-LeavesBlock.
			 * To merge changes, you should typically be able to just replace with a copy from vanilla and
			 * fix up any access issues with invokers or access wideners.
			 */
			super.animateTick(state, level, pos, random);

			BlockPos below = pos.below();
			BlockState belowState = level.getBlockState(below);

			InvokerLeavesBlock.callMakeDrippingWaterParticles(level, pos, random, belowState, below);
			// Only some leaves drop falling leaf particles (vanilla spruce leaves do not).
			if (this.leaves instanceof FallingParticlesLeavesBlock) {
				//noinspection ReferenceToMixin
				((InvokerFallingParticlesLeavesBlock) this.leaves).callMakeFallingLeavesParticles(level, pos, random);
			}
			// Only some leaves have an ambient sound (vanilla poplar leaves do).
			if (this.leaves instanceof LeavesBlock) {
				//noinspection ReferenceToMixin
				((AccessorLeavesBlock) this.leaves).getAmbientLeavesBlockSoundPlayer().playAmbientLeavesSounds(level, pos, this.leaves, random);
			}
		}
	}

	@Override
	public InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {

		// Any leaves leaf a bare log; ones other than its own are stored for the position
		Block used = held.getItem() instanceof BlockItem item ? item.getBlock() : Blocks.AIR;
		if (held.getCount() >= 1 && used.defaultBlockState().is(BlockTags.LEAVES) && !state.getValue(HAS_LEAVES)) {
			if (!player.isCreative()) {
				held.shrink(1);
			}

			SoundType sounds = used.defaultBlockState().getSoundType();
			level.playSound(player, pos, sounds.getPlaceSound(), SoundSource.BLOCKS, (sounds.getVolume() + 1.0F) / 2.0F, sounds.getPitch() * 0.8F);

			BlockState previous = state;

			state = state.setValue(HAS_LEAVES, true);

			if (state.getValue(UP) && level.getBlockState(pos.above()).getBlock() instanceof LeavesBlock) {
				state = state.setValue(UP, false);
			}

			if (state.getValue(DOWN) && level.getBlockState(pos.below()).getBlock() instanceof LeavesBlock) {
				state = state.setValue(DOWN, false);
			}

			if (state.getValue(WEST) && level.getBlockState(pos.west()).getBlock() instanceof LeavesBlock) {
				state = state.setValue(WEST, false);
			}

			if (state.getValue(EAST) && level.getBlockState(pos.east()).getBlock() instanceof LeavesBlock) {
				state = state.setValue(EAST, false);
			}

			if (state.getValue(NORTH) && level.getBlockState(pos.north()).getBlock() instanceof LeavesBlock) {
				state = state.setValue(NORTH, false);
			}

			if (state.getValue(SOUTH) && level.getBlockState(pos.south()).getBlock() instanceof LeavesBlock) {
				state = state.setValue(SOUTH, false);
			}

			level.setBlockAndUpdate(pos, pushEntitiesUp(previous, state, level, pos));
			LEAVES.set(level, pos, used == this.leaves ? null : used);

			return InteractionResult.SUCCESS;
		}

		return super.useItemOn(held, state, level, pos, player, hand, hitResult);
	}

	// Picking a leafy log picks its leaves, as for any leaves block
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return state.getValue(HAS_LEAVES) ? new ItemStack(this.leavesAt(level, pos)) : super.getCloneItemStack(level, pos, state, includeData);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);

		builder.add(HAS_LEAVES);
	}

	protected boolean shouldConnectTo(BlockState state, boolean solid, boolean leaves) {
		return solid || (!leaves && state.getBlock() instanceof LeavesBlock) || state.is(this);
	}

	@Override
	public BlockState getNeighborUpdateState(BlockState state, Direction fromDirection, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		boolean leaves = state.getBlock() instanceof SmallLogBlock ? state.getValue(HAS_LEAVES) : false;

		boolean up = fromDirection == Direction.UP && this.shouldConnectTo(neighbor, neighbor.isFaceSturdy(level, neighborPos, Direction.DOWN), leaves) || state.getValue(UP);
		boolean down = fromDirection == Direction.DOWN && this.shouldConnectTo(neighbor, neighbor.isFaceSturdy(level, neighborPos, Direction.UP), leaves) || state.getValue(DOWN);
		boolean north = fromDirection == Direction.NORTH && this.shouldConnectTo(neighbor, neighbor.isFaceSturdy(level, neighborPos, Direction.SOUTH), leaves) || state.getValue(NORTH);
		boolean east = fromDirection == Direction.EAST && this.shouldConnectTo(neighbor, neighbor.isFaceSturdy(level, neighborPos, Direction.WEST), leaves) || state.getValue(EAST);
		boolean south = fromDirection == Direction.SOUTH && this.shouldConnectTo(neighbor, neighbor.isFaceSturdy(level, neighborPos, Direction.NORTH), leaves) || state.getValue(SOUTH);
		boolean west = fromDirection == Direction.WEST && this.shouldConnectTo(neighbor, neighbor.isFaceSturdy(level, neighborPos, Direction.EAST), leaves) || state.getValue(WEST);

		return state
				.setValue(UP, up)
				.setValue(DOWN, down)
				.setValue(NORTH, north)
				.setValue(EAST, east)
				.setValue(SOUTH, south)
				.setValue(WEST, west);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HAS_LEAVES) ? Shapes.block() : this.boundingShapes[this.getShapeIndex(state)];
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HAS_LEAVES) ? Shapes.block() : this.collisionShapes[this.getShapeIndex(state)];
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state) {
		return this.collisionShapes[this.getShapeIndex(state)];
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return this.collisionShapes[this.getShapeIndex(state)];
	}
}
