package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

import java.util.Arrays;

/**
 * Immutable snapshot of the sorter's externally relevant state.
 *
 * <p>This is the main read-only object the rest of the robot should use to inspect sorter state.
 * It intentionally contains committed slot beliefs and station mapping, but not raw sensor
 * internals.
 */
public final class SorterStatus {

    private final CalibrationState calibrationState;
    private final SorterAction action;
    private final SlotContent[] slots;
    private final int loadSlot;
    private final int shootSlot;
    private final boolean loadAligned;
    private final boolean shootAligned;

    /**
     * Creates a new immutable status snapshot.
     *
     * @param calibrationState current calibration progress.
     * @param action current high-level sorter action.
     * @param slots committed slot beliefs. The expected array length is three.
     * @param loadSlot logical slot currently mapped to the load station, or {@code -1} when
     *                 no trustworthy mapping exists yet.
     * @param shootSlot logical slot currently mapped to the shoot station, or {@code -1} when
     *                  no trustworthy mapping exists yet.
     * @param loadAligned whether the slot at the load station is centered closely enough to be
     *                    treated as aligned for load-side logic.
     * @param shootAligned whether the slot at the shoot station is centered closely enough to be
     *                     treated as aligned for shooter presentation.
     */
    public SorterStatus(
            CalibrationState calibrationState,
            SorterAction action,
            SlotContent[] slots,
            int loadSlot,
            int shootSlot,
            boolean loadAligned,
            boolean shootAligned) {
        this.calibrationState = calibrationState;
        this.action = action;
        this.slots = slots == null ? new SlotContent[0] : Arrays.copyOf(slots, slots.length);
        this.loadSlot = loadSlot;
        this.shootSlot = shootSlot;
        this.loadAligned = loadAligned;
        this.shootAligned = shootAligned;
    }

    /**
     * Returns the current calibration progress.
     *
     * @return current calibration state.
     */
    public CalibrationState getCalibrationState() {
        return calibrationState;
    }

    /**
     * Returns the sorter action currently in progress.
     *
     * @return current action.
     */
    public SorterAction getAction() {
        return action;
    }

    /**
     * Returns a defensive copy of committed slot beliefs.
     *
     * @return copied slot-content array.
     */
    public SlotContent[] getSlots() {
        return Arrays.copyOf(slots, slots.length);
    }

    /**
     * Returns the logical slot currently mapped to the load station.
     *
     * @return slot index at the load station, or {@code -1} when calibration is not yet trusted.
     */
    public int getLoadSlot() {
        return loadSlot;
    }

    /**
     * Returns the logical slot currently mapped to the shooter station.
     *
     * @return slot index at the shooter station, or {@code -1} when calibration is not yet
     *         trusted.
     */
    public int getShootSlot() {
        return shootSlot;
    }

    /**
     * Returns whether the load station is aligned closely enough to perform load-side work.
     *
     * @return {@code true} when the current load slot is centered at the load station.
     */
    public boolean isLoadAligned() {
        return loadAligned;
    }

    /**
     * Returns whether the shoot station is aligned closely enough to present to the shooter.
     *
     * @return {@code true} when the current shoot slot is centered at the shooter station.
     */
    public boolean isShootAligned() {
        return shootAligned;
    }

    /**
     * Returns whether every slot is known well enough for strategy decisions.
     *
     * <p>This returns {@code false} when any slot is {@link SlotContent#UNKNOWN} or
     * {@link SlotContent#OCCUPIED_UNKNOWN_COLOR}.
     *
     * @return {@code true} when every slot is known-empty or known-color occupied.
     */
    public boolean isInventoryFullyKnown() {
        for (SlotContent slot : slots) {
            if (slot == SlotContent.UNKNOWN || slot == SlotContent.OCCUPIED_UNKNOWN_COLOR) {
                return false;
            }
        }
        return true;
    }
}
