package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * Observer-only debug snapshot.
 *
 * <p>This type is intended for telemetry and tuning while the sensor-side code is being developed.
 * It is package-private on purpose so the rest of the robot cannot accidentally grow dependent on
 * raw sensor interpretation details.
 */
final class ObserverDebug {

    private final SurfaceKind currentSurface;
    private final boolean passActive;
    private final int activePassSlot;
    private final boolean seenArtifact;
    private final SlotContent latchedCandidateContent;

    /**
     * Creates a new observer debug snapshot.
     *
     * @param currentSurface coarse surface classification for the current sensor sample.
     * @param passActive whether a slot pass is currently active.
     * @param activePassSlot logical slot currently being observed, or {@code -1} when no pass is
     *                       active.
     * @param seenArtifact whether any artifact evidence has been seen during the current pass.
     * @param latchedCandidateContent best latched slot-content candidate so far for the active
     *                                pass.
     */
    ObserverDebug(
            SurfaceKind currentSurface,
            boolean passActive,
            int activePassSlot,
            boolean seenArtifact,
            SlotContent latchedCandidateContent) {
        this.currentSurface = currentSurface;
        this.passActive = passActive;
        this.activePassSlot = activePassSlot;
        this.seenArtifact = seenArtifact;
        this.latchedCandidateContent = latchedCandidateContent;
    }

    /**
     * Returns the current coarse surface classification.
     *
     * @return current surface kind.
     */
    SurfaceKind getCurrentSurface() {
        return currentSurface;
    }

    /**
     * Returns whether a slot pass is currently active.
     *
     * @return {@code true} during an active pass.
     */
    boolean isPassActive() {
        return passActive;
    }

    /**
     * Get current slot index
     * 
     * Returns the slot currently being observed.
     *
     * @return active pass slot index, or {@code -1} when no pass is active.
     */
    int getActivePassSlot() {
        return activePassSlot;
    }

    /**
     * Did it see an artifact?
     * 
     * Returns whether the observer has seen artifact evidence during the current pass.
     *
     * @return {@code true} when the observer has seen non-background evidence for the active pass.
     */
    boolean hasSeenArtifact() {
        return seenArtifact;
    }

    /**
     * Best possible slot to shoot from (closest to shooter & correct color)
     * 
     * Returns the best candidate content latched so far for the active pass.
     *
     * @return current candidate slot-content result.
     */
    SlotContent getLatchedCandidateContent() {
        return latchedCandidateContent;
    }
}
