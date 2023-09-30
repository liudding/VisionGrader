/**
    Copyright 2020 ZynkSoftware SRL

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
    associated documentation files (the "Software"), to deal in the Software without restriction,
    including without limitation the rights to use, copy, modify, merge, publish, distribute,
    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or
    substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
    INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
    NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
    DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.linkstar.visiongrader.common.utils

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import com.linkstar.visiongrader.common.extensions.scaleRectangle
import com.linkstar.visiongrader.common.extensions.toBitmap
import com.linkstar.visiongrader.common.extensions.toMat
import com.linkstar.visiongrader.ui.components.Quadrilateral
import com.linkstar.visiongrader.utils.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*


internal class OpenCvNativeBridge {

    companion object {
        private const val ANGLES_NUMBER = 4
        private const val EPSILON_CONSTANT = 0.02
        private const val CLOSE_KERNEL_SIZE = 10.0
        private const val CANNY_THRESHOLD_LOW = 75.0
        private const val CANNY_THRESHOLD_HIGH = 200.0
        private const val CUTOFF_THRESHOLD = 155.0
        private const val TRUNCATE_THRESHOLD = 150.0
        private const val NORMALIZATION_MIN_VALUE = 0.0
        private const val NORMALIZATION_MAX_VALUE = 255.0
        private const val BLURRING_KERNEL_SIZE = 5.0
        private const val DOWNSCALE_IMAGE_SIZE = 600.0
        private const val FIRST_MAX_CONTOURS = 10
    }

    fun getScannedBitmap(bitmap: Bitmap, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float): Bitmap {
        val rectangle = MatOfPoint2f()
        rectangle.fromArray(
            Point(x1.toDouble(), y1.toDouble()),
            Point(x2.toDouble(), y2.toDouble()),
            Point(x3.toDouble(), y3.toDouble()),
            Point(x4.toDouble(), y4.toDouble())
        )
        val dstMat = PerspectiveTransformation.transform(bitmap.toMat(), rectangle)
        return dstMat.toBitmap()
    }

    fun getScanned(mat: Mat, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float): Mat {
        val rectangle = MatOfPoint2f()
        rectangle.fromArray(
            Point(x1.toDouble(), y1.toDouble()),
            Point(x2.toDouble(), y2.toDouble()),
            Point(x3.toDouble(), y3.toDouble()),
            Point(x4.toDouble(), y4.toDouble())
        )
        return PerspectiveTransformation.transform(mat, rectangle)
    }

    fun getContourEdgePoints(tempBitmap: Bitmap): List<PointF> {
        var point2f = getPoint(tempBitmap)
        if (point2f == null) point2f = MatOfPoint2f()
        val points: List<Point> = point2f.toArray().toList()
        val result: MutableList<PointF> = ArrayList()
        for (i in points.indices) {
            result.add(PointF(points[i].x.toFloat(), points[i].y.toFloat()))
        }

        return result
    }

    fun getPoint(bitmap: Bitmap): MatOfPoint2f? {
        val src = bitmap.toMat()

        val ratio = DOWNSCALE_IMAGE_SIZE / max(src.width(), src.height())
        val downscaledSize = Size(src.width() * ratio, src.height() * ratio)
        val downscaled = Mat(downscaledSize, src.type())
        Imgproc.resize(src, downscaled, downscaledSize)
        val largestRectangle = detectLargestQuadrilateral(downscaled)

        return largestRectangle?.contour?.scaleRectangle(1f / ratio)
    }


    // patch from Udayraj123 (https://github.com/Udayraj123/LiveEdgeDetection)
    fun detectLargestQuadrilateral(src: Mat): Quadrilateral? {
        val scansDir = Utils.getExternalFilesDir("scans")
        Log.d("cv", scansDir)

        val destination = Mat()
        Imgproc.blur(src, src, Size(BLURRING_KERNEL_SIZE, BLURRING_KERNEL_SIZE))

        Core.normalize(src, src, NORMALIZATION_MIN_VALUE, NORMALIZATION_MAX_VALUE, Core.NORM_MINMAX)
        Imgproc.threshold(src, src, TRUNCATE_THRESHOLD, NORMALIZATION_MAX_VALUE, Imgproc.THRESH_TRUNC)
        Core.normalize(src, src, NORMALIZATION_MIN_VALUE, NORMALIZATION_MAX_VALUE, Core.NORM_MINMAX)

        Imgproc.Canny(src, destination, CANNY_THRESHOLD_HIGH, CANNY_THRESHOLD_LOW)

        Imgproc.threshold(destination, destination, CUTOFF_THRESHOLD, NORMALIZATION_MAX_VALUE, Imgproc.THRESH_TOZERO)
        Utils.saveImage(destination.toBitmap(), Utils.joinPath(scansDir, "ttt2.png"))
        Imgproc.morphologyEx(
            destination, destination, Imgproc.MORPH_CLOSE,
            Mat(Size(CLOSE_KERNEL_SIZE, CLOSE_KERNEL_SIZE), CvType.CV_8UC1, Scalar(
                NORMALIZATION_MAX_VALUE
            )),
            Point(-1.0, -1.0), 1
        )

        Utils.saveImage(destination.toBitmap(), Utils.joinPath(scansDir, "ttt3.png"))


        val largestContour: List<MatOfPoint>? = findLargestContours(destination)
        if (null != largestContour) {
            return findQuadrilateral(largestContour)
        }
        return null
    }

    private fun findQuadrilateral(mContourList: List<MatOfPoint>): Quadrilateral? {
        for (c in mContourList) {
            val c2f = MatOfPoint2f(*c.toArray())
            val peri = Imgproc.arcLength(c2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(c2f, approx, EPSILON_CONSTANT * peri, true)
            val points = approx.toArray()
            // select biggest 4 angles polygon
            if (approx.rows() == ANGLES_NUMBER) {
                val foundPoints: Array<Point> = sortPoints(points)
                return Quadrilateral(approx, foundPoints)
            } else if(approx.rows() == 5) {
                // if document has a bent corner
                var shortestDistance = Int.MAX_VALUE.toDouble()
                var shortestPoint1: Point? = null
                var shortestPoint2: Point? = null

                var diagonal = 0.toDouble()
                var diagonalPoint1: Point? = null
                var diagonalPoint2: Point? = null

                for (i in 0 until 4) {
                    for (j in i + 1 until 5) {
                        val d = distance(points[i], points[j])
                        if (d < shortestDistance) {
                            shortestDistance = d
                            shortestPoint1 = points[i]
                            shortestPoint2 = points[j]
                        }
                        if(d > diagonal) {
                            diagonal = d
                            diagonalPoint1 = points[i]
                            diagonalPoint2 = points[j]
                        }
                    }
                }

                val trianglePointWithHypotenuse: Point? = points.toList().minus(arrayListOf(shortestPoint1, shortestPoint2, diagonalPoint1, diagonalPoint2))[0]

                val newPoint = if(trianglePointWithHypotenuse!!.x > shortestPoint1!!.x && trianglePointWithHypotenuse.x > shortestPoint2!!.x &&
                    trianglePointWithHypotenuse.y > shortestPoint1.y && trianglePointWithHypotenuse.y > shortestPoint2.y) {
                    Point(min(shortestPoint1.x, shortestPoint2.x), min(shortestPoint1.y, shortestPoint2.y))
                } else if(trianglePointWithHypotenuse.x < shortestPoint1.x && trianglePointWithHypotenuse.x < shortestPoint2!!.x &&
                    trianglePointWithHypotenuse.y > shortestPoint1.y && trianglePointWithHypotenuse.y > shortestPoint2.y) {
                    Point(max(shortestPoint1.x, shortestPoint2.x), min(shortestPoint1.y, shortestPoint2.y))
                } else if(trianglePointWithHypotenuse.x < shortestPoint1.x && trianglePointWithHypotenuse.x < shortestPoint2!!.x &&
                        trianglePointWithHypotenuse.y < shortestPoint1.y && trianglePointWithHypotenuse.y < shortestPoint2.y) {
                     Point(max(shortestPoint1.x, shortestPoint2.x), max(shortestPoint1.y, shortestPoint2.y))
                } else if(trianglePointWithHypotenuse.x > shortestPoint1.x && trianglePointWithHypotenuse.x > shortestPoint2!!.x &&
                    trianglePointWithHypotenuse.y < shortestPoint1.y && trianglePointWithHypotenuse.y < shortestPoint2.y) {
                     Point(min(shortestPoint1.x, shortestPoint2.x), max(shortestPoint1.y, shortestPoint2.y))
                } else {
                    Point(0.0, 0.0)
                }

                val sortedPoints = sortPoints(arrayOf(trianglePointWithHypotenuse, diagonalPoint1!!, diagonalPoint2!!, newPoint))
                val newApprox = MatOfPoint2f()
                newApprox.fromArray(*sortedPoints)
                return Quadrilateral(newApprox, sortedPoints)
            }
        }
        return null
    }

    private fun distance(p1: Point, p2: Point): Double {
        return sqrt((p1.x - p2.x).pow(2.0) + (p1.y - p2.y).pow(2.0))
    }

    private fun sortPoints(src: Array<Point>): Array<Point> {
        val srcPoints: ArrayList<Point> = ArrayList(src.toList())
        val result = arrayOf<Point?>(null, null, null, null)
        val sumComparator: Comparator<Point> = Comparator<Point> { lhs, rhs -> (lhs.y + lhs.x).compareTo(rhs.y + rhs.x) }
        val diffComparator: Comparator<Point> = Comparator<Point> { lhs, rhs -> (lhs.y - lhs.x).compareTo(rhs.y - rhs.x) }

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator)
        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator)
        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator)
        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator)
        return result.map {
            it!!
        }.toTypedArray()
    }

    private fun findLargestContours(inputMat: Mat): List<MatOfPoint>? {
        val mHierarchy = Mat()
        val mContourList: List<MatOfPoint> = ArrayList()
        //finding contours - as we are sorting by area anyway, we can use RETR_LIST - faster than RETR_EXTERNAL.
        Imgproc.findContours(inputMat, mContourList, mHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        // Convert the contours to their Convex Hulls i.e. removes minor nuances in the contour
        val mHullList: MutableList<MatOfPoint> = ArrayList()
        val tempHullIndices = MatOfInt()
        for (i in mContourList.indices) {
            Imgproc.convexHull(mContourList[i], tempHullIndices)
            mHullList.add(hull2Points(tempHullIndices, mContourList[i]))
        }
        // Release mContourList as its job is done
        for (c in mContourList) {
            c.release()
        }
        tempHullIndices.release()
        mHierarchy.release()
        if (mHullList.size != 0) {
            mHullList.sortWith { lhs, rhs ->
                Imgproc.contourArea(rhs).compareTo(Imgproc.contourArea(lhs))
            }
            return mHullList.subList(0, min(mHullList.size, FIRST_MAX_CONTOURS))
        }
        return null
    }

    private fun hull2Points(hull: MatOfInt, contour: MatOfPoint): MatOfPoint {
        val indexes = hull.toList()
        val points: MutableList<Point> = ArrayList()
        val ctrList = contour.toList()
        Log.d("cv", "" + ctrList.size + "  " + indexes.toString())

        for (index in indexes) {
            points.add(ctrList[index])
        }
        val point = MatOfPoint()
        point.fromList(points)
        return point
    }

    fun contourArea(approx: MatOfPoint2f): Double {
        return Imgproc.contourArea(approx)
    }


    fun splitPagesBySpine(src: Mat): Pair<Mat, Mat> {
        val width = src.size().width.toInt()
        val height = src.size().height.toInt()

        // Calculate the vertical middle line
        val middleLine = width / 2

        // Define regions of interest (ROI) for left and right halves
        val leftRoi = Rect(0, 0, middleLine, height)
        val rightRoi = Rect(middleLine, 0, width - middleLine, height)

        // Extract left and right halves from the original image
        val leftHalf = Mat(src, leftRoi)
        val rightHalf = Mat(src, rightRoi)

        return Pair(leftHalf, rightHalf)
    }


    fun detectSpine(src: Mat) {
        val destination = Mat()
        Imgproc.blur(src, src, Size(BLURRING_KERNEL_SIZE, BLURRING_KERNEL_SIZE))

        Core.normalize(src, src, NORMALIZATION_MIN_VALUE, NORMALIZATION_MAX_VALUE, Core.NORM_MINMAX)

        Imgproc.threshold(src, src, TRUNCATE_THRESHOLD, NORMALIZATION_MAX_VALUE, Imgproc.THRESH_TRUNC)
        Core.normalize(src, src, NORMALIZATION_MIN_VALUE, NORMALIZATION_MAX_VALUE, Core.NORM_MINMAX)

        val lines = Mat()
//        Imgproc.HoughLinesP(destination, lines, 1.0, Math.PI / 2, threshold, minLineLength, maxLineGap)
    }

    fun processRealMagic(input: Mat) {
        val maxdim = input.cols() //std::max(input.rows,input.cols);
        val dim = 1024
        if (maxdim > dim) {
            val scale = dim.toDouble() / maxdim.toDouble()
            val t = Mat()
            Imgproc.resize(input, input, Size(), scale, scale)
        }

        val localmax = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(15.0, 15.0))

        Imgproc.medianBlur(input, localmax, 5)
        Imgproc.morphologyEx(localmax, localmax, Imgproc.MORPH_CLOSE, kernel, Point(-1.0, -1.0), 1, Core.BORDER_REFLECT101)

        val bb: List<Rect> = detectLetters(input) // Assuming detectLetters is defined and returns List<Rect>

        val mask = Mat(input.size(), CvType.CV_8UC1, Scalar(0.0))
        if (bb.isEmpty()) {
            return // TODO: none found
        }

        for (i in bb) {
            Imgproc.rectangle(mask, i.tl(), i.br(), Scalar(1.0), -1)
        }

        val mean = MatOfDouble()
        val dev = MatOfDouble()
        Core.meanStdDev(localmax, mean, dev, mask)

        val minimum = DoubleArray(3)
        val maximum = DoubleArray(3)

        for (u in 0 until 3) {
            minimum[u] = mean.get(u, 0)[0] - dev.get(u, 0)[0]
            maximum[u] = mean.get(u, 0)[0] + dev.get(u, 0)[0]
        }

        for (y in 0 until mask.rows()) {
            for (x in 0 until mask.cols()) {
                val col = localmax[y, x]
                for (u in 0 until 3) {
                    if (col[u] > maximum[u])
                        col[u] = maximum[u]
                    else if (col[u] < minimum[u])
                        col[u] = minimum[u]
                }
            }
        }

        for (y in 0 until input.rows()) {
            for (x in 0 until input.cols()) {
                val v1 = input[y, x]
                val v2 = localmax[y, x]
                val v3 = input[y, x]
                for (i in 0 until 3) {
                    val gain = 255.0 / v2[i]
                    v3[i] = (gain * v1[i])
                }
            }
        }
    }

    private fun detectLetters(img: Mat): List<Rect> {
        val boundRect = mutableListOf<Rect>()
        val workspace = Mat()
        val element = Mat()
        Imgproc.cvtColor(img, workspace, Imgproc.COLOR_BGR2GRAY)
        Imgproc.Sobel(workspace, workspace, CvType.CV_8U, 1, 0, 3, 1.0, 0.0, Core.BORDER_DEFAULT)
        Imgproc.threshold(workspace, workspace, 0.0, 255.0, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY)
        element.setTo(Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(17.0, 3.0)))
        Imgproc.morphologyEx(workspace, workspace, Imgproc.MORPH_CLOSE, element)
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(workspace, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE)

//        val contoursPoly = List(contours.size) { MatOfPoint2f() }

        for (i in 0 until contours.size) {
            if (contours[i].size().height > 100) {
                val poly = MatOfPoint2f()
                Imgproc.approxPolyDP(MatOfPoint2f(*contours[i].toArray()), poly, 3.0, true)
                val appRect = Imgproc.boundingRect(poly)
                if (appRect.width > appRect.height) {
                    boundRect.add(appRect)
                }
            }
        }

        return boundRect
    }
}