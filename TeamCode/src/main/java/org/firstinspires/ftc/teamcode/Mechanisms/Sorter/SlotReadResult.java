package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

/**
 * Slot info (ID & contents)
 *
 * Final result of one completed load-station slot pass.
 *
 * <p>This is the unit of information handed from the observer back to the sorter. The observer
 * does not own the long-term slot table; it only reports the best result for one pass.
 */
final class SlotReadResult {

    private final int slotIndex;
    private final SlotContent slotContent;

    /**
     * Creates a new slot-read result.
     *
     * @param slotIndex logical slot index that was observed.
     * @param slotContent final belief for that slot based on the pass.
     */
    SlotReadResult(int slotIndex, SlotContent slotContent) {
        this.slotIndex = slotIndex;
        this.slotContent = slotContent;
    }

    /**
     * Returns which logical slot the result applies to.
     *
     * @return observed slot index.
     */
    int getSlotIndex() {
        return slotIndex;
    }

    /**
     * Returns the observer's final belief for that slot pass.
     *
     * @return slot-content result produced by the observer.
     */
    SlotContent getSlotContent() {
        return slotContent;
    }
}
