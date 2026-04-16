package org.firstinspires.ftc.teamcode.testing;

//import com.acmerobotics.dashboard.FtcDashboard;
//import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

// @Config
@TeleOp(name = "Kickstand Test")
public class KickstandTest extends LinearOpMode {

    public static double START_POS = 0.0;
    public static double END_POS = 0.3;
    private Servo kickstand;

    @Override
    public void runOpMode() {
        kickstand = hardwareMap.get(Servo.class, "kickstand");

        waitForStart();

        while (opModeIsActive()) {
            // test code

//            kickstand.setPosition("start", START_POS);
//
//            if (gamepad1.xWasPressed()) {
//                // tune value
//                kickstand.setPosition(0.1);
//            }
//            if (gamepad1.bWasPressed())
//                kickstand.setPosition(0.0);
        }
    }

    /*
    SO BASICALLY...
    Press X to "shoot"
    and then press B to reset
     */


}
