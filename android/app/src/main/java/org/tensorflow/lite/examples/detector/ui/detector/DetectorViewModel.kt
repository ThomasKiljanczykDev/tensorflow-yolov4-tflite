package org.tensorflow.lite.examples.detector.ui.detector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.detector.DetectorFactory
import org.tensorflow.lite.examples.detector.misc.Constants
import org.tensorflow.lite.examples.detector.misc.DetectionProcessor
import org.tensorflow.lite.examples.detector.ui.shared.tracking_overlay.TrackingOverlayView
import kotlin.system.measureTimeMillis

class DetectorViewModel : ViewModel() {

    companion object {
        private const val TAG: String = "DetectorViewModel"

        /*
        * Use Surface.ROTATION_0 for portrait and Surface.ROTATION_270 for landscape
        */
        const val CAMERA_ROTATION: Int = Surface.ROTATION_0
    }

    private lateinit var detectionProcessor: DetectionProcessor

    fun setUpDetectionProcessor(
        context: Context,
        displayMetrics: DisplayMetrics,
        trackingOverlayView: TrackingOverlayView,
        previewView: PreviewView
    ) = viewModelScope.launch(Dispatchers.Main) {
        val detector = DetectorFactory.createDetector(
            context,
            Constants.DETECTION_MODEL,
            Constants.MINIMUM_SCORE
        )

        detectionProcessor = DetectionProcessor(
            displayMetrics = displayMetrics,
            detector = detector,
            trackingOverlay = trackingOverlayView,
        )

        while (previewView.childCount == 0) {
            delay(200)
        }

        val surfaceView: View = previewView.getChildAt(0)
        detectionProcessor.initializeTrackingLayout(
            previewWidth = surfaceView.width,
            previewHeight = surfaceView.height,
            cropSize = detector.getDetectionModel().inputSize,
            rotation = CAMERA_ROTATION
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun detectObjectsOnImage(image: ImageProxy): Long {
        var bitmap: Bitmap
        val conversionTime = measureTimeMillis {
            bitmap = image.toBitmap()
            if (CAMERA_ROTATION % 2 == 0) {
                bitmap = rotateImage(bitmap, 90.0f)
            }
        }
        Log.v(TAG, "Conversion time : $conversionTime ms")


        val detectionTime: Long = detectionProcessor.processImage(bitmap)
        Log.v(TAG, "Detection time : $detectionTime ms")

        val processingTime = conversionTime + detectionTime
        Log.v(TAG, "Analysis time : $processingTime ms")
        return detectionTime
    }

    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

}