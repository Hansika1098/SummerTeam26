package org.firstinspires.ftc.teamcode.Mechanisms.Sorter;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.util.Locale;

import edu.ftcphoenix.fw.core.time.LoopClock;
import edu.ftcphoenix.fw.input.Gamepads;
import edu.ftcphoenix.fw.input.binding.Bindings;

/**
 * Student A's motor-free test for one complete observer pass at a time.
 *
 * <p>Move the spinner only with the mentor-approved manual procedure and after confirming it is
 * safe to back-drive. This OpMode does not command the spinner motor.
 */
@TeleOp(name = "Sorter: DualColor Observer Test", group = "Sorter")
public final class ObserverTeleop extends OpMode {

    private static final int FIRST_SLOT_INDEX = 0;

    private final LoopClock clock = new LoopClock();

    private Gamepads gamepads;
    private Bindings bindings;

    private DualColorLoadStationObserver observer;

    private int activeSlotIndex;
    private SlotReadResult lastCompletedResult;

    @Override
    public void init() {
        clock.reset(getRuntime());

        gamepads = Gamepads.create(gamepad1, gamepad2);
        bindings = new Bindings();

        observer = new DualColorLoadStationObserver(hardwareMap);

        bindings.onRise(gamepads.p1().a(), this::completeCurrentSlotAndAdvance);
        bindings.onRise(gamepads.p1().b(), this::resetTestSequence);

        activeSlotIndex = FIRST_SLOT_INDEX;
        observer.beginSlotPass(activeSlotIndex);

        telemetry.log().add("Observer test ready; this OpMode does not control the spinner.");
    }

    @Override
    public void loop() {
        // 1) Clock
        clock.update(getRuntime());

        // 2) Sensor / observer sampling.
        // Sample before bindings so an A-press closes the slot after it gets one final sample.
        observer.sampleSlotPass(clock);

        // 3) Button edge handling
        bindings.update(clock);

        // 4) Driver telemetry
        renderRunTelemetry();
    }

    @Override
    public void stop() {
        observer.reset();
    }

    /**
     * Ends the current slot, prints its result to the Driver Hub, then begins the next slot.
     */
    private void completeCurrentSlotAndAdvance() {
        SlotReadResult result = observer.endSlotPass();
        lastCompletedResult = result;

        telemetry.log().add(formatSlotResult(result));

        activeSlotIndex = (activeSlotIndex + 1) % 3;
        observer.beginSlotPass(activeSlotIndex);
    }

    /**
     * Clears observer/test state and restarts from slot 0.
     */
    private void resetTestSequence() {
        observer.reset();
        lastCompletedResult = null;

        activeSlotIndex = FIRST_SLOT_INDEX;
        observer.beginSlotPass(activeSlotIndex);

        telemetry.log().add(String.format(Locale.US, "Observer test reset. Restarted at slot %d", activeSlotIndex));
    }

    /**
     * Draws driver-facing telemetry for the current test state.
     */
    private void renderRunTelemetry() {
        ObserverDebug debug = observer.getDebug();

        telemetry.addLine("DualColorLoadStationObserver Test");
        telemetry.addLine("MOTOR OFF: manually move one complete slot under the observer");
        telemetry.addLine("A = finish after the slot's trailing edge passes");
        telemetry.addLine("B = reset observer test to slot 0");
        telemetry.addData("clock.cycle", clock.cycle());
        telemetry.addData("clock.dtSec", clock.dtSec());
        telemetry.addData("activeSlot", activeSlotIndex);
        telemetry.addData("passActive", debug.isPassActive());
        telemetry.addData("currentSurface", debug.getCurrentSurface());
        telemetry.addData("seenArtifact", debug.hasSeenArtifact());
        telemetry.addData("latchedContent", debug.getLatchedCandidateContent());

        if (lastCompletedResult == null) {
            telemetry.addData("lastCompleted", "none yet");
        } else {
            telemetry.addData("lastCompleted", formatSlotResult(lastCompletedResult));
        }

        telemetry.update();
    }

    private static String formatSlotResult(SlotReadResult result) {
        return String.format(
                Locale.US,
                "slot %d -> %s",
                result.getSlotIndex(),
                result.getSlotContent());
    }
}
