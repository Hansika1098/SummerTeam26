package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * Public sorter stations that matter to the rest of the robot.
 *
 * <p>The load station is where intake handoff and load-side classification occur. The shoot
 * station is where a chosen slot is presented to the shooter.
 *
 * <p>The current mechanism mounts the color sensors at the load station, but that is intentionally
 * hidden from the public sorter API. If hardware changes later, the load observer can move without
 * forcing the rest of the robot to change.
 */
public enum Station {
    /** Intake / loading side of the spindexer. */
    LOAD,

    /** Shooter presentation side of the spindexer. */
    SHOOT
}
