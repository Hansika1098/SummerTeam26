package org.firstinspires.ftc.teamcode.testing;
import static android.graphics.Color.RGBToHSV;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.Locale;

import edu.ftcphoenix.fw.core.color.NormalizedRgba;
import edu.ftcphoenix.fw.core.source.Source;
import edu.ftcphoenix.fw.core.time.LoopClock;
import edu.ftcphoenix.fw.ftc.FtcSensors;

@TeleOp(name = "Sensor read test", group = "test")
public class SensorReadTest extends LinearOpMode {

    NormalizedColorSensor sensorColor;

    @Override
    public void runOpMode() {

        LoopClock clock = new LoopClock();
        clock.reset(getRuntime());

        // get reference to the color sensor
        sensorColor = hardwareMap.get(NormalizedColorSensor.class, "color sensor 1");

        sensorColor.setGain(10f);

        Source<NormalizedRgba> sensorSource = FtcSensors.normalizedRgba(sensorColor);

        // wait for the start button to be pressed
        waitForStart();

        // loop & read RGB data
        while (opModeIsActive()) {
            clock.update(getRuntime());
            NormalizedRgba color = sensorSource.get(clock);

            /*
            // convert RGB values to HSV values
            // multiply by the scale factor
            // cast it back to int (SCALE_FACTOR is a double)
            Color.RGBToHSV((int) (sensorColor.red() * SCALE_FACTOR),
                    (int) (sensorColor.green() * SCALE_FACTOR),
                    (int) (sensorColor.blue() * SCALE_FACTOR),
                    hsvValues);

            double hue = hsvValues[0];
            String object = "UNKNOWN";

            // red: 18 to 100
            if (hue >= 18 && hue <= 100) {
                object = "SEPERATOR";
            }
            // purple: 190 to 250
            else if (hue >= 190 && hue <= 250) {
                object = "PURPLE";
            }

            // green: 150 to 170
            else if (hue >= 150 && hue <= 170) {
                object = "GREEN";
            }
             */

            // send info back to driver station using telemetry
            telemetry.addData("Alpha", color.a);
            telemetry.addData("rRatio", color.rRatio());
            telemetry.addData("gRatio", color.gRatio());
            telemetry.addData("bRatio", color.bRatio());
            telemetry.addData("Chroma", color.chroma());

            telemetry.update();

        }
    }

}
