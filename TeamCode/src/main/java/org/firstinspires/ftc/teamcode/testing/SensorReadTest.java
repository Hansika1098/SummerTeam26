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

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.Locale;

@TeleOp(name = "Sensor read test", group = "test")
public class SensorReadTest extends LinearOpMode {

    ColorSensor sensorColor;

    @Override
    public void runOpMode() {
        // get reference to the color sensor
        sensorColor = hardwareMap.get(ColorSensor.class, "colorSensor");

        // hsvValues is an array holding the hue, saturation, and value info
        float hsvValues[] = {0F, 0f, 0f};

        // values is a reference to hsvValues array
        final float values[] = hsvValues;

        // create scale factor to multiply RGB values with (amplify measured values)
        final double SCALE_FACTOR = 255;

        // get reference to the RelativeLayout to change background color of driver hub
        // to the hue detected by sensor
        int relativeLayoutId = hardwareMap.appContext.getResources().getIdentifier("RelativeLayout", "id", hardwareMap.appContext.getPackageName());
        final View relativeLayout = ((Activity) hardwareMap.appContext).findViewById(relativeLayoutId);

        // wait for the start button to be pressed
        waitForStart();

        // loop & read RGB data
        while (opModeIsActive()) {
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

            // send info back to driver station using telemetry
            telemetry.addData("Alpha", sensorColor.alpha());
            telemetry.addData("Red  ", sensorColor.red());
            telemetry.addData("Green", sensorColor.green());
            telemetry.addData("Blue ", sensorColor.blue());
            telemetry.addData("Hue", hsvValues[0]);
            telemetry.addData("Object", object);

            // change background color to match color detected by the RGB sensor
            relativeLayout.post(new Runnable() {
                public void run() {
                    relativeLayout.setBackgroundColor(Color.HSVToColor(0xff, values));
                }
            });

            telemetry.update();

        }

        // Set panel back to default color
        relativeLayout.post(new Runnable() {
            public void run() {
                relativeLayout.setBackgroundColor(Color.WHITE);
            }
        });
    }

}
