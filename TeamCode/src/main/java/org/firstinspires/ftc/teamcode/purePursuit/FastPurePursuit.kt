package org.firstinspires.ftc.teamcode.purePursuit

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.control.PIDCoefficients
import com.acmerobotics.roadrunner.control.PIDFController
import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.kinematics.Kinematics
import com.acmerobotics.roadrunner.kinematics.MecanumKinematics
import com.acmerobotics.roadrunner.kinematics.TankKinematics
import com.acmerobotics.roadrunner.localization.Localizer
import org.firstinspires.ftc.teamcode.Controllers.DriveTrain
import kotlin.math.abs
import kotlin.math.sign


@Config
class FastPurePursuit(val localizer: Localizer, val startPose:Pose2d) {
    val waypoints: MutableList<Path> = mutableListOf()
    val actions: MutableList<Pair<Int, () -> Unit>> = mutableListOf()
    var index = 0

    private val lookAhead = 10.0 //Look Ahead Distance, 5 is arbitrary, depends on application and needs tuning, inches

    private val translationalTol = 0.2 //inches
    private val angularTol = Math.toRadians(1.0) // one degree angular tolerance
    private val kStatic = 0.20

    private val translationalCoeffs: PIDCoefficients = PIDCoefficients(0.07)
    private val headingCoeffs: PIDCoefficients = PIDCoefficients(0.07)

    private val axialController = PIDFController(translationalCoeffs)
    private val lateralController = PIDFController(translationalCoeffs)
    private val headingController = PIDFController(headingCoeffs)

    init {
        axialController.update(0.0)
        lateralController.update(0.0)
        headingController.update(0.0)
    }

    fun FollowSync(drivetrain: DriveTrain, mecanum:Boolean = true) {

        runAction(0)

        waypoints.add(0, LinearPath(localizer.poseEstimate, waypoints[0].start))

        var running = true

        while (!Thread.currentThread().isInterrupted && running) {
            running = followStep(drivetrain, mecanum)
        }
        this.waypoints.clear()
        index = 0
    }


    fun followStep(drivetrain: DriveTrain, mecanum:Boolean = true):Boolean {
        localizer.update()

        val path = waypoints[index]

        val currentPos = localizer.poseEstimate

        if (currentPos.vec() distTo waypoints[index].end.vec() < translationalTol &&
                abs(currentPos.heading - waypoints[index].end.heading) < angularTol) {
            // go to next waypoint
            runAction(index)
            if (index == waypoints.size-1) {
                return true
            } else {
                index += 1
                return false
            }
        }

        val target : Pose2d
        val candidateGoal = path.findClosestT(currentPos) + lookAhead/path.length


        target = if (candidateGoal > 1.0 && (actions.find { it.first == index } == null) && index < waypoints.size-1) {
            val excessLength = (path.findClosestT(currentPos) + lookAhead / path.length - 1.0) * path.length

            if (excessLength > lookAhead/2) {
                index += 1
                return false
            }

            waypoints[index+1].getPointfromT(limit(waypoints[index+1].length / excessLength, 0.0, 1.0))
        } else {
            path.getPointfromT(limit(candidateGoal, 0.0, 1.0))
        }


        if (mecanum) {
            val wheelVel = getWheelVelocityFromTarget(target = target, currentPos = currentPos)
            drivetrain.start(DriveTrain.Square(wheelVel[3], wheelVel[2], wheelVel[0], wheelVel[1]))
        } else {
            //TODO figure out how to make drivetrain more generic for tank and mecanum
//            val wheelVel = getWheelVelocityFromTargetTank(target, currentPos)
            print("Tank is still TODO, not fully implemented")
        }
        return false
    }

    fun addPoint(point: Pose2d): FastPurePursuit {
        if (waypoints.size > 0) {
            waypoints.add(LinearPath(waypoints.last().end, point))
        } else {
            waypoints.add(LinearPath(startPose, point))
        }
        return this
    }

    fun runAction(index:Int) {
        val action = actions.find { it.first == index }?.second
        if (action != null) action()
    }


    fun addPoint(x: Double, y: Double, heading: Double): FastPurePursuit {
        addPoint(Pose2d(x, y, heading))
        return this
    }

    fun setPose(start: Pose2d): FastPurePursuit{
        localizer.poseEstimate = start
        return this
    }

    fun addTurn(theta: Double): FastPurePursuit {
        val last = waypoints.last()
        waypoints.add(LinearPath(waypoints.last().end, Pose2d(last.end.vec(), last.end.heading + theta)))
        return this
    }

    fun addAction(action : () -> Unit): FastPurePursuit {
        actions.add(Pair(waypoints.size, action))
        return this
    }

    fun addSpline(end:Pose2d, startTanAngle: Double, endTanAngle:Double) : FastPurePursuit {
//        val start = waypoints.last().end

        val start = if (waypoints.size > 0) {
            waypoints.last().end
        } else {
            startPose
        }
        waypoints.add(CubicSplinePath(start, end, startTanAngle, endTanAngle))

        return this
    }

    fun addSpline(end:Pose2d, endTanAngle:Double) : FastPurePursuit {
        val startTan = if (waypoints.last() is CubicSplinePath) {
            val path = waypoints.last() as CubicSplinePath
            path.endTangent
        } else {
            (waypoints.last().end - waypoints.last().start).vec().angle()
        }
        return addSpline(end, startTan, endTanAngle)
    }

    fun getWheelVelocityFromTarget(target:Pose2d, currentPos:Pose2d): List<Double> {

        val error = Kinematics.calculatePoseError(target, currentPos)

        val velocity = errorToPower(error)

        print(velocity)

        var wheelPow = MecanumKinematics.robotToWheelVelocities(velocity, Constants.trackwidth, Constants.wheelBase, lateralMultiplier = 1.0)

        wheelPow = wheelPow.map { it + sign(it) * kStatic }

        val wheelCopy = wheelPow.map {abs(it)}

        if (wheelCopy.max() != null && wheelCopy.max()!! > 1) {
            wheelPow = wheelPow.map {it/wheelCopy.max()!!}
        }

        return wheelPow
    }

    fun errorToPower(poseError: Pose2d): Pose2d {
        axialController.targetPosition = poseError.x
        lateralController.targetPosition = poseError.y
        headingController.targetPosition = poseError.heading


        // note: feedforward is processed at the wheel level
        val axialCorrection = axialController.update(0.0)
        val lateralCorrection = lateralController.update(0.0)
        val headingCorrection = headingController.update(0.0)

        return Pose2d(
                axialCorrection,
                lateralCorrection,
                headingCorrection
        )
    }

    fun getWheelVelocityFromTargetTank(target:Pose2d, currentPos:Pose2d): List<Double> {

        val error = Kinematics.calculatePoseError(target, currentPos)

        val velocity = TankerrorToPower(error)

        var wheelPow = TankKinematics.robotToWheelVelocities(velocity, Constants.trackwidth)

        wheelPow = wheelPow.map { it + sign(it) * kStatic }

        val wheelCopy = wheelPow.map {abs(it)}

        if (wheelCopy.max() != null && wheelCopy.max()!! > 1) {
            wheelPow = wheelPow.map {it/wheelCopy.max()!!}
        }

        return wheelPow
    }

    // TODO edit using tank pidva follower from roadrunner
    fun TankerrorToPower(poseError: Pose2d): Pose2d {
        axialController.targetPosition = poseError.x
        lateralController.targetPosition = poseError.y
        headingController.targetPosition = poseError.heading


        // note: feedforward is processed at the wheel level
        val axialCorrection = axialController.update(0.0)
        val lateralCorrection = lateralController.update(0.0)
        val headingCorrection = headingController.update(0.0)

        return Pose2d(
                axialCorrection,
                lateralCorrection,
                headingCorrection
        )
    }
}