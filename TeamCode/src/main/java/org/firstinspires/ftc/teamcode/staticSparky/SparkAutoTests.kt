package org.firstinspires.ftc.teamcode.staticSparky

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.firstinspires.ftc.teamcode.Controllers.shootingGoal
import org.firstinspires.ftc.teamcode.robotConfigs.RobotBase
import org.firstinspires.ftc.teamcode.robotConfigs.SparkyRobot
import org.firstinspires.ftc.teamcode.robotConfigs.SparkyV2Robot

@Autonomous(name = "SparkyTests", group = "StaticDischarge")
class SparkAutoTests : SparkOpModeBase() {

    lateinit var robot: SparkyV2Robot

    override fun runOpMode() {
        // UNCOMMENT THIS IF SOUNDS ARE NEEDED
        robot = SparkyV2Robot(hardwareMap, telemetry) { opModeIsActive() && !isStopRequested() }
        waitForStart()


//        robot.arm.toAngle(0.0)
//        while (robot.arm.arm_motor.isBusy) {
//            telemetry.addLine(robot.arm.arm_motor.device.currentPosition.toString())
//            telemetry.addLine(robot.arm.arm_motor.device.targetPosition.toString())
//            telemetry.update()
//        }
//        robot.arm.run(0.0)
//        sleep(4000)
        robot.pursuiter.addAction { robot.shooter.simpleShootAtTarget(Pose2d(0.0, 0.0, 0.0), shootingGoal(70.0, 0.0, 36.0))
        sleep(3000)
        robot.shooter.shoot()
            sleep(1250)
            robot.shooter.shoot()
            sleep(1250)
            robot.shooter.shoot()}
        robot.pursuiter.FollowSync(robot.driveTrain, telemetry = telemetry)


    }
}
