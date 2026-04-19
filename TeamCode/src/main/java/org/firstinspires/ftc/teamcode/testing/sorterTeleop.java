package org.firstinspires.ftc.teamcode.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.Mechanisms.Sorter.Sorter;
import org.firstinspires.ftc.teamcode.Mechanisms.Sorter.SorterImpl;
import org.firstinspires.ftc.teamcode.Mechanisms.Sorter.SorterStatus;

import edu.ftcphoenix.fw.core.time.LoopClock;
import edu.ftcphoenix.fw.input.Gamepads;
import edu.ftcphoenix.fw.input.binding.Bindings;



@TeleOp(name = "Sorter Test Teleop ", group = "Sorter")
public final class sorterTeleop extends OpMode {

    private final LoopClock clock = new LoopClock();

    private Gamepads gamepads;
    private Bindings bindings;

    private Sorter sorter;

    @Override
    public void init() {
        clock.reset(getRuntime());

        gamepads = Gamepads.create(gamepad1, gamepad2);
        bindings = new Bindings();

        sorter = new SorterImpl(hardwareMap);

        //gamepasd controls bindings.rise makes it press continously

        bindings.onRise(gamepads.p1().a(), () -> sorter.startCalibration());

        bindings.onRise(gamepads.p1().b(), () -> sorter.startInventoryScan());

        bindings.onRise(gamepads.p1().x(), () -> sorter.tryPrepareLoad());

        bindings.onRise(gamepads.p1().y(), () -> sorter.tryFeed());

        bindings.onRise(gamepads.p1().dpadLeft(), () -> sorter.tryPresentSlot(0));
        bindings.onRise(gamepads.p1().dpadUp(), () -> sorter.tryPresentSlot(1));
        bindings.onRise(gamepads.p1().dpadRight(), () -> sorter.tryPresentSlot(2));

        bindings.onRise(gamepads.p1().rightBumper(), sorter::confirmShotComplete);

        bindings.onRise(gamepads.p1().leftBumper(), sorter::clearInventory);

        telemetry.log().add("Sorter test ready. Press START.");
    }

    @Override
    public void loop() {

        // 1) Clock (IMPORTANT for observer timing)
        clock.update(getRuntime());

        // 2) Update sorter (this runs EVERYTHING)
        sorter.update();

        // 3) Handle button edges
        bindings.update(clock);

        // 4) Telemetry
        renderTelemetry();
    }

    @Override
    public void stop() {
        // optional safety reset
    }

    private void renderTelemetry() {
        SorterStatus status = sorter.getStatus();

        telemetry.addLine("Sorter Full System Test");

        telemetry.addLine("A = calibrate");
        telemetry.addLine("B = inventory scan");
        telemetry.addLine("X = prepare load");
        telemetry.addLine("Y = feed");
        telemetry.addLine("DPAD = present slot (0/1/2)");
        telemetry.addLine("RB = confirm shot");
        telemetry.addLine("LB = clear inventory");

        telemetry.addData("Action", status.getAction());
        telemetry.addData("Calibration", status.getCalibrationState());

        telemetry.addData("Load Slot", status.getLoadSlot());
        telemetry.addData("Shoot Slot", status.getShootSlot());

        telemetry.addData("Load Aligned", status.isLoadAligned());
        telemetry.addData("Shoot Aligned", status.isShootAligned());

        telemetry.addData("Can Load", sorter.canLoad());
        telemetry.addData("Can Feed", sorter.canFeed());

        telemetry.addLine("--- Slots ---");
        telemetry.addData("Slot 0", sorter.getSlotContent(0));
        telemetry.addData("Slot 1", sorter.getSlotContent(1));
        telemetry.addData("Slot 2", sorter.getSlotContent(2));

        telemetry.update();
    }

}





