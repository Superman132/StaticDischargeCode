package org.firstinspires.ftc.teamcode.Controllers

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import org.firstinspires.ftc.teamcode.StaticSparky.SparkyRobot
import org.firstinspires.ftc.teamcode.hardware.general.Motor
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.tan

const val g = 386.088583 //  g in in/s^2

class Shooter(val flywheel: Motor, val shooterAngle:Double){
    val slip = 1.7 // flywheel shooter slip, MUST BE TUNED

    fun navShootAtTarget(robot: SparkyRobot, target: shootingGoal) {
        //start up flywheel at desired velocity and move robot to correct orientation
        // only use if odo and pure pursuit are working and tested
        val position = robot.localizer.poseEstimate.vec()
        val targetVector = Vector2d(target.x, target.y)
        val shotDistance = targetVector distTo position
        turnToTarget(robot, target)
        val requiredVelocity = Math.sqrt(g /2) * shotDistance/( cos(shooterAngle) * sqrt( shotDistance * tan(shooterAngle) - target.height))
        flywheel.setSpeed(2*requiredVelocity * slip) // remove 2 times if using double flywheel, doesnt account for direction

    }

    fun simpleShootAtTarget(pose: Pose2d, target: shootingGoal) {
        //start up flywheel at desired velocity
        //use if using basic move commands without pure pursuit
        val position = pose.vec()
        val targetVector = Vector2d(target.x, target.y)
        val shotDistance = targetVector distTo position
        val requiredVelocity = Math.sqrt(g /2) * shotDistance/( cos(shooterAngle) * sqrt( shotDistance * tan(shooterAngle) - target.height))
        flywheel.setSpeed(2*requiredVelocity * slip) // remove 2 times if using double flywheel, doesnt account for direction

    }

    fun turnToTarget(robot: SparkyRobot, target: shootingGoal) {
        val position = robot.localizer.poseEstimate.vec()
        val targetVector = Vector2d(target.x, target.y)
        val shootingHeading = targetVector.minus(position).angle()
        robot.pursuiter.addPoint(Pose2d(robot.localizer.poseEstimate.vec(), shootingHeading))
        robot.pursuiter.FollowSync(robot.driveTrain)
    }

    fun shoot() {
        //release chamber servo to let a ring into flywheel

    }
}


data class shootingGoal(val x:Double, val y:Double, val height:Double)