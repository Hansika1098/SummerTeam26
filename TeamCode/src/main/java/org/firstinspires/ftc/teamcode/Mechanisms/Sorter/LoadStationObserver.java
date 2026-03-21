package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * Internal contract for the load-station sensor subsystem.
 *
 * <p>This interface is intentionally narrower than a generic "dual color sensor wrapper". It owns
 * the real sensor-side behaviors that matter to the sorter project:
 *
 * <ul>
 *   <li>separator detection during calibration,
 *   <li>dual-sensor fusion,
 *   <li>artifact-vs-background detection,
 *   <li>color latching across a slot pass,
 *   <li>building one final {@link SlotReadResult} when a pass ends.
 * </ul>
 *
 * <p>The rest of the robot should not depend on this interface directly. It is a collaboration
 * seam between the sorter implementation and the sensor-side observation logic.
 */
interface LoadStationObserver {

    /**
     * Resets all observer-owned transient state.
     *
     * <p>Typical responsibilities include clearing latched color, clearing any active pass, and
     * resetting separator-detection history.
     */
    void reset();

    /**
     * Does it see a seperator?
     * 
     * Samples the sensors for calibration/reference finding.
     *
     * <p>This method is used while the spinner slowly rotates in search of a separator transition
     * that can serve as a trusted angular reference.
     *
     * @return current calibration/reference signal derived from the sensors.
     */
    ReferenceDetection sampleForCalibration();

    /**
     * New slot!!
     * 
     * Starts a new slot pass for the given logical slot.
     *
     * <p>The sorter decides when a slot has entered the coarse read zone based on spinner geometry.
     * The observer should clear any previous latches and begin gathering evidence for this slot.
     *
     * @param slotIndex logical slot index currently entering the read zone.
     */
    void beginSlotPass(int slotIndex);

    /**
     * Read color :)
     * 
     * Samples sensors during an active slot pass.
     *
     * <p>The observer should retain state between calls. Repeated calls during the same pass are
     * expected and should be used to latch presence and color information over time.
     */
    void sampleSlotPass();

    /**
     * Stop reading color & return result
     * 
     * Ends the active slot pass and returns one committed result for the slot.
     *
     * <p>The returned result should summarize the observer's best belief for that pass. The sorter
     * then decides how to store that result in its authoritative slot table.
     *
     * @return final result for the active slot pass.
     */
    SlotReadResult endSlotPass();

    /**
     * Return an object with a bunch of details (for testing)
     * 
     * Returns observer-specific debug information for tuning.
     *
     * @return debug snapshot of the observer's current internal sensing state.
     */
    ObserverDebug getDebug();
}
