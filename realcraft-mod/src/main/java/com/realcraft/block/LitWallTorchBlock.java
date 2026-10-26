package com.realcraft.block;

import com.realcraft.RealCraftMod;
import com.realcraft.config.RealCraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 点燃的火把（壁挂式）：贴墙、有燃料，熄灭时保持朝向。
 */
public class LitWallTorchBlock extends Block {
    public static final DirectionProperty FACING =
            DirectionProperty.create("facing", d -> d.getAxis().isHorizontal());

    public LitWallTorchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public static VoxelShape shape(Direction dir) {
        return switch (dir) {
            case NORTH -> Block.box(5.5D, 3.0D, 11.0D, 10.5D, 13.0D, 16.0D);
            case SOUTH -> Block.box(5.5D, 3.0D, 0.0D, 10.5D, 13.0D, 5.0D);
            case WEST -> Block.box(11.0D, 3.0D, 5.5D, 16.0D, 13.0D, 10.5D);
            case EAST -> Block.box(0.0D, 3.0D, 5.5D, 5.0D, 13.0D, 10.5D);
            default -> Block.box(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D);
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction dir = state.getValue(FACING);
        return canSupportCenter(level, pos.relative(dir.getOpposite()), dir);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            int fuel = RealCraftConfig.get().torchFuelTicks + level.random.nextInt(1200);
            level.scheduleTick(pos, this, fuel);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (RealCraftConfig.get().enableTorchBurnout) {
            this.extinguish(level, pos, state);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (RealCraftConfig.get().enableRainExtinguish && level.isRainingAt(pos)) {
            this.extinguish(level, pos, state);
        }
    }

    protected void extinguish(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, RealCraftMod.UNLIT_WALL_TORCH.defaultBlockState()
                .setValue(UnlitWallTorchBlock.FACING, state.getValue(FACING)), 3);
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.2F);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D, 10, 0.1D, 0.1D, 0.1D, 0.01D);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction dir = state.getValue(FACING);
        double x = pos.getX() + 0.5D - dir.getStepX() * 0.22D;
        double y = pos.getY() + 0.72D;
        double z = pos.getZ() + 0.5D - dir.getStepZ() * 0.22D;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
