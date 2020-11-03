package org.firstinspires.ftc.teamcode.StaticSparky

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.Servo
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.teamcode.Controllers.Shooter
import org.firstinspires.ftc.teamcode.Controllers.shootingGoal
import org.firstinspires.ftc.teamcode.hardware.general.Motor
import org.firstinspires.ftc.teamcode.hardware.general.ServoM
import kotlin.math.PI

@TeleOp(name = "Testing TeleOP", group = "Static Discharge")
class TestingTele: OpMode() {
    lateinit var flywheel: Motor
    lateinit var arm: ServoM
    lateinit var claw: ServoM
    lateinit var shoot: Shooter
    override fun init() {
        flywheel = Motor("flywheel", 1120.0, 17.36,4.0, hardwareMap)
        arm = ServoM("arm", hardwareMap)
        claw = ServoM("claw", hardwareMap)
        shoot = Shooter(flywheel, 45*PI/180)
//        telemetry.addData("pidf", flywheel.device.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER))
//        flywheel.device.setVelocityPIDFCoefficients(10.0, 3.0, 0.5, 0.0)
    }

    override fun loop() {
//        flywheel.start(gamepad1.left_stick_y.toDouble()*0.70)


        if (gamepad1.a) {
            arm.start(0.5)
        } else if(gamepad1.b) {
            arm.start(0.0)
        }
//        if (gamepad1.x) {
//            claw.start(0.5)
//        } else if(gamepad1.y) {
//            claw.start(0.0)
//        }
        if (gamepad1.x) {
//            flywheel.setSpeed(4.5*40*3.2)
            shoot.simpleShootAtTarget(Pose2d(0.0, 0.0, 0.0), shootingGoal(70.0, 0.0, 35.0-11.0))
            telemetry.addData("Rpm of motor shaft", flywheel.device.getVelocity(AngleUnit.RADIANS)*60/(2*PI))
        }

    }

}