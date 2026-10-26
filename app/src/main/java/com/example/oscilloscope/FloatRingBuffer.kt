package com.example.oscilloscope

import kotlin.math.max
import kotlin.math.min

/**
 * High-performance circular ring buffer for storing up to [capacity] (e.g. 100,000) float samples.
 * Designed for zero GC object allocations on stream write and rapid Min-Max downsampling
 * during 60 FPS Canvas frame rendering.
 */
class FloatRingBuffer(val capacity: Int = 100_000) {

    private val buffer = FloatArray(capacity)
    @Volatile
    private var writeHead: Long = 0L // Monotonically increasing 64-bit index

    @Volatile
    var minVal: Float = Float.MAX_VALUE
        private set

    @Volatile
    var maxVal: Float = Float.MIN_VALUE
        private set

    @Volatile
    var latestVal: Float = 0f
        private set

    /**
     * Appends a single float point to the buffer.
     * Thread-safe, non-blocking, zero allocation.
     */
    @Synchronized
    fun append(value: Float) {
        val idx = (writeHead % capacity).toInt()
        buffer[idx] = value
        latestVal = value

        if (writeHead == 0L) {
            minVal = value
            maxVal = value
        } else {
            if (value < minVal) minVal = value
            if (value > maxVal) maxVal = value
        }

        writeHead++
    }

    /**
     * Bulk append of multiple float samples.
     */
    @Synchronized
    fun appendAll(values: FloatArray, count: Int = values.size) {
        for (i in 0 until min(count, values.size)) {
            val v = values[i]
            val idx = (writeHead % capacity).toInt()
            buffer[idx] = v
            latestVal = v
            if (writeHead == 0L) {
                minVal = v
                maxVal = v
            } else {
                if (v < minVal) minVal = v
                if (v > maxVal) maxVal = v
            }
            writeHead++
        }
    }

    /**
     * Total number of samples ever received.
     */
    val totalCount: Long
        get() = writeHead

    /**
     * Current valid samples stored in memory (capped at [capacity]).
     */
    val availableCount: Int
        get() = min(writeHead, capacity.toLong()).toInt()

    /**
     * Clears all buffer points and resets statistics.
     */
    @Synchronized
    fun clear() {
        writeHead = 0L
        minVal = Float.MAX_VALUE
        maxVal = Float.MIN_VALUE
        latestVal = 0f
    }

    /**
     * Retrieves the sample at global index [globalIndex].
     */
    fun getGlobal(globalIndex: Long): Float {
        if (globalIndex < 0 || globalIndex >= writeHead) return 0f
        if (writeHead - globalIndex > capacity) return 0f // Overwritten
        val idx = (globalIndex % capacity).toInt()
        return buffer[idx]
    }

    /**
     * Highly optimized Min-Max downsampling.
     * Takes the window of [pointCount] points ending at [endGlobalIndex],
     * divides it into [targetBuckets] (screen pixels), and writes
     * paired min and max values with normalized X progress into [outPoints].
     *
     * [outPoints] is a pre-allocated FloatArray where each point is stored as [xNorm, yVal].
     * Returns the number of points written to [outPoints].
     */
    fun downsample(
        endGlobalIndex: Long,
        pointCount: Int,
        targetBuckets: Int,
        outPoints: FloatArray
    ): Int {
        val total = writeHead
        if (total == 0L || pointCount <= 0 || targetBuckets <= 0) return 0

        val actualEnd = min(endGlobalIndex, total)
        val actualStart = max(0L, actualEnd - pointCount)
        val validPoints = (actualEnd - actualStart).toInt()
        if (validPoints <= 0) return 0

        // If data points are fewer than screen buckets, output directly
        if (validPoints <= targetBuckets) {
            var outIdx = 0
            for (i in 0 until validPoints) {
                if (outIdx + 1 >= outPoints.size) break
                val gIdx = actualStart + i
                val y = getGlobal(gIdx)
                val xNorm = i.toFloat() / max(1, validPoints - 1)
                outPoints[outIdx++] = xNorm
                outPoints[outIdx++] = y
            }
            return outIdx / 2
        }

        // Min-Max bucket compression (2 points per bucket: Min and Max)
        val bucketSize = validPoints.toDouble() / targetBuckets
        var outIdx = 0

        for (b in 0 until targetBuckets) {
            val bStart = actualStart + (b * bucketSize).toLong()
            val bEnd = min(actualEnd, actualStart + ((b + 1) * bucketSize).toLong())
            if (bStart >= bEnd) continue

            var bMin = Float.MAX_VALUE
            var bMax = -Float.MAX_VALUE
            var minOffset = 0
            var maxOffset = 0

            var currIdx = bStart
            var offset = 0
            while (currIdx < bEnd) {
                val v = getGlobal(currIdx)
                if (v < bMin) {
                    bMin = v
                    minOffset = offset
                }
                if (v > bMax) {
                    bMax = v
                    maxOffset = offset
                }
                currIdx++
                offset++
            }

            val bSpan = max(1, (bEnd - bStart).toInt())
            val xMinNorm = (bStart - actualStart + minOffset).toFloat() / validPoints
            val xMaxNorm = (bStart - actualStart + maxOffset).toFloat() / validPoints

            // Write in chronological order
            if (minOffset <= maxOffset) {
                if (outIdx + 3 < outPoints.size) {
                    outPoints[outIdx++] = xMinNorm
                    outPoints[outIdx++] = bMin
                    outPoints[outIdx++] = xMaxNorm
                    outPoints[outIdx++] = bMax
                }
            } else {
                if (outIdx + 3 < outPoints.size) {
                    outPoints[outIdx++] = xMaxNorm
                    outPoints[outIdx++] = bMax
                    outPoints[outIdx++] = xMinNorm
                    outPoints[outIdx++] = bMin
                }
            }
        }

        return outIdx / 2
    }
}
