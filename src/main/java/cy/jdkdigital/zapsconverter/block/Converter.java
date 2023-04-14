package cy.jdkdigital.zapsconverter.block;

import cy.jdkdigital.zapsconverter.Config;
import cy.jdkdigital.zapsconverter.ZapsConverter;
import cy.jdkdigital.zapsconverter.block.entity.ConverterBlockEntity;
import dev.ftb.mods.ftbic.FTBICConfig;
import dev.ftb.mods.ftbic.block.SprayPaintable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Converter extends BaseEntityBlock implements SprayPaintable
{
    public Converter(Properties pProperties) {
        super(pProperties);

        this.registerDefaultState(this.stateDefinition.any().setValue(DirectionalBlock.FACING, Direction.NORTH).setValue(SprayPaintable.DARK, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateBuilder) {
        stateBuilder.add(DirectionalBlock.FACING, SprayPaintable.DARK);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(DirectionalBlock.FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(DirectionalBlock.FACING, pRotation.rotate(pState.getValue(DirectionalBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(DirectionalBlock.FACING)));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, ZapsConverter.CONVERTER_BLOCK_ENTITY.get(), ConverterBlockEntity::serverTicker);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ConverterBlockEntity(pPos, pState);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState newState, boolean something) {
        BlockEntity generatorTile = level.getBlockEntity(pos);
        if (generatorTile instanceof ConverterBlockEntity generatorBlockEntity) {
            generatorBlockEntity.refreshConnectedTileEntityCache(state);
        }
        super.onPlace(state, level, pos, newState, something);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState newState, LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        BlockEntity generatorTile = level.getBlockEntity(pos);
        if (generatorTile instanceof ConverterBlockEntity generatorBlockEntity) {
            generatorBlockEntity.refreshConnectedTileEntityCache(state);
        }
        return super.updateShape(state, direction, newState, level, pos, facingPos);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable BlockGetter pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
        pTooltip.add(Component.translatable("zapsconverter.fe_to_zap_conversion", FTBICConfig.ENERGY_FORMAT, Config.SERVER.conversionRate.get()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
