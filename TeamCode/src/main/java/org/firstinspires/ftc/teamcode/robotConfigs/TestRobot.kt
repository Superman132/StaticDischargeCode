package org.firstinspires.ftc.teamcode.robotConfigs

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.hardware.general.Motor
import org.firstinspires.ftc.teamcode.purePursuit.FastPurePursuit
import org.firstinspires.ftc.teamcode.robotConfigs.RobotBase

class TestRobot(hardwareMap: HardwareMap, telemetry: Telemetry, opModeActive: () -> Boolean) : RobotBase(hardwareMap, telemetry, opModeActive) {
    // OpMode members

    val pursuiter: FastPurePursuit


    // initialize robot
    init {
        val rf = Motor("rf", 1120.0, 1.0, 2.95, hardwareMap)
        val rb = Motor("rb", 1120.0, 1.0, 2.95, hardwareMap)
        val lf = Motor("lf", 1120.0, 1.0, 2.95, hardwareMap)
        val lb = Motor("lb", 1120.0, 1.0, 2.95, hardwareMap)
        setDriveTrain(rf, rb, lf, lb)



//        localizer = TwoWheelRevLocalizer(hardwareMap, "front", "side")
        pursuiter = FastPurePursuit(localizer, Pose2d())
    }
}