package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;

import edu.ftcphoenix.fw.core.color.NormalizedRgba;
import edu.ftcphoenix.fw.core.source.Source;
import edu.ftcphoenix.fw.core.time.LoopClock;
import edu.ftcphoenix.fw.ftc.FtcSensors;

/**
 * Dual-color observer for the sorter load station.
 *
 * <p>This observer reads two adjacent normalized REV color sensors, classifies what each one sees,
 * merges the two per-sensor classifications so a confident result from either sensor wins over
 * ambiguity, and uses a Phoenix accumulator to remember the best slot result across an entire
 * slot pass.
 *
 * <p>Expected call pattern:
 * <ul>
 *   <li>{@link #beginSlotPass(int)} when a slot enters the read window,</li>
 *   <li>{@link #sampleSlotPass(LoopClock)} every loop while the slot is under the sensors,</li>
 *   <li>{@link #endSlotPass()} once the slot leaves the read window.</li>
 * </ul>
 *
 * <p>Minimal observer interface change required for Phoenix usage:
 * <ul>
 *   <li>{@code SeparatorDetection sampleForCalibration(LoopClock clock)}</li>
 *   <li>{@code void sampleSlotPass(LoopClock clock)}</li>
 * </ul>
 */
final class DualColorLoadStationObserver implements LoadStationObserver {

    private static final String SENSOR_1_NAME = "color sensor 1";
    private static final String SENSOR_2_NAME = "color sensor 2";

    /** Tune on-robot with the Phoenix color sensor tester. */
    private static final float SENSOR_GAIN = 6.0f;

    // STUDENT A: change these thresholds only after recording both sensors under real lighting.

    /**
     * Very dim readings usually mean the sensor is looking at the far-away slot base / background.
     * Keep this mostly alpha-based so changing the base from tan to black does not break logic.
     */
    private static final double SLOT_BASE_ALPHA_MAX = 0.036;
    private static final double SLOT_BASE_ALPHA_RELAXED_MAX = 0.03;
    private static final double SLOT_BASE_CHROMA_MAX = 0.0035;

    /**
     * Separator is red and can be farther away than the top of an artifact, so let it classify
     * with a slightly weaker brightness gate than the artifact color rules.
     */
    private static final double SEPARATOR_ALPHA_MIN = 0.2;
    private static final double SEPARATOR_CHROMA_MIN = 0.004;
    private static final double SEPARATOR_RED_RATIO_MIN = 0.34;
    private static final double SEPARATOR_GREEN_RATIO_MAX = 0.39;
    private static final double SEPARATOR_BLUE_RATIO_MAX = 0.28;

    /** Confident artifact presence gate. */
    private static final double ARTIFACT_ALPHA_MIN = 0.060;
    private static final double ARTIFACT_CHROMA_MIN = 0.004;

    /** Confident green artifact ratios. */
    private static final double GREEN_GREEN_RATIO_MIN = 0.43;
    private static final double GREEN_RED_RATIO_MAX = 0.2;
    private static final double GREEN_BLUE_RATIO_MAX = 0.38;

    /** Confident purple artifact ratios. */
    private static final double PURPLE_RED_RATIO_MIN = 0.25;
    private static final double PURPLE_BLUE_RATIO_MIN = 0.38;
    private static final double PURPLE_GREEN_RATIO_MAX = 0.35;

    private final Source<SensorFrame> frameSource;
    private final Source<MergedObservation> mergedObservationSource;
    private final Source<PassMemory> passMemorySource;

    private SurfaceKind currentSurface;
    private boolean passActive;
    private int activePassSlot;
    private boolean artifactSeen;
    private SlotContent latchedSlotContent;
    private PassMemory passMemory;

    /**
     * Creates a new dual-color observer.
     *
     * @param hardwareMap FTC hardware map containing the configured normalized color sensors.
     */
    DualColorLoadStationObserver(HardwareMap hardwareMap) {
        NormalizedColorSensor sensor1 = hardwareMap.get(NormalizedColorSensor.class, SENSOR_1_NAME);
        NormalizedColorSensor sensor2 = hardwareMap.get(NormalizedColorSensor.class, SENSOR_2_NAME);

        sensor1.setGain(SENSOR_GAIN);
        sensor2.setGain(SENSOR_GAIN);

        Source<NormalizedRgba> sensor1Color = FtcSensors.normalizedRgba(sensor1);
        Source<NormalizedRgba> sensor2Color = FtcSensors.normalizedRgba(sensor2);

        this.frameSource = Source.of(clock -> new SensorFrame(
                        sensor1Color.get(clock),
                        sensor2Color.get(clock)))
                .memoized();

        this.mergedObservationSource = frameSource
                .map(DualColorLoadStationObserver::mergeFrame)
                .memoized();

        this.passMemorySource = mergedObservationSource
                .accumulate(DualColorLoadStationObserver::updatePassMemory, PassMemory.EMPTY);

        this.currentSurface = SurfaceKind.UNKNOWN;
        this.passActive = false;
        this.activePassSlot = -1;
        this.artifactSeen = false;
        this.latchedSlotContent = SlotContent.UNKNOWN;
        this.passMemory = PassMemory.EMPTY;
    }

    /**
     * Clears all transient observer state.
     */
    @Override
    public void reset() {
        frameSource.reset();
        mergedObservationSource.reset();
        clearPassState();
        currentSurface = SurfaceKind.UNKNOWN;
    }

    /**
     * Samples the sensors for calibration/reference finding.
     *
     * <p>The main calibration job here is separator detection.</p>
     *
     * @param clock current shared Phoenix loop clock.
     * @return reference-detection sample for the current loop.
     */
    @Override
    public SeparatorDetection sampleForCalibration(LoopClock clock) {
        MergedObservation observation = mergedObservationSource.get(clock);
        currentSurface = observation.surfaceKind;

        // TODO(A): This constant makes every surface look like a separator. Which part of the
        // merged observation represents the real separator evidence?
        return new SeparatorDetection(true);
    }

    /**
     * Starts a new pass for a slot entering the coarse read zone.
     *
     * @param slotIndex logical slot index entering the read zone.
     */
    @Override
    public void beginSlotPass(int slotIndex) {
        clearPassState();
        passActive = true;
        activePassSlot = slotIndex;
        currentSurface = SurfaceKind.UNKNOWN;
    }

    /**
     * Samples both sensors during an active pass.
     *
     * <p>This method is idempotent by {@link LoopClock#cycle()} because the Phoenix sources are
     * memoized / accumulated by loop cycle.</p>
     *
     * @param clock current shared Phoenix loop clock.
     */
    @Override
    public void sampleSlotPass(LoopClock clock) {
        if (!passActive) {
            return;
        }

        MergedObservation observation = mergedObservationSource.get(clock);
        currentSurface = observation.surfaceKind;

        passMemory = passMemorySource.get(clock);
        artifactSeen = passMemory.artifactSeen;
        latchedSlotContent = passMemory.finalContent();
    }

    /**
     * Ends the active pass and returns the observer's best final result.
     *
     * @return final result for the active slot pass.
     */
    @Override
    public SlotReadResult endSlotPass() {
        if (!passActive) {
            return new SlotReadResult(-1, SlotContent.UNKNOWN);
        }

        int slotIndex = activePassSlot;
        SlotReadResult result = new SlotReadResult(slotIndex, passMemory.finalContent());
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
                latchedSlotContent);
    }

    /**
     * Classifies one sensor's current reading.
     *
     * <p>Policy:
     * <ul>
     *   <li>separator is a red reference marker for calibration,</li>
     *   <li>green / purple are confident artifact colors,</li>
     *   <li>artifact-unknown means "something artifact-like is here but color is not confident",</li>
     *   <li>slot-base is mostly a dim/far-away reading and should work for tan or black bases.</li>
     * </ul>
     */
    private static SensorObservation classifySingleSensor(NormalizedRgba color) {
        double alpha = color.a;
        double chroma = color.chroma();
        double rRatio = color.rRatio();
        double gRatio = color.gRatio();
        double bRatio = color.bRatio();

        boolean looksSeparator = alpha >= SEPARATOR_ALPHA_MIN
                && chroma >= SEPARATOR_CHROMA_MIN
                && rRatio >= SEPARATOR_RED_RATIO_MIN
                && gRatio <= SEPARATOR_GREEN_RATIO_MAX
                && bRatio <= SEPARATOR_BLUE_RATIO_MAX;
        if (looksSeparator) {
            return SensorObservation.SEPARATOR;
        }

        boolean artifactStrength = alpha >= ARTIFACT_ALPHA_MIN && chroma >= ARTIFACT_CHROMA_MIN;
        if (artifactStrength) {
            boolean looksGreen = gRatio >= GREEN_GREEN_RATIO_MIN
                    && rRatio <= GREEN_RED_RATIO_MAX
                    && bRatio <= GREEN_BLUE_RATIO_MAX;
            if (looksGreen) {
                return SensorObservation.GREEN_ARTIFACT;
            }

            boolean looksPurple = rRatio >= PURPLE_RED_RATIO_MIN
                    && bRatio >= PURPLE_BLUE_RATIO_MIN
                    && gRatio <= PURPLE_GREEN_RATIO_MAX;
            if (looksPurple) {
                return SensorObservation.PURPLE_ARTIFACT;
            }

            return SensorObservation.ARTIFACT_UNKNOWN;
        }

        boolean looksBase = alpha <= SLOT_BASE_ALPHA_MAX
                || (alpha <= SLOT_BASE_ALPHA_RELAXED_MAX && chroma <= SLOT_BASE_CHROMA_MAX);
        if (looksBase) {
            return SensorObservation.SLOT_BASE;
        }

        return SensorObservation.UNKNOWN;
    }

    /**
     * Merges the two sensor observations for one loop sample.
     *
     * <p>A confident result from either sensor wins over uncertainty from the other sensor. This is
     * the key rule that lets one sensor recover when the other is over a ball hole.</p>
     */
    private static MergedObservation mergeFrame(SensorFrame frame) {
        SensorObservation left = classifySingleSensor(frame.sensor1);
        SensorObservation right = classifySingleSensor(frame.sensor2);

        boolean separatorDetected = left.separatorDetected || right.separatorDetected;
        boolean backgroundEvidence = left.backgroundEvidence || right.backgroundEvidence;

        boolean green = left.sampleContent == SlotContent.GREEN
                || right.sampleContent == SlotContent.GREEN;
        boolean purple = left.sampleContent == SlotContent.PURPLE
                || right.sampleContent == SlotContent.PURPLE;
        boolean artifactUnknown = left.sampleContent == SlotContent.OCCUPIED_UNKNOWN_COLOR
                || right.sampleContent == SlotContent.OCCUPIED_UNKNOWN_COLOR;

        if (green && purple) {
            return new MergedObservation(
                    SurfaceKind.ARTIFACT,
                    true,
                    separatorDetected,
                    backgroundEvidence,
                    SlotContent.OCCUPIED_UNKNOWN_COLOR);
        }

        if (green) {
            return new MergedObservation(
                    SurfaceKind.ARTIFACT,
                    true,
                    separatorDetected,
                    backgroundEvidence,
                    SlotContent.GREEN);
        }

        if (purple) {
            return new MergedObservation(
                    SurfaceKind.ARTIFACT,
                    true,
                    separatorDetected,
                    backgroundEvidence,
                    SlotContent.PURPLE);
        }

        if (artifactUnknown) {
            return new MergedObservation(
                    SurfaceKind.ARTIFACT,
                    true,
                    separatorDetected,
                    backgroundEvidence,
                    SlotContent.OCCUPIED_UNKNOWN_COLOR);
        }

        if (separatorDetected) {
            return new MergedObservation(
                    SurfaceKind.SEPARATOR,
                    false,
                    true,
                    backgroundEvidence,
                    SlotContent.UNKNOWN);
        }

        if (left == SensorObservation.SLOT_BASE || right == SensorObservation.SLOT_BASE) {
            return new MergedObservation(
                    SurfaceKind.SLOT_BASE,
                    false,
                    false,
                    backgroundEvidence,
                    SlotContent.UNKNOWN);
        }

        return new MergedObservation(
                SurfaceKind.UNKNOWN,
                false,
                false,
                backgroundEvidence,
                SlotContent.UNKNOWN);
    }

    /**
     * Phoenix reducer that accumulates pass memory across repeated loop samples.
     */
    private static PassMemory updatePassMemory(PassMemory previous, MergedObservation current) {
        return new PassMemory(
                true,
                previous.backgroundSeen || current.backgroundEvidence,
                previous.artifactSeen || current.artifactDetected,
                previous.greenSeen || current.sampleContent == SlotContent.GREEN,
                previous.purpleSeen || current.sampleContent == SlotContent.PURPLE);
    }

    /**
     * Clears all per-pass latches.
     */
    private void clearPassState() {
        passMemorySource.reset();
        passMemory = PassMemory.EMPTY;
        passActive = false;
        activePassSlot = -1;
        artifactSeen = false;
        latchedSlotContent = SlotContent.UNKNOWN;
    }

    /**
     * One sensor snapshot from both color sensors.
     */
    private static final class SensorFrame {
        private final NormalizedRgba sensor1;
        private final NormalizedRgba sensor2;

        private SensorFrame(NormalizedRgba sensor1, NormalizedRgba sensor2) {
            this.sensor1 = sensor1;
            this.sensor2 = sensor2;
        }
    }

    /**
     * Per-sensor classification before the two sensors are fused together.
     */
    private enum SensorObservation {
        GREEN_ARTIFACT(true, false, false, SlotContent.GREEN),
        PURPLE_ARTIFACT(true, false, false, SlotContent.PURPLE),
        ARTIFACT_UNKNOWN(true, false, false, SlotContent.OCCUPIED_UNKNOWN_COLOR),
        SEPARATOR(false, true, true, SlotContent.UNKNOWN),
        SLOT_BASE(false, false, true, SlotContent.UNKNOWN),
        UNKNOWN(false, false, false, SlotContent.UNKNOWN);

        private final boolean artifactDetected;
        private final boolean separatorDetected;
        private final boolean backgroundEvidence;
        private final SlotContent sampleContent;

        SensorObservation(boolean artifactDetected,
                          boolean separatorDetected,
                          boolean backgroundEvidence,
                          SlotContent sampleContent) {
            this.artifactDetected = artifactDetected;
            this.separatorDetected = separatorDetected;
            this.backgroundEvidence = backgroundEvidence;
            this.sampleContent = sampleContent;
        }
    }

    /**
     * Fused interpretation of both sensors for one loop sample.
     */
    private static final class MergedObservation {
        private final SurfaceKind surfaceKind;
        private final boolean artifactDetected;
        private final boolean separatorDetected;
        private final boolean backgroundEvidence;
        private final SlotContent sampleContent;

        private MergedObservation(SurfaceKind surfaceKind,
                                  boolean artifactDetected,
                                  boolean separatorDetected,
                                  boolean backgroundEvidence,
                                  SlotContent sampleContent) {
            this.surfaceKind = surfaceKind;
            this.artifactDetected = artifactDetected;
            this.separatorDetected = separatorDetected;
            this.backgroundEvidence = backgroundEvidence;
            this.sampleContent = sampleContent;
        }
    }

    /**
     * Slot-pass memory held by the Phoenix accumulator.
     */
    private static final class PassMemory {
        private static final PassMemory EMPTY = new PassMemory(false, false, false, false, false);

        private final boolean anySample;
        private final boolean backgroundSeen;
        private final boolean artifactSeen;
        private final boolean greenSeen;
        private final boolean purpleSeen;

        private PassMemory(boolean anySample,
                           boolean backgroundSeen,
                           boolean artifactSeen,
                           boolean greenSeen,
                           boolean purpleSeen) {
            this.anySample = anySample;
            this.backgroundSeen = backgroundSeen;
            this.artifactSeen = artifactSeen;
            this.greenSeen = greenSeen;
            this.purpleSeen = purpleSeen;
        }

        private SlotContent finalContent() {
            if (!anySample) {
                return SlotContent.UNKNOWN;
            }

            if (greenSeen && !purpleSeen) {
                return SlotContent.GREEN;
            }

            if (purpleSeen && !greenSeen) {
                return SlotContent.PURPLE;
            }

            if (artifactSeen) {
                return SlotContent.OCCUPIED_UNKNOWN_COLOR;
            }

            if (backgroundSeen) {
                return SlotContent.EMPTY;
            }

            return SlotContent.UNKNOWN;
        }
    }
}
