package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * Result of issuing a non-blocking sorter command.
 *
 * <p>Sorter commands are designed for FTC loop-based code. A command request should return
 * immediately and report whether the sorter accepted the request, rather than blocking until the
 * motion is complete.
 */
public enum CommandResult {
    /** The sorter accepted the command and will work on it over future {@code update()} calls. */
    ACCEPTED,

    /** The sorter rejected the command because another action is already running. */
    REJECTED_BUSY,

    /** The sorter rejected the command because slot geometry is not calibrated yet. */
    REJECTED_NOT_CALIBRATED,

    /** The requested slot index was outside the valid range for the spinner. */
    REJECTED_INVALID_SLOT,

    /** The command required a known-empty slot, but no such slot was currently available. */
    REJECTED_NO_EMPTY_SLOT,

    /** The command was rejected because the current state was not suitable for that action. */
    REJECTED_NOT_READY
}
