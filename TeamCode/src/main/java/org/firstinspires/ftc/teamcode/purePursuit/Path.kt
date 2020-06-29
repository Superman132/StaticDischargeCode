package org.firstinspires.ftc.teamcode.purePursuit

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.acmerobotics.roadrunner.util.epsilonEquals
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.LUDecomposition
import kotlin.math.*

abstract class Path {
    abstract val start: Pose2d

    abstract val end: Pose2d

    abstract val length: Double

    abstract fun getPointfromT(t:Double): Pose2d

    abstract fun findClosestT(position:Pose2d): Double

    fun limit(value: Double, min: Double, max: Double): Double {
        return max(min, min(value, max))
    }
}

class LinearPath(override val start: Pose2d, override val end:Pose2d) :Path() {
    override val length = (end.vec()-start.vec()).norm()
    override fun getPointfromT(t: Double): Pose2d {
        val x = lerp(start.x, end.x, t)
        val y = lerp(start.y, end.y, t)
        val heading = lerpAngle(start.heading, end.heading, t)
        return Pose2d(x, y, heading)
    }

    override fun findClosestT(position: Pose2d): Double {
        val v = end.vec().minus(start.vec())
        val u = start.vec().minus(position.vec())

        val t = if (v.norm() == 0.0 && u.norm() == 0.0) {
            0.5
        } else if (v.norm() == 0.0) {
            (position.heading - start.heading) / (end.heading - start.heading)
        } else if (u.dot(u) epsilonEquals 0.0) {
            0.0
        } else {
            -v.dot(u) / u.dot(u)
        }
        return limit(t, 0.0, 1.0)
    }
}

class ArcPath(override val start: Pose2d, override val end:Pose2d, mid: Vector2d) :Path() {

    lateinit var center: Vector2d
    var radius: Double = 0.0
    var endAngle: Double = 0.0
    var beginAngle: Double = 0.0
    override var length = 0.0

    init {
        fromThreePoints(start.vec(), mid, end.vec())
        length = length()
    }


    override fun getPointfromT(t: Double): Pose2d {
        val ang = angleFromT(t)
        return Pose2d(center + Vector2d(cos(ang), sin(ang)).times(radius), lerpAngle(start.heading, end.heading, t))
    }

    override fun findClosestT(position: Pose2d): Double {
        val angle = (position.vec() - center).angle()
        return when {
            angle < beginAngle -> {
                0.0
            }
            angle < endAngle -> {
                (angle - beginAngle) / (endAngle - beginAngle)
            }
            else -> {
                1.0
            }
        }

    }

    private fun angleFromT(t: Double): Double {
        return lerp(beginAngle, endAngle, t)
    }

    fun length(): Double {
        return (endAngle - beginAngle).absoluteValue * radius
    }

    fun fromThreePoints(ptBegin: Vector2d, ptMid: Vector2d, ptEnd: Vector2d) {
        val denomMat = Array2DRowRealMatrix(arrayOf(
                doubleArrayOf(ptBegin.x, ptBegin.y, 1.0),
                doubleArrayOf(ptMid.x, ptMid.y, 1.0),
                doubleArrayOf(ptEnd.x, ptEnd.y, 1.0)
        ))
        val hNum = Array2DRowRealMatrix(arrayOf(
                doubleArrayOf(ptBegin.dot(ptBegin), ptBegin.y, 1.0),
                doubleArrayOf(ptMid.dot(ptMid), ptMid.y, 1.0),
                doubleArrayOf(ptEnd.dot(ptEnd), ptEnd.y, 1.0)))
        val denom = (2 * LUDecomposition(denomMat).determinant)
        val h = LUDecomposition(hNum).determinant / denom

        val kNum = Array2DRowRealMatrix(arrayOf(
                doubleArrayOf(ptBegin.x, ptBegin.dot(ptBegin), 1.0),
                doubleArrayOf(ptMid.x, ptMid.dot(ptMid), 1.0),
                doubleArrayOf(ptEnd.x, ptEnd.dot(ptEnd), 1.0)
        ))
        val k = LUDecomposition(kNum).determinant / denom

        val center = Vector2d(h, k)
        radius = center distTo (ptBegin)
        beginAngle = (ptBegin - center).angle()
        endAngle = (ptEnd - center).angle()
    }
}

class CubicSplinePath(override val start: Pose2d, override val end: Pose2d, startTangent: Double, val endTangent: Double):Path() {

    override val length:Double
    var lastT: Double? = null
    var xCoeffs : Coeffs
    var yCoeffs : Coeffs

    init {
        val derivMag = (start.vec() distTo end.vec())
        xCoeffs = calculateCoeffs(start.x, end.x, derivMag * cos(startTangent), derivMag * cos(endTangent))
        yCoeffs = calculateCoeffs(start.y, end.y, derivMag * sin(startTangent), derivMag * sin(endTangent))
        length = length(0.0, 1.0)
    }

    data class Coeffs(val a:Double, val b:Double, val c:Double, val d:Double)

    override fun getPointfromT(t: Double): Pose2d {
        val xPos = xCoeffs.a * (t * t * t) + xCoeffs.b * (t*t) + xCoeffs.c * t + xCoeffs.d

        val yPos = yCoeffs.a * (t * t * t) + yCoeffs.b * (t*t) + yCoeffs.c * t + yCoeffs.d

        val head = lerpAngle(start.heading, end.heading, t)

        return Pose2d(xPos, yPos, head)
    }

    fun DerivFromT(t: Double): Vector2d {
        val xPos = 3 * xCoeffs.a * (t * t) + 2 * xCoeffs.b * (t) + xCoeffs.c

        val yPos = 3 * yCoeffs.a * (t * t) + 2 * yCoeffs.b * (t) + yCoeffs.c

        return Vector2d(xPos, yPos)
    }

    fun SecondDerivFromT(t: Double): Vector2d {
        val xPos = 6 * xCoeffs.a * (t) + (2 * xCoeffs.b)

        val yPos = 6 * yCoeffs.a * (t) + (2 * yCoeffs.b)

        return Vector2d(xPos, yPos)
    }

    override fun findClosestT(position: Pose2d): Double {
        var minDist:Double = Double.MAX_VALUE
        var minT = 0.0
        var UpperT:Double
        var LowerT:Double

        if (lastT != null) {
            UpperT  = lastT!! + 0.20
            LowerT = lastT!! - 0.10
        } else {
            UpperT = 1.0
            LowerT = 0.0
        }


        for (i in 0..5) {
            val testT = lerp(LowerT, UpperT, i/5.0)
            val newDist = (position.vec() - getPointfromT(testT).vec()).norm()
            if (newDist < minDist) {
                minDist = newDist
                minT = testT
                UpperT = lerp(LowerT, UpperT, (i + 0.5) / 5.0)
                LowerT = lerp(LowerT, UpperT,(i - 0.5) / 5.0)
            }
        }

        for (i in 0..5) {
            val testT = lerp(LowerT, UpperT, i/5.0)
            val newDist = (position.vec() - getPointfromT(testT).vec()).norm()
            if (newDist < minDist) {
                minDist = newDist
                minT = testT
                UpperT = lerp(LowerT, UpperT, (i + 0.5) / 5.0)
                LowerT = lerp(LowerT, UpperT,(i - 0.5) / 5.0)
            }
        }

        lastT = minT

        return minT
    }


    fun calculateCoeffs(start:Double, end:Double, startDeri:Double, endDeri:Double): Coeffs {
        val c = startDeri
        val d = start
        val AandB = end - c - d
        val a = endDeri - (2 * AandB) - c
        val b = AandB - a
        return Coeffs(a, b, c, d)
    }

    private fun approxLength(v1: Vector2d, v2: Vector2d, v3: Vector2d): Double {
        val w1 = (v2 - v1) * 2.0
        val w2 = (v2 - v3) * 2.0
        val det = w1.x * w2.y - w2.x * w1.y
        val chord = v1 distTo v3
        return if (det epsilonEquals 0.0) {
            chord
        } else {
            val x1 = v1.x * v1.x + v1.y * v1.y
            val x2 = v2.x * v2.x + v2.y * v2.y
            val x3 = v3.x * v3.x + v3.y * v3.y

            val y1 = x2 - x1
            val y2 = x2 - x3

            val origin = Vector2d(y1 * w2.y - y2 * w1.y, y2 * w1.x - y1 * w2.x) / det
            val radius = origin distTo v1
            2.0 * radius * asin(chord / (2.0 * radius))
        }
    }

    private fun internalCurvature(t: Double): Double {
        val deriv = DerivFromT(t)
        val derivNorm = deriv.norm()
        val secondDeriv = SecondDerivFromT(t)
        return abs(secondDeriv.x * deriv.y - deriv.x * secondDeriv.y) / (derivNorm * derivNorm * derivNorm)
    }

    private fun length(tLo: Double,
                       tHi: Double,
                       vLo: Vector2d = getPointfromT(tLo).vec(),
                       vHi: Vector2d = getPointfromT(tHi).vec(),
                       depth:Int = 0):Double {
        var runningLength = 0.0
        if (depth >= 10) {
            return (vHi - vLo).norm()
        }

        val tMid = 0.5 * (tLo + tHi)
        val vMid = getPointfromT(tMid).vec()

        val deltaK = abs(internalCurvature(tLo) - internalCurvature(tHi))
        val segmentLength = approxLength(vLo, vMid, vHi)

        if (deltaK > 0.1 || segmentLength > 0.25) {
            runningLength += length(tLo, tMid, vLo, vMid, depth + 1)
            runningLength += length(tMid, tHi, vMid, vHi, depth + 1)
        } else {
            runningLength += segmentLength
        }

        return runningLength
    }


}

