package dev.ftb.powerpots.pot;

import net.minecraftforge.energy.IEnergyStorage;

public class PotEnergyStorage implements IEnergyStorage {
    private int energy;

    private boolean energyChanged;
    private final int capacity;
    private final int maxInOut;
    private final PowerPotTileBase tile;

    public PotEnergyStorage(PowerPotTileBase tile, int capacity) {
        this.energy = 0;
        this.capacity = capacity;
        this.maxInOut = capacity;
        this.tile = tile;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int energyReceived = Math.min(this.maxInOut, maxReceive);
        int actuallyReceived;
        if (energyReceived >= (actuallyReceived = capacity - energy)) {
            if (!simulate) {
                energy += actuallyReceived;
            }
            return actuallyReceived;
        } else {
            if (!simulate) {
                energy += energyReceived;
                energyChanged = true;
                this.tile.sync(false);
            }
        }


        return energyReceived;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int consumeEnergy(int maxExtract, boolean simulate) {
        int energyExtracted = Math.min(energy, Math.min(this.maxInOut, maxExtract));

        if (!simulate) {
            energy -= energyExtracted;
            energyChanged = true;
        }

        return energyExtracted;
    }

    // We don't use this method and thus we don't let other people use it either
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    public boolean getEnergyChanged() {
        return this.energyChanged;
    }

    public void setEnergyChanged(boolean changed) {
        this.energyChanged = changed;
    }

    @Override
    public int getEnergyStored() {
        return this.energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return this.capacity;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public String toString() {
        return "ChargerEnergyStorage{" +
                "energy=" + energy +
                ", capacity=" + capacity +
                ", maxInOut=" + maxInOut +
                '}';
    }
}
