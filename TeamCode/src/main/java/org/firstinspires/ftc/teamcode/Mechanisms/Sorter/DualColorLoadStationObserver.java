package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

import android.graphics.Color;

import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Skeleton observer that uses two adjacent REV-style color sensors above the load station.
 *
 * <p>This class is the recommended home for all sensor-side complexity:
 *
 * <ul>
 *   <li>reading both sensors,
 *   <li>classifying separator vs. slot base vs. artifact,
 *   <li>opening and interpreting a pass after the sorter says a slot entered the coarse read zone,
 *   <li>latching color across multiple loop samples,
 *   <li>building one final {@link SlotReadResult} at the end of the pass.
 * </ul>
 *
 * <p>The sorter should never need to know how those details are implemented.
 */
final class DualColorLoadStationObserver implements LoadStationObserver {

    private static final String SENSOR_1_NAME = "color sensor 1";
    private static final String SENSOR_2_NAME = "color sensor 2";

    private static final float[] HSV_BUFFER_1 = new float[3];
    private static final float[] HSV_BUFFER_2 = new float[3];

    private final ColorSensor sensor1;
    private final ColorSensor sensor2;

    private SurfaceKind currentSurface;
    private boolean passActive;
    private int activePassSlot;
    private boolean artifactSeen;
    private SlotContent latchedCandidateContent;

    /**
     * Creates a new dual-color observer.
     *
     * @param hardwareMap FTC hardware map containing the configured color sensors.
     */
    DualColorLoadStationObserver(HardwareMap hardwareMap) {
        this.sensor1 = hardwareMap.get(ColorSensor.class, SENSOR_1_NAME);
        this.sensor2 = hardwareMap.get(ColorSensor.class, SENSOR_2_NAME);

        this.currentSurface = SurfaceKind.UNKNOWN;
        this.passActive = false;
        this.activePassSlot = -1;
        this.artifactSeen = false;
        this.latchedCandidateContent = SlotContent.UNKNOWN;
    }

    /**
     * Clears all transient observer state.
     */
    @Override
    public void reset() {
        clearPassState();
        currentSurface = SurfaceKind.UNKNOWN;
    }

    /**
     * Samples the sensors for calibration/reference finding.
     *
     * <p>The eventual implementation should detect the distinctive separator appearance while the
     * spinner is rotated slowly during calibration.
     *
     * @return reference-detection sample for the current loop.
     */
    @Override
    public ReferenceDetection sampleForCalibration() {
        SensorFrame frame = readSensorFrame();
        currentSurface = classifySurface(frame);

        // TODO: replace this placeholder with real separator detection logic.
        boolean separatorDetected = currentSurface == SurfaceKind.SEPARATOR;
        return new ReferenceDetection(separatorDetected);
    }

    /**
     * Starts a new pass for a slot entering the coarse read zone.
     *
     * @param slotIndex logical slot index entering the read zone.
     */
    @Override
    public void beginSlotPass(int slotIndex) {
        passActive = true;
        activePassSlot = slotIndex;
        artifactSeen = false;
        latchedCandidateContent = SlotContent.UNKNOWN;
    }

    /**
     * Samples both sensors during an active pass.
     *
     * <p>The eventual implementation should keep accumulating evidence across repeated calls.
     */
    @Override
    public void sampleSlotPass() {
        if (!passActive) {
            return;
        }

        SensorFrame frame = readSensorFrame();
        currentSurface = classifySurface(frame);

        updateArtifactLatch(frame, currentSurface);
        updateColorLatch(frame, currentSurface);
    }

    /**
     * Ends the active pass and returns the observer's best final result.
     *
     * @return final result for the active slot pass.
     */
    @Override
    public SlotReadResult endSlotPass() {
        SlotReadResult result = buildReadResult(activePassSlot);
        clearPassState();
        return result;
    }

    /**
     * Returns debug information for telemetry and tuning.
     *
     * @return observer debug snapshot.
     */
    @Override
    public ObserverDebug getDebug() {
        return new ObserverDebug(
                currentSurface,
                passActive,
                activePassSlot,
                artifactSeen,
                latchedCandidateContent);
    }

    /**
     * Reads both color sensors and computes per-sensor HSV values.
     *
     * <p>This helper centralizes raw sensor access so the rest of the observer can work from one
     * consistent frame object.
     *
     * @return current raw-and-derived sensor frame.
     */
    private SensorFrame readSensorFrame() {
        int red1 = sensor1.red();
        int green1 = sensor1.green();
        int blue1 = sensor1.blue();

        int red2 = sensor2.red();
        int green2 = sensor2.green();
        int blue2 = sensor2.blue();

        Color.RGBToHSV(red1, green1, blue1, HSV_BUFFER_1);
        Color.RGBToHSV(red2, green2, blue2, HSV_BUFFER_2);

        return new SensorFrame(
                red1, green1, blue1, HSV_BUFFER_1[0],
                red2, green2, blue2, HSV_BUFFER_2[0]);
    }

    /**
     * Classifies what kind of surface the sensors appear to be seeing right now.
     *
     * <p>Expected future logic:
     *
     * <ul>
     *   <li>detect slot base / black background,
     *   <li>detect separator appearance for calibration,
     *   <li>detect artifact presence,
     *   <li>stay conservative when the sample is ambiguous.
     * </ul>
     *
     * @param frame current sensor frame.
     * @return current coarse surface classification.
     */
    private SurfaceKind classifySurface(SensorFrame frame) {
        // TODO: replace placeholder heuristics with tuned logic.
        return SurfaceKind.UNKNOWN;
    }

    /**
     * Updates the observer's "artifact seen" latch during an active pass.
     *
     * <p>The goal is to remember that the slot appeared occupied at some point during the pass,
     * even if later samples briefly look like background due to holes or rolling.
     *
     * @param frame current sensor frame.
     * @param surfaceKind coarse current surface classification.
     */
    private void updateArtifactLatch(SensorFrame frame, SurfaceKind surfaceKind) {
        if (surfaceKind == SurfaceKind.ARTIFACT) {
            artifactSeen = true;
            if (latchedCandidateContent == SlotContent.UNKNOWN) {
                latchedCandidateContent = SlotContent.OCCUPIED_UNKNOWN_COLOR;
            }
        }
    }

    /**
     * Updates the latched color candidate for the active pass.
     *
     * <p>The intended design is to keep the best valid color seen anywhere in the pass rather than
     * relying on only one instant in time.
     *
     * @param frame current sensor frame.
     * @param surfaceKind coarse current surface classification.
     */
    private void updateColorLatch(SensorFrame frame, SurfaceKind surfaceKind) {
        if (surfaceKind != SurfaceKind.ARTIFACT) {
            return;
        }

        // TODO: replace placeholder logic with tuned hue / confidence rules.
        // Example:
        // if (looksPurple(frame)) { latchedCandidateContent = SlotContent.PURPLE; }
        // if (looksGreen(frame))  { latchedCandidateContent = SlotContent.GREEN;  }
    }

    /**
     * Builds the final result for the completed pass.
     *
     * <p>Recommended semantics:
     *
     * <ul>
     *   <li>no artifact ever seen -> {@link SlotContent#EMPTY}
     *   <li>artifact seen and color latched -> {@link SlotContent#PURPLE} or {@link SlotContent#GREEN}
     *   <li>artifact seen without confident color -> {@link SlotContent#OCCUPIED_UNKNOWN_COLOR}
     * </ul>
     *
     * @param slotIndex observed slot index.
     * @return final slot-read result.
     */
    private SlotReadResult buildReadResult(int slotIndex) {
        if (!artifactSeen) {
            return new SlotReadResult(slotIndex, SlotContent.EMPTY);
        }
        return new SlotReadResult(slotIndex, latchedCandidateContent);
    }

    /**
     * Clears all per-pass latches.
     */
    private void clearPassState() {
        passActive = false;
        activePassSlot = -1;
        artifactSeen = false;
        latchedCandidateContent = SlotContent.UNKNOWN;
    }

    /**
     * One raw-and-derived sensor snapshot from both color sensors.
     */
    private static final class SensorFrame {

        private final int red1;
        private final int green1;
        private final int blue1;
        private final float hue1;

        private final int red2;
        private final int green2;
        private final int blue2;
        private final float hue2;

        private SensorFrame(
                int red1,
                int green1,
                int blue1,
                float hue1,
                int red2,
                int green2,
                int blue2,
                float hue2) {
            this.red1 = red1;
            this.green1 = green1;
            this.blue1 = blue1;
            this.hue1 = hue1;
            this.red2 = red2;
            this.green2 = green2;
            this.blue2 = blue2;
            this.hue2 = hue2;
        }

        /**
         * Returns the first sensor's red channel.
         */
        int getRed1() {
            return red1;
        }

        /**
         * Returns the first sensor's green channel.
         */
        int getGreen1() {
            return green1;
        }

        /**
         * Returns the first sensor's blue channel.
         */
        int getBlue1() {
            return blue1;
        }

        /**
         * Returns the first sensor's hue.
         */
        float getHue1() {
            return hue1;
        }

        /**
         * Returns the second sensor's red channel.
         */
        int getRed2() {
            return red2;
        }

        /**
         * Returns the second sensor's green channel.
         */
        int getGreen2() {
            return green2;
        }

        /**
         * Returns the second sensor's blue channel.
         */
        int getBlue2() {
            return blue2;
        }

        /**
         * Returns the second sensor's hue.
         */
        float getHue2() {
            return hue2;
        }
    }
}
