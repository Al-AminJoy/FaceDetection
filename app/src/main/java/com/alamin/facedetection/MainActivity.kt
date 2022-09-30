package com.alamin.facedetection

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture
    private lateinit var btnCapture: MaterialButton
    private lateinit var btnDetect: MaterialButton

    private lateinit var faceData: FaceData

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)
        btnDetect = findViewById(R.id.btnDetect)


        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture!!.addListener(Runnable {
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture!!.get()
                startCameraX(cameraProvider)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, getExecutor())

        btnCapture.setOnClickListener {
            captureImage()
        }
        btnDetect.setOnClickListener {
            val intent = Intent(this, FaceDetectActivity::class.java)
            Log.d(TAG, "onCreate: " + faceData)
            intent.putExtra("DATA", faceData)
            startActivity(intent)
        }

    }

    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    private fun startCameraX(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        val preview = Preview.Builder()
            .build()

        preview.setSurfaceProvider(previewView?.surfaceProvider)

        imageCapture = ImageCapture.Builder().build()


         // Image analysis use case
         val imageAnalysis = ImageAnalysis.Builder()
             .setTargetResolution(Size(1280, 720))
             .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
             .build()

         imageAnalysis.setAnalyzer(getExecutor()!!, ImageAnalysis.Analyzer { imageProxy ->
             val img = imageProxy.image
             if (img != null) {
                 val image = InputImage.fromMediaImage(img, imageProxy.imageInfo.rotationDegrees)
                 Log.d(TAG, "analyze: $image")
                 detectFaces(image,imageProxy,cameraProvider)
             }})
        cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageAnalysis,
            preview
        )

    }

    private fun captureImage() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this), // Defines where the callbacks are run
            object : ImageCapture.OnImageCapturedCallback() {
                @SuppressLint("UnsafeOptInUsageError")
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val img = imageProxy.image
                    if (img != null) {
                        val image =
                            InputImage.fromMediaImage(img, imageProxy.imageInfo.rotationDegrees)
                        Log.d(TAG, "analyze: $image")
                        //detectFaces(image, imageProxy, cameraProvider)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    // Handle exception
                }
            }
        )
    }

    private fun detectFaces(
        image: InputImage,
        imageProxy: ImageProxy,
        cameraProvider: ProcessCameraProvider
    ) {
        // [START set_detector_options]
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        // [END set_detector_options]

        // [START get_detector]
        val detector = FaceDetection.getClient(options)
        // Or, to use the default option:
        // val detector = FaceDetection.getClient();
        // [END get_detector]

        // [START run_detector]

        val result = detector.process(image)

            .addOnSuccessListener { faces ->
                imageProxy.close()
                if (faces.size == 0) {
                    //Toast.makeText(this, "No Face Detected", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                } else if (faces.size > 1) {
                   // Toast.makeText(this, "Multiple Face Detected", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                for (face in faces) {
                    val bounds = face.boundingBox
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                    Log.d(TAG, "detectFaces: $rotY $rotZ")

                  /*  if (rotY <= 0) {
                        //Turning Right
                        if (rotY <= -12.00) {
                            Toast.makeText(
                                this,
                                "Too Much Right Turn\nCapture Again",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        if (rotY <= 12.00) {
                            Toast.makeText(
                                this,
                                "Too Much Left Turn\nCapture Again",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    if (rotZ <= 0) {
                        //Turning Right
                        if (rotZ <= -12.00) {
                            Toast.makeText(
                                this,
                                "Too Much Right Tilted\nCapture Again",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        if (rotZ <= 12.00) {
                            Toast.makeText(
                                this,
                                "Too Much Left Tilted\nCapture Again",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
*/

                    if ((rotY < 1 && rotY > -1) && (rotZ < 1 && rotZ > -1)) {
                        // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                        // nose available):
                        val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                        lateinit var leftEarPosition: Position
                        leftEar?.let {
                            val position = leftEar.position
                            leftEarPosition = Position(position.x, position.y)
                        }
                        val rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR)
                        lateinit var rightEarPosition: Position
                        rightEar?.let {
                            val position = rightEar.position
                            rightEarPosition = Position(position.x, position.y)
                        }

                        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
                        lateinit var leftEyePosition: Position

                        leftEye?.let {
                            val position = leftEye.position
                            leftEyePosition = Position(position.x, position.y)
                        }

                        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
                        lateinit var rightEyePosition: Position
                        rightEye?.let {
                            val position = rightEye.position
                            rightEyePosition = Position(position.x, position.y)
                        }

                        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
                        lateinit var nosePosition: Position
                        nose?.let {
                            val position = nose.position
                            nosePosition = Position(position.x, position.y)
                        }

                        faceData = FaceData(
                            bounds.top,
                            bounds.bottom,
                            bounds.left,
                            bounds.right,
                            leftEarPosition,
                            rightEarPosition,
                            leftEyePosition,
                            rightEyePosition,
                            nosePosition
                        )
                        cameraProvider.unbindAll()
                        Log.d(TAG, "RegisterData $faceData")
                        val intent = Intent(this, FaceDetectActivity::class.java)
                        intent.putExtra("DATA", faceData)
                        startActivity(intent)
                    }



                    // If classification was enabled:
                   /* if (face.smilingProbability != null) {
                        val smileProb = face.smilingProbability
                        smileProb?.let {
                            if (smileProb >= 0.8) {
                                Toast.makeText(this, "Happy", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Sad", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }*/

                   /* if (face.rightEyeOpenProbability != null) {
                        val rightEyeOpenProb = face.rightEyeOpenProbability
                    }

                    // If face tracking was enabled:
                    if (face.trackingId != null) {
                        val id = face.trackingId
                    }*/
                }
                // [END get_face_info]
                // [END_EXCLUDE]
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "detectFaces: Failed $e")
            }

        // [END run_detector]
    }


    private fun selectFaces(image: InputImage, imageProxy: ImageProxy) {
        // [START set_detector_options]
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        // [END set_detector_options]

        // [START get_detector]
        val detector = FaceDetection.getClient(options)
        // Or, to use the default option:
        // val detector = FaceDetection.getClient();
        // [END get_detector]

        // [START run_detector]

        val result = detector.process(image)

            .addOnSuccessListener { faces ->
                imageProxy.close()
                if (faces.size == 0) {
                   // Toast.makeText(this, "No Face Detected", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                } else if (faces.size > 1) {
                    //Toast.makeText(this, "Multiple Face Detected", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                for (face in faces) {
                    val bounds = face.boundingBox
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                    if (rotY < 0 && rotY > -1) {
                        captureImage()
                    }

                }
                // [END get_face_info]
                // [END_EXCLUDE]
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "detectFaces: Failed $e")
            }

        // [END run_detector]
    }

    private fun differenceCalculator(firstValue: Number, secondValue: Number) {

    }

    private fun getExecutor(): Executor? {
        return ContextCompat.getMainExecutor(this)
    }

    private fun faceOptionsExamples() {
        // [START mlkit_face_options_examples]
        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        // Real-time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        // [END mlkit_face_options_examples]
    }

    private fun processFaceList(faces: List<Face>) {
        // [START mlkit_face_list]
        for (face in faces) {
            val bounds = face.boundingBox
            val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
            val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
            // nose available):
            val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
            leftEar?.let {
                val leftEarPos = leftEar.position
            }

            // If contour detection was enabled:
            val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)?.points
            val upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points

            // If classification was enabled:
            if (face.smilingProbability != null) {
                val smileProb = face.smilingProbability
            }
            if (face.rightEyeOpenProbability != null) {
                val rightEyeOpenProb = face.rightEyeOpenProbability
            }

            // If face tracking was enabled:
            if (face.trackingId != null) {
                val id = face.trackingId
            }
        }
        // [END mlkit_face_list]
    }

}