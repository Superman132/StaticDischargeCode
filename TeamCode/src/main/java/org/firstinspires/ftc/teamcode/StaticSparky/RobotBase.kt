package org.firstinspires.ftc.teamcode.StaticSparky

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.kinematics.Kinematics
import com.acmerobotics.roadrunner.localization.Localizer
import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.Controllers.DriveTrain
import org.firstinspires.ftc.teamcode.hardware.general.Gyro
import org.firstinspires.ftc.teamcode.hardware.general.Motor
import org.firstinspires.ftc.teamcode.localizers.MecanumLocalizerRev
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

open class RobotBase(val hardwareMap: HardwareMap, val telemetry: Telemetry) {
    var driveTrain: DriveTrain
    var localizer: Localizer
    val gyro: Gyro
    var pose: Pose2d = Pose2d(0.0, 0.0, 0.0)

    init {
        val rf = Motor("rf", 1120.0, 1.0, 2.95, hardwareMap)
        val rb = Motor("rb", 1120.0, 1.0, 2.95, hardwareMap)
        val lf = Motor("lf", 1120.0, 1.0, 2.95, hardwareMap)
        val lb = Motor("lb", 1120.0, 1.0, 2.95, hardwareMap)

        driveTrain = DriveTrain(rf, rb, lf, lb)

        gyro = Gyro("gyro", hardwareMap)


        localizer = MecanumLocalizerRev(hardwareMap, gyro = gyro)


        val allHubs = hardwareMap.getAll(LynxModule::class.java)
        for (module in allHubs) {
            module.bulkCachingMode = LynxModule.BulkCachingMode.AUTO
        }
    }

    fun move(hori: Double, vert: Double) {
        telemetry.addData("Encoders", "moving")
        telemetry.addData("Horizontal", hori)
        telemetry.addData("Vertical", vert)
        telemetry.update()

        driveTrain.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER)
        driveTrain.setTarget(DriveTrain.Direction(hori, -vert, 0.0).speeds())
        driveTrain.setMode(DcMotor.RunMode.RUN_TO_POSITION)

        val allDrive = DriveTrain.Square(0.8, 0.8, 0.8, 0.8)

        driveTrain.start(allDrive)

        val orientation = gyro.measure()


        // do gyro adjustment         |
        //                            v
        while (driveTrain.isBusy && !Thread.interrupted()) {
            localizer.update()

            val turnSquare = if (abs(headingError(orientation)) > 0.02) {
                DriveTrain.Vector(0.0, 0.0, turnCorrection(orientation)).speeds()
            } else {
                DriveTrain.Square(0.0, 0.0, 0.0, 0.0)
            }

            driveTrain.start(DriveTrain.addSquares(allDrive, turnSquare))
        }
        //turn (-headingError(angle)) * gyroAdjust)
        val robotPoseDelta = Pose2d(vert, -hori, 0.0)
        pose = Kinematics.relativeOdometryUpdate(pose, robotPoseDelta)
        driveTrain.start(DriveTrain.Square(0.0, 0.0, 0.0, 0.0))
        driveTrain.setMode(DcMotor.RunMode.RUN_USING_ENCODER)
    }

    fun turnTo(degrees: Double) {
        while (abs(headingError(degrees / 360)) > 0.02 && !Thread.interrupted()) {
            localizer.update()

            telemetry.addData("Gyro Sensor", "turning")
            telemetry.addData("Angle", gyro.measure())
            telemetry.update()

            val turn = turnCorrection(degrees)
            driveTrain.start(DriveTrain.Vector(0.0, 0.0, turn).speeds())
        }

        pose = Pose2d(pose.vec(), gyro.measure() * 2 * PI)

        driveTrain.start(DriveTrain.Vector(0.0, 0.0, 0.0).speeds())
    }

    fun toGoal(goalPose: Pose2d) {
        val error = Kinematics.calculatePoseError(goalPose, pose)
        this.move(-error.y, error.x)
        this.turnTo((error.heading + gyro.measure() * 2 * PI) * 180 / PI)
    }

    fun turnCorrection(orientation: Double): Double {
        val rawError = headingError(orientation)

        return rawError * ((1 - turnStatic) / 0.5) + (turnStatic * sign(rawError))
    }

    fun headingError(orientation: Double): Double {
        var rawError = orientation - gyro.measure()
        if (rawError < -0.5) {
            rawError += 1.0
        }
        if (rawError > 0.5) {
            rawError -= 1.0
        }
        return rawError
    }

    companion object {
        const val turnStatic = 0.2
    }

    fun setDriveTrain(rf: Motor, rb: Motor, lf: Motor, lb: Motor) {
        driveTrain = DriveTrain(rf, rb, lf, lb)
    }
}