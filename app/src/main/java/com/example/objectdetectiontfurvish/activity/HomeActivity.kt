package com.example.objectdetectiontfurvish.activity

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.objectdetectiontfurvish.R
import com.example.objectdetectiontfurvish.tfhelper.ObjectDetectorHelper
import com.example.objectdetectiontfurvish.tfhelper.OverlayView
import com.example.objectdetectiontfurvish.utils.PermissionsUtil
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class HomeActivity : AppCompatActivity(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "HomeActivity"

    private lateinit var objectDetectorHelper: ObjectDetectorHelper // Helper for object detection
    private lateinit var bitmapBuffer: Bitmap // Buffer for image processing
    private var preview: Preview? = null // Camera preview
    private var imageAnalyzer: ImageAnalysis? = null // Image analyzer
    private var camera: Camera? = null // Camera instance
    private var cameraProvider: ProcessCameraProvider? = null // Camera provider
    private lateinit var cameraExecutor: ExecutorService // Executor for camera operations
    private lateinit var viewFinder: PreviewView // Camera preview view
    private lateinit var overlayView: OverlayView // View for overlaying object detection results
    private lateinit var flipCamera: RelativeLayout // Button for flipping between front and back cameras
    private lateinit var captureButton: RelativeLayout // Button for capturing snapshots
    private var isBackCameraSelected = true // Flag to track the selected camera
    private lateinit var seekBarThreshold: SeekBar // SeekBar for adjusting the detection threshold
    private lateinit var textViewThreshold: TextView // Text view to display threshold value
    private var isCameraSettingsFired = false // Flag for camera settings state
    private var isStorageSettingsFired = false // Flag for storage settings state
    private val requestCameraPermissions = 457 // Request code for camera permissions
    private val requestStoragePermissions = 458 // Request code for storage permissions
    private lateinit var textViewDetectionCount: TextView // TextView for the detection count
    private var detectionCount = 0 // Initialize the detection count


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the activity content view to the specified layout
        setContentView(R.layout.activity_home)

        // Initialize the object detection helper for this activity
        objectDetectorHelper = ObjectDetectorHelper(this, this)

        // Initialize the background executor for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out before proceeding
        initViews()

        // Use a post operation on the viewFinder to ensure views are laid out
        viewFinder.post {
            if (!PermissionsUtil.hasPermissions(
                    this, PermissionsUtil.cameraPermissions()
                )
            ) {
                // Request camera permissions if not already granted
                requestPermissions(requestCameraPermissions)
            } else {
                // Set up the camera if camera permissions are granted
                setUpCamera()
            }
            // Set up event listeners for UI elements
            setUpListeners()
        }
    }


    private fun initViews() {
        // Initialize views by finding their respective XML elements
        viewFinder = findViewById(R.id.pv_view_finder)
        overlayView = findViewById(R.id.ov_overlay)
        flipCamera = findViewById(R.id.rel_camera_flip)
        seekBarThreshold = findViewById(R.id.seekbar_threshold)
        textViewThreshold = findViewById(R.id.tv_threshold)
        textViewDetectionCount = findViewById(R.id.tv_detection_count)
        captureButton = findViewById(R.id.rel_capture_button)

        // Set up the action bar, allowing for navigation to the previous screen
        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun updateDetectionCount(count: Int) {
        // Update the detection count based on the given value
        detectionCount += count

        // Update the detection count TextView on the UI thread
        runOnUiThread {
            textViewDetectionCount.text =
                String.format(getString(R.string.detection_count), detectionCount)
        }
    }

    private fun setUpListeners() {
        // Set up click listeners for UI elements

        // Flip Camera button click listener
        flipCamera.setOnClickListener { view ->
            // Check camera permissions, then switch camera
            if (!PermissionsUtil.hasPermissions(
                    view!!.context, PermissionsUtil.cameraPermissions()
                )
            ) {
                requestPermissions(requestCameraPermissions)
            } else {
                switchCamera()
            }
        }

        // Capture Button click listener
        captureButton.setOnClickListener { view ->
            // Check camera and storage permissions, then capture a snapshot
            if (!PermissionsUtil.hasPermissions(
                    view!!.context, PermissionsUtil.cameraPermissions()
                )
            ) {
                requestPermissions(requestCameraPermissions)
            } else if (!PermissionsUtil.hasPermissions(
                    view.context, PermissionsUtil.storagePermissions()
                )
            ) {
                requestPermissions(requestStoragePermissions)
            } else {
                captureSnapshot()
            }
        }

        // SeekBar for adjusting the detection threshold
        seekBarThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update threshold TextView
                textViewThreshold.text = String.format(getString(R.string.threshold), progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Actions when the user starts touching the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Convert SeekBar value to threshold and clear detector
                val threshold = seekBar?.progress?.div(100.0)
                objectDetectorHelper.threshold = threshold!!.toFloat()
                objectDetectorHelper.clearObjectDetector()
                overlayView.clear()
            }
        })
    }

    // Switch between front and back cameras
    private fun switchCamera() {
        // Toggle between front and back cameras
        isBackCameraSelected = !isBackCameraSelected

        // Re-bind the camera use cases with the new camera selector
        bindCameraUseCases()
    }

    private fun setUpCamera() {
        // Set up the camera with appropriate use cases

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this)
        )
    }

    private fun bindCameraUseCases() {
        // Binds camera use cases

        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // Configure camera selector based on the selected camera (front or back)
        val cameraSelector = CameraSelector.Builder().apply {
            if (isBackCameraSelected) {
                requireLensFacing(CameraSelector.LENS_FACING_BACK)
            } else {
                requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            }
        }.build()

        // Configure the preview use case
        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(viewFinder.display.rotation)
            .build()

        // Configure the image analysis use case
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().also {
                it.setAnalyzer(cameraExecutor) { image ->
                    if (!::bitmapBuffer.isInitialized) {
                        // Initialize the image rotation and RGB image buffer once the analyzer starts running
                        bitmapBuffer = Bitmap.createBitmap(
                            image.width, image.height, Bitmap.Config.ARGB_8888
                        )
                    }
                    // Perform object detection on the image
                    detectObjects(image)
                }
            }

        // Unbind all previously bound use cases
        cameraProvider.unbindAll()

        try {
            // Bind the camera use cases
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to the preview use case
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        // Detect objects in the provided image

        // Copy the RGB bits from the image's buffer to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass the Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update the target rotation of the image analyzer based on the viewfinder's display rotation
        imageAnalyzer?.targetRotation = viewFinder.display.rotation
    }

    override fun onError(error: String) {
        // Handle and display errors to the user
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        runOnUiThread {
            // Update the OverlayView with detection results and dimensions
            overlayView.setResults(
                results ?: LinkedList<Detection>(), imageHeight, imageWidth
            )

            // Force a redraw of the OverlayView
            overlayView.invalidate()

            // Update the detection count
            results?.let { updateDetectionCount(it.size) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shutdown the camera executor to release resources
        cameraExecutor.shutdown()
    }

    private fun captureSnapshot() {
        // Capture a snapshot of the camera view

        // Obtain a Bitmap from the PreviewView (viewFinder)
        val snapshotBitmap = viewFinder.bitmap

        if (snapshotBitmap != null) {
            // Create a Canvas from the snapshotBitmap to draw detection boxes and labels
            val canvas = Canvas(snapshotBitmap)

            // Draw detection boxes and labels from the OverlayView on the Canvas
            canvas.let { overlayView.draw(it) }

            // Generate a unique file name for the snapshot
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Snapshot_$timeStamp.jpg"

            // Define the path and file where the image will be saved
            val imageFile = File(getOutputDirectory(), fileName)

            // Compress the snapshotBitmap to JPEG format and save it to the file
            FileOutputStream(imageFile).use { outputStream ->
                snapshotBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }

            // Notify the media scanner to add the image to the gallery
            MediaScannerConnection.scanFile(
                this, arrayOf(imageFile.path), null, null
            )

            // Show a toast message to indicate the image has been saved
            Toast.makeText(this, "Snapshot saved to $imageFile", Toast.LENGTH_SHORT).show()
        } else {
            // Handle the case where snapshotBitmap is null
            Toast.makeText(this, "Snapshot capture failed", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getOutputDirectory(): File {
        // Get the public directory for pictures
        val publicDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        // Create a subdirectory for your app if it doesn't exist
        val file = File(publicDirectory, getString(R.string.app_name))
        if (!file.exists()) {
            file.mkdirs()
        }

        // Return the directory where the image will be saved
        return file
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            requestCameraPermissions -> {
                if (PermissionsUtil.permissionsGranted(grantResults)) {
                    // Permissions granted, set up the camera
                    setUpCamera()
                } else {
                    // Show a permission dialog
                    showPermissionDialog(requestCameraPermissions)
                }
            }

            requestStoragePermissions -> {
                if (PermissionsUtil.permissionsGranted(grantResults)) {
                    // Permissions granted, capture the snapshot
                    captureSnapshot()
                } else {
                    // Show a permission dialog
                    showPermissionDialog(requestStoragePermissions)
                }
            }
        }
    }

    private fun showPermissionDialog(permissionTypeCode: Int) {
        val builder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.app_requires_permissions))
            .setCancelable(false)

        builder.setPositiveButton(getString(R.string.grant)) { _, _ ->
            if (PermissionsUtil.isPermissionDeniedAndDontAskAgain(
                    this, getPermissionsForType(permissionTypeCode)
                )
            ) {
                // Open the app settings
                openAppSettings(this, permissionTypeCode)
            } else {
                // Request permissions again
                requestPermissions(permissionTypeCode)
            }
        }

        builder.setNegativeButton(getString(R.string.not_now)) { dialog, _ ->
            // Dismiss the dialog
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }


    private fun getPermissionsForType(permissionTypeCode: Int): Array<String> {
        // Determine the type of permissions based on the provided code
        return when (permissionTypeCode) {
            requestCameraPermissions -> PermissionsUtil.cameraPermissions()
            requestStoragePermissions -> PermissionsUtil.storagePermissions()
            else -> emptyArray()
        }
    }

    private fun openAppSettings(activity: Activity, permissionTypeCode: Int) {
        // Open the app settings for the specified permission type
        when (permissionTypeCode) {
            requestCameraPermissions -> isCameraSettingsFired = true
            requestStoragePermissions -> isStorageSettingsFired = true
        }
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (isCameraSettingsFired) {
            isCameraSettingsFired = false
            if (PermissionsUtil.hasPermissions(
                    this, PermissionsUtil.cameraPermissions()
                )
            ) {
                // Camera permissions granted, set up the camera
                setUpCamera()
            } else {
                // Show a permission dialog for camera permissions
                showPermissionDialog(requestCameraPermissions)
            }
        }
        if (isStorageSettingsFired) {
            isStorageSettingsFired = false
            if (!PermissionsUtil.hasPermissions(
                    this, PermissionsUtil.storagePermissions()
                )
            ) {
                // Show a permission dialog for storage permissions
                showPermissionDialog(requestStoragePermissions)
            }
        }
    }

    private fun requestPermissions(permissionTypeCode: Int) {
        // Request permissions based on the provided code
        when (permissionTypeCode) {
            requestCameraPermissions -> ActivityCompat.requestPermissions(
                this, PermissionsUtil.cameraPermissions(), requestCameraPermissions
            )

            requestStoragePermissions -> ActivityCompat.requestPermissions(
                this, PermissionsUtil.storagePermissions(), requestStoragePermissions
            )
        }
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        // Handle options item selection, including the back button
        when (item.itemId) {
            android.R.id.home -> {
                // Emulate the back button press
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
