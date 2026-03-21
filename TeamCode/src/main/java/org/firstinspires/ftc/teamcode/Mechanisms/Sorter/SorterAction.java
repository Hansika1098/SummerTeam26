package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * High-level action currently being executed by the sorter.
 *
 * <p>This is intentionally more specific than a vague {@code busy} boolean. Downstream code can
 * use it for telemetry and guard logic without guessing why the sorter is unavailable.
 */
public enum SorterAction {
    /** No command is currently in progress. */
    IDLE,

    /** The spinner is calibrating its angular reference. */
    CALIBRATING,

    /** The spinner is rotating through every slot to populate or rebuild inventory beliefs. */
    INVENTORY_SCANNING,

    /** The sorter is positioning a known-empty slot to the load station. */
    PREPARING_LOAD,

    /** The sorter is positioning a chosen slot to the shooter station. */
    PRESENTING_SLOT,

    /** The sorter is executing one shooter feed cycle. */
    FEEDING
}
