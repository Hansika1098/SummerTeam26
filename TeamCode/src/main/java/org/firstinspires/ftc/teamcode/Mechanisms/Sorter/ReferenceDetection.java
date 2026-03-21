package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * Is seperator detected?
 *
 * Result of one calibration/reference sample from {@link LoadStationObserver}.
 *
 * <p>This object is intentionally small. The sorter mainly needs to know whether the observer has
 * seen the separator transition or separator state strongly enough to lock a geometric reference.
 */
final class ReferenceDetection {

    private final boolean separatorDetected;

    /**
     * Creates a new calibration/reference sample result.
     *
     * @param separatorDetected whether the observer believes the separator reference should be
     *                          treated as detected on this sample.
     */
    ReferenceDetection(boolean separatorDetected) {
        this.separatorDetected = separatorDetected;
    }

    /**
     * Returns whether the separator reference was detected.
     *
     * @return {@code true} when the sorter should consider locking a reference.
     */
    boolean isSeparatorDetected() {
        return separatorDetected;
    }
}
