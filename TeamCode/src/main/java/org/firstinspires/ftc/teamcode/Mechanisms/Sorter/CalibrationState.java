package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * Progress of spinner calibration.
 *
 * <p>Calibration means finding a trustworthy angular reference for the spinner so the software can
 * map physical slot positions to logical slot indices.
 */
public enum CalibrationState {
    /** No calibration has been started since boot or reset. */
    NOT_CALIBRATED,

    /** The spinner is rotating slowly while searching for a separator reference. */
    SEARCHING_SEPARATOR,

    /** A separator reference was found and the sorter is aligning to a slot-center reference. */
    ALIGNING_REFERENCE,

    /** Slot geometry is now trusted. */
    CALIBRATED,

    /** Calibration ended unsuccessfully and requires operator or software recovery. */
    FAILED
}
