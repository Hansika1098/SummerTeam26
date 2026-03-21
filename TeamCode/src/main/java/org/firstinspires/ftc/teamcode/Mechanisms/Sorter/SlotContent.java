package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * Public belief about what a sorter slot currently contains.
 *
 * <p>This enum is designed to stay simple for the rest of the robot while still covering the
 * cases that matter in a real DECODE robot:
 *
 * <ul>
 *   <li>{@link #UNKNOWN}: the sorter does not currently trust its belief for this slot.
 *   <li>{@link #EMPTY}: the sorter believes the slot is empty.
 *   <li>{@link #PURPLE}: the sorter believes the slot holds a purple artifact.
 *   <li>{@link #GREEN}: the sorter believes the slot holds a green artifact.
 *   <li>{@link #OCCUPIED_UNKNOWN_COLOR}: the sorter believes the slot is occupied, but the
 *       observer did not latch a confident color before the slot left the read window.
 * </ul>
 *
 * <p>Using a single enum keeps downstream code easier to read than splitting the state into
 * separate {@code occupied} and {@code color} fields.
 */
public enum SlotContent {
    /** The sorter does not currently trust the content of the slot. */
    UNKNOWN,

    /** The sorter believes the slot contains no artifact. */
    EMPTY,

    /** The sorter believes the slot contains a purple artifact. */
    PURPLE,

    /** The sorter believes the slot contains a green artifact. */
    GREEN,

    /**
     * The sorter believes something is in the slot, but does not know whether it is purple or
     * green.
     */
    OCCUPIED_UNKNOWN_COLOR;

    /**
     * Returns whether this slot belief says the slot is occupied by some artifact.
     *
     * @return {@code true} when the slot is believed to contain an artifact of known or unknown
     *         color.
     */
    public boolean isOccupied() {
        return this == PURPLE || this == GREEN || this == OCCUPIED_UNKNOWN_COLOR;
    }

    /**
     * Returns whether this slot belief contains a fully known artifact color.
     *
     * @return {@code true} for {@link #PURPLE} and {@link #GREEN}.
     */
    public boolean hasKnownColor() {
        return this == PURPLE || this == GREEN;
    }

    /**
     * Converts this slot belief to a concrete artifact color when one is known.
     *
     * @return the known artifact color, or {@code null} when the slot is empty or not fully known.
     */
    public ArtifactColor getKnownColorOrNull() {
        if (this == PURPLE) {
            return ArtifactColor.PURPLE;
        }
        if (this == GREEN) {
            return ArtifactColor.GREEN;
        }
        return null;
    }
}
