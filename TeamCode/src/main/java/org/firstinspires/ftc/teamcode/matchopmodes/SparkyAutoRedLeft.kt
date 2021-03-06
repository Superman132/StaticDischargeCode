package org.firstinspires.ftc.teamcode.matchopmodes

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.firstinspires.ftc.teamcode.controllers.ShootingGoal
import org.firstinspires.ftc.teamcode.UltimateGoalPositions
import org.firstinspires.ftc.teamcode.cv.RingPipeline
import org.firstinspires.ftc.teamcode.robotconfigs.SparkyV2Robot
import kotlin.math.PI


@Autonomous(name = "SparkyAutoRedLeft", group = "StaticDischarge")
class SparkyAutoRedLeft : GenericOpModeBase() {

    private lateinit var robot: SparkyV2Robot
    private val shootingPositionHighGoal = Pose2d(x = -0.3 * TILE_LENGTH, y = UltimateGoalPositions.highGoalRed.y - 7.0, PI - Math.toRadians(2.75))
    val shootingPositionPowerShots = Vector2d(x = -0.20 * TILE_LENGTH, y = UltimateGoalPositions.highGoalRed.y)
    val powerShotAngleAdjustment = Math.toRadians(-3.0)

    override fun runOpMode() {

        /*
        **************
        * also, use the ftc dashboard for path and robot tracking, not the telemetry
        * 192.168.43.1/dash on the same wifi
        **************
        */

        robot = SparkyV2Robot(hardwareMap, telemetry) { opModeIsActive() && !isStopRequested }
        robot.pursuiter.startAt(UltimateGoalPositions.startLeftRed)

        setUpPipelineUltimateGoal(right = true)
        initCV()
        startCV()


        var analysis: RingPipeline.RingPosition = RingPipeline.RingPosition.NONE

        while (!isStarted) {
            if (isStopRequested) {
                return
            }

            analysis = (pipeline as RingPipeline).position()
            telemetry.addData("analysis", analysis)
            telemetry.update()
        }

        val goalZone = when (analysis) {
            RingPipeline.RingPosition.NONE -> {
                Pose2d(UltimateGoalPositions.aZoneRed, 0.0)
            }
            RingPipeline.RingPosition.ONE -> {
                Pose2d(UltimateGoalPositions.bZoneRed, 0.0)
            }
            else -> {
                Pose2d(UltimateGoalPositions.cZoneRed, 0.0)
            }
        }

        //start button has been hit
        // main auto code begins
        stopCV()


        // WARM UP SHOOTER EARLY
        robot.pursuiter.action { robot.shooter.aimShooter(Pose2d(-0.20 * TILE_LENGTH, UltimateGoalPositions.powerFarRed.y, PI), UltimateGoalPositions.powerFarRed) }

        // INTERMEDIATE POINT SO WE DON'T HIT RING STACK
        robot.pursuiter.move(-0.75 * TILE_LENGTH, -0.3 * TILE_LENGTH, 0.0)


        /* HIGH GOAL */

        //shoots 3 rings

        shootHighGoals(robot, shootingPositionHighGoal, 3)

        robot.pursuiter.action {
            robot.shooter.stopWheel()
        }

        /* FIRST WOBBLE */
        dropFirstWobble(robot, goalZone)


        //split path based on number of rings in stack
        // 0 or 4 -> stop the shooter and continue
        // 1 -> intake and shoot it, then stop the shooter
        when (analysis) {
            RingPipeline.RingPosition.ONE -> {

                robot.pursuiter.action {
                    robot.intake.setForward()
                    robot.intake.run()
                }

                robot.pursuiter.spline(end = Pose2d(-TILE_LENGTH  + 10, -TILE_LENGTH * 1.7, PI), endTanAngle = PI, startTanAngle = PI)



                robot.pursuiter.spline(end = Pose2d(-TILE_LENGTH - 2, -TILE_LENGTH * 1.7, PI), endTanAngle = PI, startTanAngle = PI)

                getSecondWobble(robot)

                robot.pursuiter.action {
                    robot.intake.off()
                    robot.intake.run()
                }

                shootHighGoals(robot, shootingPositionHighGoal, 1)

                robot.pursuiter.action {
                    robot.shooter.stopWheel()
                }

                dropSecondWobble(robot, goalZone)
            }

            RingPipeline.RingPosition.FOUR -> {

//            get close to stack

                robot.pursuiter.spline(end = Pose2d(-TILE_LENGTH + 15, -TILE_LENGTH * 2.5, PI/2), endTanAngle = PI, startTanAngle = PI)

                //start intake

                robot.pursuiter.action {
                    robot.pursuiter.runSpeed = 0.7
                    robot.intake.setForward()
                    robot.intake.run()
                }

                //run over rings

                robot.pursuiter.spline(Pose2d(-TILE_LENGTH - 3, -TILE_LENGTH * 1.9, PI/2), PI, PI*1/2)

                //turn off intake

                robot.pursuiter.action {
                    robot.pursuiter.runSpeed = 0.95
                }

                getSecondWobble(robot)

                robot.pursuiter.action {
                    robot.intake.off()
                    robot.intake.run()
                }

                shootHighGoals(robot, shootingPositionHighGoal, 3)

                robot.pursuiter.action {
                    robot.shooter.stopWheel()
                }

                dropSecondWobble(robot, goalZone)
            }
            else -> {
                /* SECOND WOBBLE */
                getSecondWobble(robot)
                dropSecondWobble(robot, goalZone)
            }
        }


        /* PARK */

        robot.pursuiter.move(0.5 * TILE_LENGTH, -1 * TILE_LENGTH, 0.0)


        // the entire auto program above is dynamically building the path and actions
        // the line below does all the activity
        robot.pursuiter.follow(robot.driveTrain, telemetry = telemetry)

        //record position for teleop
        robot.savePose()

    }
}

/* POWER SHOTS */
fun shootPowerShots(robot: SparkyV2Robot, shootingPosition: Vector2d, angleAdjustment: Double) {
    robot.pursuiter
            .move(Pose2d(shootingPosition, angleAdjustment + robot.shooter.turningTarget(shootingPosition, UltimateGoalPositions.powerNearRed)))
            .action {
                robot.shooter.aimShooter(robot.localizer.poseEstimate,
                        UltimateGoalPositions.powerNearRed)
                Thread.sleep(350)
                robot.shooter.shoot()
            }

    robot.pursuiter
            .turnTo(angleAdjustment + robot.shooter.turningTarget(shootingPosition, UltimateGoalPositions.powerMidRed))
            .action {
                robot.shooter.aimShooter(robot.localizer.poseEstimate,
                        UltimateGoalPositions.powerMidRed)
                Thread.sleep(350)
                robot.shooter.shoot()
            }

    robot.pursuiter
            .turnTo(angleAdjustment + robot.shooter.turningTarget(shootingPosition, UltimateGoalPositions.powerFarRed))
            .action {
                robot.shooter.aimShooter(robot.localizer.poseEstimate,
                        UltimateGoalPositions.powerFarRed)
                Thread.sleep(500)
                robot.shooter.shoot()
            }
}

fun shootHighGoals(robot: SparkyV2Robot, shootingPosition: Pose2d, rings: Int) {
    if (rings == 0) {
        return
    }
    robot.pursuiter.move(shootingPosition)

    robot.pursuiter.action {
        robot.shooter.aimShooter(Pose2d(0.0, 0.0, 0.0), ShootingGoal(77.0, 0.0, 35.5))
        // first ring
        Thread.sleep(500)
        robot.shooter.shoot()
        // all other rings
        for (i in 1 until rings) {
            Thread.sleep(50)
            robot.shooter.shoot()
        }
    }
}

fun dropFirstWobble(robot: SparkyV2Robot, goalZone: Pose2d) {
    robot.pursuiter
            .move(goalZone + Pose2d(-5.0, 5.0, -PI / 4))
            .action { robot.arm.dropAuto() }
}

fun getSecondWobble(robot: SparkyV2Robot) {
    robot.pursuiter
            .move(-1.3 * GenericOpModeBase.TILE_LENGTH, -2.4 * GenericOpModeBase.TILE_LENGTH, PI)
            .action {
                robot.arm.run(0.95)
                Thread.sleep(120)
                robot.arm.run(0.0)
                robot.pursuiter.runSpeed *= 0.7
            }

    robot.pursuiter
            .move(-1.65 * GenericOpModeBase.TILE_LENGTH, -2.4 * GenericOpModeBase.TILE_LENGTH, PI)
            .action {

                robot.arm.grabAuto()
                robot.pursuiter.runSpeed *= 1/0.7
                //buck it to the target zone
            }
}

fun dropSecondWobble(robot: SparkyV2Robot, goalZone: Pose2d) {
    robot.pursuiter
            .move(goalZone + Pose2d(-14.0, 2.0, -Math.toRadians((10.0))))
            .action {
                robot.arm.dropTele()
                Thread.sleep(450)
            }
            .relative(0.0, -10.0, 0.0)
}