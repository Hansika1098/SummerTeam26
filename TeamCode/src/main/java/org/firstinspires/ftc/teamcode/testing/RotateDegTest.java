package org.firstinspires.ftc.teamcode.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

/**
 * Student B's low-power, hold-to-run spinner measurement tool.
 *
 * <p>The ticks-per-revolution value is only a provisional display aid. Measure direction and
 * encoder travel on the real robot before using either value as sorter geometry.
 */
@TeleOp(name = "Spinner low-power jog test", group = "Sorter")
public class RotateDegTest extends OpMode {

    private static final double JOG_POWER = 0.08;
    private static final double PROVISIONAL_TICKS_PER_REV = 383.5;

    private DcMotorEx spinner;

    @Override
    public void init() {
        spinner = hardwareMap.get(DcMotorEx.class, "spinnerMotor");

        spinner.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        spinner.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        spinner.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        spinner.setPower(0.0);
    }

    @Override
    public void loop() {
        double requestedPower = 0.0;
        if (gamepad1.dpad_right && !gamepad1.dpad_left) {
            requestedPower = JOG_POWER;
        } else if (gamepad1.dpad_left && !gamepad1.dpad_right) {
            requestedPower = -JOG_POWER;
        }

        spinner.setPower(requestedPower);

        telemetry.addLine("Hold D-pad LEFT/RIGHT to jog; release to stop");
        telemetry.addLine("Keep hands and loose items clear; use Driver Station STOP if needed");
        telemetry.addData("Current ticks", spinner.getCurrentPosition());
        telemetry.addData("Commanded power", "%.2f", requestedPower);
        telemetry.addData("Provisional ticks/rev", PROVISIONAL_TICKS_PER_REV);
        telemetry.addData(
                "Provisional angle",
                "%.1f",
                spinner.getCurrentPosition() * 360.0 / PROVISIONAL_TICKS_PER_REV);
        telemetry.update();
    }

    @Override
    public void stop() {
        if (spinner != null) {
            spinner.setPower(0.0);
        }
    }
}
