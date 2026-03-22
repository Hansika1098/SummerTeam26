package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.Arrays;

import com.qualcomm.robotcore.hardware.DcMotor;

import com.qualcomm.robotcore.hardware.DcMotor;
/**
 * Skeleton implementation of {@link Sorter}.
 *
 * <p>This class is intentionally non-final in behavior but structurally complete enough for the
 * a real implementation to grow from a shared contract. It owns the public sorter API,
 * committed slot inventory, spinner action state, and the coarse geometry/read-zone decisions.
 *
 * <p>The second collaborator object, {@link LoadStationObserver}, owns dual-sensor interpretation,
 * separator detection, read-window latching, and conversion of one slot pass into one
 * {@link SlotReadResult}.
 */
public class SorterImpl implements Sorter {



    private static final int SLOT_COUNT = 3;

    private final HardwareMap hardwareMap;

    private DcMotor spinnerMotor;
    private final LoadStationObserver loadStationObserver;

    private final SlotContent[] slots;

    private CalibrationState calibrationState;
    private SorterAction action;

    private int loadSlot;
    private int shootSlot;

    private boolean loadAligned;
    private boolean shootAligned;

    private boolean slotPassActive;
    private int activePassSlot;

    private int targetSlot = -1;
    private double targetAngle = getAngleForSlot(targetSlot);

    private static final double DEGREES_PER_TICK = 360.0 / TICKS_PER_REV;

    private static final double TICKS_PER_REV = 0;// replac with the motors actual value





    private final double DEGREES_PER_SLOT= 45.0

            //idk the degrees lowk so i js put random number

    /**
     * Creates a sorter implementation using the default dual color sensor observer.
     *
     * @param hardwareMap FTC hardware map.
     */
    public SorterImpl(HardwareMap hardwareMap) {
        this(hardwareMap, new DualColorLoadStationObserver(hardwareMap));
    }

    /**
     * Package-private constructor for testing or dependency injection.
     *
     * <p>This supports tests and dependency injection with a fake or scripted observer without
     * waiting on physical sensor code.
     *
     * @param hardwareMap FTC hardware map.
     * @param loadStationObserver collaborator that interprets the dual color sensors.
     */
    SorterImpl(HardwareMap hardwareMap, LoadStationObserver loadStationObserver) {
        this.hardwareMap = hardwareMap;
        this.loadStationObserver = loadStationObserver;
        this.slots = new SlotContent[SLOT_COUNT];
        Arrays.fill(this.slots, SlotContent.UNKNOWN);

        this.calibrationState = CalibrationState.NOT_CALIBRATED;
        this.action = SorterAction.IDLE;

        this.loadSlot = -1;
        this.shootSlot = -1;

        this.loadAligned = false;
        this.shootAligned = false;

        this.slotPassActive = false;
        this.activePassSlot = -1;

       //defining motor
        spinnerMotor = hardwareMap.get(DcMotor.class, "spinnerMotor");
        spinnerMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE); //stops when power 0
    }

    private void setSpinnerPower(double power) {
        spinnerMotor.setPower(power);
    }
    /**
     * Runs one sorter loop.
     *
     * <p>The implementation should eventually route to action-specific update methods. The sorter
     * owns coarse geometry, spinner motion, and when a slot has entered or left the load read zone.
     * The observer owns the sensor interpretation that happens during each pass.
     */
    @Override
    public void update() {
        switch (action) {
            case CALIBRATING:
                updateCalibration();
                break;

            case INVENTORY_SCANNING:
                updateInventoryScan();
                break;

            case PREPARING_LOAD:
                updatePrepareLoad();
                break;

            case PRESENTING_SLOT:
                updatePresentSlot();
                break;

            case FEEDING:
                updateFeedCycle();
                break;

            case IDLE:
            default:
                updateIdleObservation();
                break;
        }
    }

    /**
     * Starts the calibration state machine.
     *
     * @return command acceptance or rejection.
     */
    @Override
    public CommandResult startCalibration() {
        if (action != SorterAction.IDLE) {
            return CommandResult.REJECTED_BUSY;
        }

        calibrationState = CalibrationState.SEARCHING_SEPARATOR;
        action = SorterAction.CALIBRATING;
        slotPassActive = false;
        activePassSlot = -1;
        loadStationObserver.reset();
        return CommandResult.ACCEPTED;
    }

    /**
     * Returns whether spinner geometry is currently trusted.
     *
     * @return {@code true} after calibration succeeds.
     */
    @Override
    public boolean isCalibrated() {
        return calibrationState == CalibrationState.CALIBRATED;
    }

    /**
     * Builds an immutable status snapshot from current sorter state.
     *
     * @return status snapshot.
     */
    @Override
    public SorterStatus getStatus() {
        return new SorterStatus(
                calibrationState,
                action,
                slots,
                loadSlot,
                shootSlot,
                loadAligned,
                shootAligned);
    }

    /**
     * Returns the committed belief for one slot.
     *
     * @param slotIndex logical slot index.
     * @return current slot belief.
     */
    @Override
    public SlotContent getSlotContent(int slotIndex) {
        validateSlotIndex(slotIndex);
        return slots[slotIndex];
    }

    /**
     * Returns the slot currently mapped to a station.
     *
     * @param station requested station.
     * @return slot at that station, or {@code -1} when not calibrated.
     */
    @Override
    public int getSlotAt(Station station) {
        if (!isCalibrated()) {
            return -1;
        }

        return station == Station.LOAD ? loadSlot : shootSlot;
    }

    /**
     * Returns whether a known-empty slot is aligned at load and the sorter is idle.
     *
     * @return {@code true} when loading should be safe from the sorter's perspective.
     */
    @Override
    public boolean canLoad() {
        return isCalibrated()
                && action == SorterAction.IDLE
                && loadAligned
                && loadSlot >= 0
                && slots[loadSlot] == SlotContent.EMPTY;
    }

    /**
     * Returns whether the shoot station currently holds an aligned occupied slot and the sorter is
     * idle.
     *
     * @return {@code true} when one slot is mechanically ready to feed.
     */
    @Override
    public boolean canFeed() {
        return isCalibrated()
                && action == SorterAction.IDLE
                && shootAligned
                && shootSlot >= 0
                && slots[shootSlot].isOccupied();
    }

    /**
     * Starts a full inventory scan.
     *
     * @return command acceptance or rejection.
     */
    @Override
    public CommandResult startInventoryScan() {
        if (action != SorterAction.IDLE) {
            return CommandResult.REJECTED_BUSY;
        }
        if (!isCalibrated()) {
            return CommandResult.REJECTED_NOT_CALIBRATED;
        }

        action = SorterAction.INVENTORY_SCANNING;
        return CommandResult.ACCEPTED;
    }

    /**
     * Starts positioning a known-empty slot at the load station.
     *
     * @return command acceptance or rejection.
     */
    @Override
    public CommandResult tryPrepareLoad() {
        if (action != SorterAction.IDLE) {
            return CommandResult.REJECTED_BUSY;
        }
        if (!isCalibrated()) {
            return CommandResult.REJECTED_NOT_CALIBRATED;
        }
        if (!hasKnownEmptySlot()) {
            return CommandResult.REJECTED_NO_EMPTY_SLOT;
        }

        action = SorterAction.PREPARING_LOAD;
        return CommandResult.ACCEPTED;
    }

    /**
     * Starts positioning a chosen slot at the shooter station.
     *
     * @param slotIndex chosen slot.
     * @return command acceptance or rejection.
     */
    @Override
    public CommandResult tryPresentSlot(int slotIndex) {
        validateSlotIndex(slotIndex);

        if (action != SorterAction.IDLE) {
            return CommandResult.REJECTED_BUSY;
        }
        if (!isCalibrated()) {
            return CommandResult.REJECTED_NOT_CALIBRATED;
        }//reject if slot is empty or unknown
        if (slots[slotIndex] == SlotContent.UNKNOWN || slots[slotIndex] == SlotContent.EMPTY) {
            return CommandResult.REJECTED_NOT_READY;

            //set the target slot and target angle
            targetSlot = slotIndex;

            targetAngle = slotIndex * DEGREES_PER_SLOT;

            //updates sorter state
            action= SorterAction.PRESENTING_SLOT;

            return CommandResult.ACCEPTED;



        }

        action = SorterAction.PRESENTING_SLOT;
        return CommandResult.ACCEPTED;
    }

    /**
     * Starts one shooter feed cycle.
     *
     * @return command acceptance or rejection.
     */
    @Override
    public CommandResult tryFeed() {
        if (action != SorterAction.IDLE) {
            return CommandResult.REJECTED_BUSY;
        }
        if (!isCalibrated()) {
            return CommandResult.REJECTED_NOT_CALIBRATED;
        }
        if (!canFeed()) {
            return CommandResult.REJECTED_NOT_READY;
        }

        action = SorterAction.FEEDING;
        return CommandResult.ACCEPTED;
    }

    /**
     * Clears the currently presented shoot slot after downstream code confirms the artifact exited.
     *
     * <p>This method is deliberately simple in the skeleton. A full implementation may want to
     * check feed state, a beam break, or another shooter-side confirmation before allowing the
     * inventory update.
     */
    @Override
    public void confirmShotComplete() {
        if (shootSlot >= 0 && shootSlot < SLOT_COUNT) {
            slots[shootSlot] = SlotContent.EMPTY;
        }
    }

    /**
     * Marks every slot as unknown.
     */
    @Override
    public void markInventoryUnknown() {
        Arrays.fill(slots, SlotContent.UNKNOWN);
    }

    /**
     * Marks every slot as empty.
     */
    @Override
    public void clearInventory() {
        Arrays.fill(slots, SlotContent.EMPTY);
    }

    /**
     * Updates the calibration sequence.
     *
     * <p>Expected responsibilities:
     *
     * <ul>
     *   <li>rotate the spinner slowly,
     *   <li>call {@link LoadStationObserver#sampleForCalibration()},
     *   <li>lock the separator reference when it is detected,
     *   <li>align from separator reference to a slot-center reference,
     *   <li>populate {@code loadSlot}, {@code shootSlot}, and alignment state,
     *   <li>set {@link CalibrationState#CALIBRATED} and return to {@link SorterAction#IDLE}.
     * </ul>
     */
    private void updateCalibration() {
        // TODO: implement calibration state machine.
    }

    /**
     * Updates the action that scans all slots through the load station.
     *
     * <p>This action is especially useful after startup with preloaded artifacts or after a manual
     * intervention that invalidated the slot table.
     */
    private void updateInventoryScan() {
        // TODO: implement inventory-scan state machine.
    }

    /**
     * Updates the action that moves a known-empty slot to the load station.
     */
    private void updatePrepareLoad() {
        // TODO: implement motion and completion rules for prepare-load.
    }

    /**
     * Updates the action that moves a chosen slot to the shooter station.
     */
    private void updatePresentSlot() {

        //work needed for this func:
        // TODO: implement motion and completion rules for slot presentation.
        //read the current angle fo spinner
        if(targetSlot <0) return; //only runs if slot has been commanded

        double currentAngle = getCurrentSpinnerAngle();//method below uses motor encoder to know encoder pos

        //calculate the error  (distance from target)
        double error = targetAngle - currentAngle;


        if (Math.abs(error)<1.0) {
            setSpinnerPower(0.0);
            shootSlot = targetSlot;  //if distance to target less than  1 do these things
            shootAligned = true;
            targetSlot = -1;
            action = SorterAction.IDLE;
        }
        else{
            double power =0.3 * Math.signum (error);
              setSpinnerPower(power);
              //spin to target




        }



    }

    private double getAngleForSlot(int slotIndex) {
        return slotIndex * 360.0 / SLOT_COUNT;
    }




    private double getCurrentSpinnerAngle() {
        int ticks = spinnerMotor.getCurrentPosition();
        return ticks * DEGREES_PER_TICK;
    }





    /**
     * Updates the feed-cycle action.
     *
     * <p>The sorter side should own spinner/feed sequencing here. The actual decision to clear a
     * slot is still deferred to {@link #confirmShotComplete()}.
     */
    private void updateFeedCycle() {
        // TODO: implement feed timing/state machine.
    }

    /**
     * Updates passive observation while the sorter is otherwise idle.
     *
     * <p>This is where the sorter should watch for a slot entering the coarse load read zone,
     * begin a slot pass, forward repeated samples to the observer, and commit the result when the
     * pass ends.
     */
    private void updateIdleObservation() {
        // TODO: implement passive read-pass handling during normal loading.
    }

    /**
     * Returns whether at least one slot is currently known-empty.
     *
     * @return {@code true} when a future prepare-load action could select an empty slot.
     */
    private boolean hasKnownEmptySlot() {
        for (SlotContent slot : slots) {
            if (slot == SlotContent.EMPTY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Commits one observer result into the authoritative slot table.
     *
     * @param slotReadResult completed result for one slot pass.
     */
    private void applyReadResult(SlotReadResult slotReadResult) {
        if (slotReadResult == null) {
            return;
        }

        int slotIndex = slotReadResult.getSlotIndex();
        if (slotIndex >= 0 && slotIndex < SLOT_COUNT) {
            slots[slotIndex] = slotReadResult.getSlotContent();
        }
    }

    /**
     * Starts a new observer pass for the given slot.
     *
     * @param slotIndex slot entering the coarse read zone.
     */
    private void beginSlotPass(int slotIndex) {
        slotPassActive = true;
        activePassSlot = slotIndex;
        loadStationObserver.beginSlotPass(slotIndex);
    }

    /**
     * Finishes the current observer pass and commits the observer's slot result.
     */
    private void endSlotPass() {
        SlotReadResult result = loadStationObserver.endSlotPass();
        applyReadResult(result);
        slotPassActive = false;
        activePassSlot = -1;
    }

    /**
     * Validates a slot index.
     *
     * @param slotIndex slot index to validate.
     * @throws IllegalArgumentException if the index is outside {@code 0..2}.
     */
    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) {
            throw new IllegalArgumentException("slotIndex must be in the range 0..2");
        }
    }
}
