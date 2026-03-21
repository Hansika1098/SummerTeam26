package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * Coarse surface classification currently seen by the observer.
 *
 * <p>This type is kept internal because it is useful for calibration and tuning, but downstream
 * robot code should not depend on these sensing details.
 */
enum SurfaceKind {
    /** No stable classification is currently available. */
    UNKNOWN,

    /** The observer believes it is currently looking at a spinner separator or wall. */
    SEPARATOR,

    /** The observer believes it is looking at the slot floor / background surface. */
    SLOT_BASE,

    /** The observer believes it is looking at an artifact. */
    ARTIFACT
}
