package cy.jdkdigital.zapsconverter.block.entity;

import cy.jdkdigital.zapsconverter.Config;
import cy.jdkdigital.zapsconverter.ZapsConverter;
import dev.ftb.mods.ftbic.util.EnergyHandler;
import dev.ftb.mods.ftbic.util.FTBICUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConverterBlockEntity extends BlockEntity
{
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> new EnergyStorage(100000));
    private List<EnergyHandler> recipients;
    private boolean hasLoaded = false;

    public ConverterBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ZapsConverter.CONVERTER_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    public static void serverTicker(Level level, BlockPos blockPos, BlockState blockState, ConverterBlockEntity blockEntity) {
        if (!blockEntity.hasLoaded) {
            blockEntity.refreshConnectedTileEntityCache(blockState);
            blockEntity.hasLoaded = true;
        }

        // Output to adjacent receivers
        blockEntity.energyHandler.ifPresent(h -> {
            int drainedPower = blockEntity.outputPower(h.getEnergyStored() / Config.SERVER.conversionRate.get());
            h.extractEnergy(drainedPower * Config.SERVER.conversionRate.get(), false);
        });
    }

    private int outputPower(int energy) {
        if (energy <= 0) {
            return 0;
        }

        int drainedZaps = 0;
        if (this.recipients.size() > 0) {
            for (EnergyHandler storage : this.recipients) {
                if (storage.isEnergyHandlerInvalid() || storage.getEnergy() == storage.getEnergyCapacity()) {
                    continue;
                }

                double a = storage.insertEnergy(Math.min(storage.getMaxInputEnergy(), energy), false);
                if (a > 0D) {
                    drainedZaps += a;
                    energy -= a;
                    setChanged();
                }

                if (energy == 0) {
                    break;
                }
            }
        }
        return drainedZaps;
    }

    public void refreshConnectedTileEntityCache(BlockState state) {
        if (level instanceof ServerLevel) {
            List<EnergyHandler> recipients = new ArrayList<>();
            for (Direction direction : FTBICUtils.DIRECTIONS) {
                if (!direction.equals(state.getValue(DirectionalBlock.FACING))) {
                    BlockEntity entity = level.getBlockEntity(worldPosition.relative(direction));
                    if (entity != null) {
                        EnergyHandler handler = entity instanceof EnergyHandler ? (EnergyHandler) entity : null;
                        if (handler != null && handler.getMaxInputEnergy() > 0D && !handler.isBurnt() && handler.isValidEnergyInputSide(direction.getOpposite())) {
                            recipients.add(handler);
                        }
                    }
                }
            }
            this.recipients = recipients;
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side != null && side.equals(getBlockState().getValue(DirectionalBlock.FACING))) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("energy")) {
            getCapability(ForgeCapabilities.ENERGY).ifPresent(handler -> {
                ((EnergyStorage) handler).deserializeNBT(tag.get("energy"));
            });
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        getCapability(ForgeCapabilities.ENERGY).ifPresent(handler -> {
            tag.put("energy", ((EnergyStorage) handler).serializeNBT());
        });
    }
}
