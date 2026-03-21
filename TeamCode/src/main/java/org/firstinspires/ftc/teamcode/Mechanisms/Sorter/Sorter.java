package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * Public contract for the sorter subsystem.
 *
 * <p>The rest of the robot should talk to the sorter through this interface rather than depending
 * on sensor details or spinner geometry internals. This keeps the sensor-side work and the
 * motion/state-machine work decoupled.
 *
 * <p>All command methods are non-blocking. Calling code should issue a command, then continue
 * calling {@link #update()} every loop until {@link #getStatus()} shows the sorter has returned to
 * {@link SorterAction#IDLE} or the desired station/readiness condition has become true.
 */
public interface Sorter {

    /**
     * Advances the sorter state machine by one loop iteration.
     *
     * <p>This method should be called exactly once per robot loop while the sorter is in use.
     * Typical responsibilities include:
     *
     * <ul>
     *   <li>updating calibration logic,
     *   <li>updating motion and action state,
     *   <li>running the load-station observer while a slot is in the read zone,
     *   <li>committing completed slot-pass results into inventory.
     * </ul>
     */
    void update();

    /**
     * Starts the calibration sequence.
     *
     * <p>Calibration is the process of finding a trusted angular reference for the spinner by
     * using sensor-visible separator transitions. The command is non-blocking: it starts the
     * process and returns immediately.
     *
     * @return {@link CommandResult#ACCEPTED} if calibration was started, otherwise a rejection
     *         reason such as {@link CommandResult#REJECTED_BUSY}.
     */
    CommandResult startCalibration();

    /**
     * Returns whether the sorter currently trusts its spinner geometry.
     *
     * @return {@code true} once the spinner reference has been found and slot mapping is usable.
     */
    boolean isCalibrated();

    /**
     * Returns a read-only snapshot of the sorter's externally relevant state.
     *
     * @return immutable sorter status snapshot.
     */
    SorterStatus getStatus();

    /**
     * Returns the committed belief for one slot.
     *
     * @param slotIndex logical slot index from {@code 0} to {@code 2}.
     * @return current slot belief.
     * @throws IllegalArgumentException if {@code slotIndex} is out of range.
     */
    SlotContent getSlotContent(int slotIndex);

    /**
     * Returns which logical slot is currently mapped to the requested station.
     *
     * @param station station of interest.
     * @return slot index at that station, or {@code -1} when calibration is not trusted.
     */
    int getSlotAt(Station station);

    /**
     * Returns whether the sorter is mechanically ready to accept a load at the load station.
     *
     * <p>This is a mechanical/readiness question, not a strategy question. It is true only when:
     *
     * <ul>
     *   <li>the sorter is calibrated,
     *   <li>the sorter is idle,
     *   <li>a slot is aligned at the load station,
     *   <li>that slot is believed to be empty.
     * </ul>
     *
     * @return {@code true} when a known-empty slot is aligned and ready for loading.
     */
    boolean canLoad();

    /**
     * Returns whether the sorter is mechanically ready to feed the currently presented shoot slot.
     *
     * <p>This is intentionally about physical readiness rather than motif correctness. If the shoot
     * slot is occupied but color-unknown, this method may still return {@code true}; strategy code
     * should inspect {@link #getSlotContent(int)} when color matters.
     *
     * @return {@code true} when a slot is aligned at the shooter and is believed occupied.
     */
    boolean canFeed();

    /**
     * Starts an inventory scan of all slots.
     *
     * <p>This command is important for DECODE because robots may begin a match with preloaded
     * artifacts, and because manual intervention or a jam can leave inventory beliefs uncertain.
     * The expected implementation is to rotate each slot through the load-side observer and rebuild
     * the slot table.
     *
     * @return {@link CommandResult#ACCEPTED} if scanning started, otherwise a rejection reason.
     */
    CommandResult startInventoryScan();

    /**
     * Starts the process of positioning a known-empty slot at the load station.
     *
     * @return {@link CommandResult#ACCEPTED} if the request was accepted; otherwise a rejection
     *         reason such as {@link CommandResult#REJECTED_NO_EMPTY_SLOT}.
     */
    CommandResult tryPrepareLoad();

    /**
     * Starts the process of presenting one chosen slot to the shooter station.
     *
     * <p>The sorter does not decide which color should be shot next. That policy stays outside the
     * sorter so game-specific sequencing can live in a higher-level mechanism or autonomous
     * planner.
     *
     * @param slotIndex logical slot index from {@code 0} to {@code 2}.
     * @return {@link CommandResult#ACCEPTED} if the request was accepted; otherwise a rejection
     *         reason.
     */
    CommandResult tryPresentSlot(int slotIndex);

    /**
     * Starts one non-blocking feed cycle for the slot currently presented to the shooter.
     *
     * @return {@link CommandResult#ACCEPTED} if the feed cycle was started; otherwise a rejection
     *         reason.
     */
    CommandResult tryFeed();

    /**
     * Confirms that the currently presented shoot-slot artifact has actually left the spinner.
     *
     * <p>This method lets the sorter update its committed inventory only after another part of the
     * robot has evidence that the artifact truly exited. That avoids double-clearing a slot when a
     * feed attempt fails or partially advances.
     */
    void confirmShotComplete();

    /**
     * Marks all slot beliefs as unknown without moving hardware.
     *
     * <p>Use this after manual intervention, a major jam, or any other event that makes the sorter
     * distrust its current slot table but does not guarantee the spinner is physically empty.
     */
    void markInventoryUnknown();

    /**
     * Marks all slots as empty without moving hardware.
     *
     * <p>Use this only when the team knows the spinner has physically been emptied by hand or by a
     * separate recovery action. This is stronger than {@link #markInventoryUnknown()} and should
     * not be used casually.
     */
    void clearInventory();
}
