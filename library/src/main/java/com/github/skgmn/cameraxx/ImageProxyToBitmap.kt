package com.github.skgmn.cameraxx

import android.graphics.*
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import kotlin.math.min

// Logic here are copied from androidx.camera.core.internal.utils.ImageUtil but little bit tweaked.

/**
 * @param format Currently only JPEG is supported.
 */
fun ImageProxy.toByteArray(format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): ByteArray? {
    require(format == Bitmap.CompressFormat.JPEG) {
        "Currently only JPEG is supported."
    }
    val data = when (this.format) {
        ImageFormat.JPEG -> jpegImageToJpegByteArray(this)
        ImageFormat.YUV_420_888 -> yuvImageToJpegByteArray(this)
        else -> null
    }
    return data
}

fun ImageProxy.toBitmap(): Bitmap? {
    if (format == ImageFormat.JPEG && shouldCropImage(this)) {
        return cropByteArrayToBitmap(readToByteArray(this), cropRect)
    }
    val bytes = toByteArray() ?: return null
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun readToByteArray(image: ImageProxy): ByteArray {
    val planes = image.planes
    val buffer = planes[0].buffer
    val data = ByteArray(buffer.capacity())
    buffer.rewind()
    buffer[data]
    return data
}

private fun jpegImageToJpegByteArray(image: ImageProxy): ByteArray? {
    val data = readToByteArray(image)
    if (shouldCropImage(image)) {
        return cropByteArray(data, image.cropRect)
    }
    return data
}

private fun shouldCropImage(image: ImageProxy): Boolean {
    val cropRect = image.cropRect
    return image.width != cropRect.width() || image.height != cropRect.height()
}

private fun cropByteArrayToBitmap(data: ByteArray, cropRect: Rect): Bitmap? {
    val decoder = BitmapRegionDecoder.newInstance(data, 0, data.size, false)
    return try {
        decoder.decodeRegion(cropRect, BitmapFactory.Options())
    } catch (e: Throwable) {
        null
    } finally {
        decoder.recycle()
    }
}

private fun cropByteArray(data: ByteArray, cropRect: Rect): ByteArray? {
    val bitmap = cropByteArrayToBitmap(data, cropRect) ?: return null
    try {
        val out = ByteArrayOutputStream()
        val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        if (!success) {
            return null
        }
        return out.toByteArray()
    } finally {
        bitmap.recycle()
    }
}

private fun yuvImageToJpegByteArray(image: ImageProxy): ByteArray? {
    return nv21ToJpeg(
        yuv420888toNv21(image),
        image.width,
        image.height,
        if (shouldCropImage(image)) image.cropRect else null
    )
}

private fun nv21ToJpeg(nv21: ByteArray, width: Int, height: Int, cropRect: Rect?): ByteArray? {
    val out = ByteArrayOutputStream()
    val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val success = yuv.compressToJpeg(
        cropRect ?: Rect(0, 0, width, height), 100, out
    )
    if (!success) {
        return null
    }
    return out.toByteArray()
}

private fun yuv420888toNv21(image: ImageProxy): ByteArray {
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]
    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    yBuffer.rewind()
    uBuffer.rewind()
    vBuffer.rewind()
    val ySize = yBuffer.remaining()
    var position = 0
    val nv21 = ByteArray(ySize + image.width * image.height / 2)

    // Add the full y buffer to the array. If rowStride > 1, some padding may be skipped.
    for (row in 0 until image.height) {
        yBuffer[nv21, position, image.width]
        position += image.width
        yBuffer.position(
            min(ySize, yBuffer.position() - image.width + yPlane.rowStride)
        )
    }
    val chromaHeight = image.height / 2
    val chromaWidth = image.width / 2
    val vRowStride = vPlane.rowStride
    val uRowStride = uPlane.rowStride
    val vPixelStride = vPlane.pixelStride
    val uPixelStride = uPlane.pixelStride

    // Interleave the u and v frames, filling up the rest of the buffer. Use two line buffers to
    // perform faster bulk gets from the byte buffers.
    val vLineBuffer = ByteArray(vRowStride)
    val uLineBuffer = ByteArray(uRowStride)
    for (row in 0 until chromaHeight) {
        vBuffer[vLineBuffer, 0, min(vRowStride, vBuffer.remaining())]
        uBuffer[uLineBuffer, 0, min(uRowStride, uBuffer.remaining())]
        var vLineBufferPosition = 0
        var uLineBufferPosition = 0
        for (col in 0 until chromaWidth) {
            nv21[position++] = vLineBuffer[vLineBufferPosition]
            nv21[position++] = uLineBuffer[uLineBufferPosition]
            vLineBufferPosition += vPixelStride
            uLineBufferPosition += uPixelStride
        }
    }
    return nv21
}