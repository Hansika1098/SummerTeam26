package org.firstinspires.ftc.teamcode.testing;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;

import edu.ftcphoenix.fw.core.color.NormalizedRgba;
import edu.ftcphoenix.fw.core.source.Source;
import edu.ftcphoenix.fw.core.time.LoopClock;
import edu.ftcphoenix.fw.ftc.FtcSensors;

/**
 * Student A's stationary color-sensor measurement tool.
 *
 * <p>This OpMode never powers the spinner. Follow the existing Phoenix pattern for color sensor 2,
 * display both readings separately, and collect empty/separator/green/purple measurements before
 * changing observer thresholds.
 */
@TeleOp(name = "Sensor read test", group = "test")
public class SensorReadTest extends LinearOpMode {

    private static final String SENSOR_1_NAME = "color sensor 1";
    private static final String SENSOR_2_NAME = "color sensor 2";
    private static final float SENSOR_GAIN = 6.0f;

    @Override
    public void runOpMode() {
        LoopClock clock = new LoopClock();

        NormalizedColorSensor sensor1 =
                hardwareMap.get(NormalizedColorSensor.class, SENSOR_1_NAME);
        sensor1.setGain(SENSOR_GAIN);
        Source<NormalizedRgba> sensor1Source = FtcSensors.normalizedRgba(sensor1);

        telemetry.addLine("Motor is not controlled by this OpMode.");
        telemetry.addLine("TODO(A): add color sensor 2 using the same Phoenix pattern.");
        telemetry.addData("Second hardware name", SENSOR_2_NAME);
        telemetry.update();
        waitForStart();
        clock.reset(getRuntime());

        while (opModeIsActive()) {
            clock.update(getRuntime());
            NormalizedRgba sensor1Color = sensor1Source.get(clock);

            telemetry.addData("sensor1.alpha", sensor1Color.a);
            telemetry.addData("sensor1.rRatio", sensor1Color.rRatio());
            telemetry.addData("sensor1.gRatio", sensor1Color.gRatio());
            telemetry.addData("sensor1.bRatio", sensor1Color.bRatio());
            telemetry.addData("sensor1.chroma", sensor1Color.chroma());
            telemetry.update();
        }
    }
}
