package com.alamin.facedetection

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.math.absoluteValue

private const val TAG = "FaceDetectActivity"
class FaceDetectActivity : AppCompatActivity() {
    private lateinit var faceDataExtra: FaceData
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    lateinit var previewView: PreviewView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)

        intent?.let {
            faceDataExtra = intent.getParcelableExtra("DATA")!!
        }
        previewView = findViewById(R.id.previewView)

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

    }

    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    private fun startCameraX(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        val preview = Preview.Builder()
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)



         // Image analysis use case
         val imageAnalysis = ImageAnalysis.Builder()
             .setTargetResolution(Size(1080, 720))
             .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
             .build()

         imageAnalysis.setAnalyzer(getExecutor()!!, ImageAnalysis.Analyzer { imageProxy ->
             val img = imageProxy.image
             if (img != null) {
                 val image = InputImage.fromMediaImage(img, imageProxy.imageInfo.rotationDegrees)
                 Log.d(TAG, "analyze: $image")
                 detectFaces(image,imageProxy,cameraProvider)
             }})
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)

    }

    private fun detectFaces(
        image: InputImage,
        imageProxy: ImageProxy,
        cameraProvider: ProcessCameraProvider
    ) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        val detector = FaceDetection.getClient(options)


        val result = detector.process(image)

            .addOnSuccessListener { faces ->
                imageProxy.close()
                if (faces.size == 0){
                   // Toast.makeText(this, "No Face Detected", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }else if (faces.size > 1){
                   // Toast.makeText(this, "Multiple Face Detected", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                for (face in faces) {
                    val bounds = face.boundingBox
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees
                    Log.d(TAG, "detectFaces: $rotY $rotZ ")

                    if ((rotY >= -12 && rotY <= 12) && (rotZ >= -12 && rotZ <= 12)){
                        Log.d(TAG, "detectFaces: In Range ")

                        val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                        lateinit var leftEarPosition: Position
                        leftEar?.let {
                            val position = leftEar.position
                            leftEarPosition = Position(position.x,position.y)
                        }
                        val rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR)
                        lateinit var  rightEarPosition:Position
                        rightEar?.let {
                            val position = rightEar.position
                            rightEarPosition = Position(position.x,position.y)
                        }

                        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
                        lateinit var leftEyePosition:Position

                        leftEye?.let {
                            val position = leftEye.position
                            leftEyePosition = Position(position.x,position.y)
                        }

                        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
                        lateinit var  rightEyePosition:Position
                        rightEye?.let {
                            val position = rightEye.position
                            rightEyePosition = Position(position.x,position.y)
                        }

                        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
                        lateinit var nosePosition:Position
                        nose?.let {
                            val position = nose.position
                            nosePosition = Position(position.x,position.y)
                        }

                        val faceData = FaceData(bounds.top,bounds.bottom,bounds.left,bounds.right,leftEarPosition,rightEarPosition,leftEyePosition,rightEyePosition,nosePosition)
                        Log.d(TAG, "difference: $faceData")
                        Log.d(TAG, "difference: $faceDataExtra")
                        var matched = 0
                        if (checkFace(faceData)){
                            matched++
                            cameraProvider.unbindAll()
                            Log.d(TAG, "differenceCalculator: Matched $matched")
                            Toast.makeText(this, "Matched", Toast.LENGTH_SHORT).show()

                        }else{
                            // Toast.makeText(this, "Didn't Matched", Toast.LENGTH_SHORT).show()
                            //Log.d(TAG, "differenceCalculator: Not Matched")

                        }
                    }else{
                        Log.d(TAG, "detectFaces: Out of Range")
                    }

                }

            }
            .addOnFailureListener { e ->
                Log.d(TAG, "detectFaces: Failed $e")
            }

    }

    private fun checkFace(faceData: FaceData): Boolean {
        if (differenceCalculator(faceData.leftEyePosition.x,faceDataExtra.leftEyePosition.x) < 10.00){
        }else{
            Log.d(TAG, "FaceCheckData Left Eye X")
            return false
        }
        if (differenceCalculator(faceData.leftEyePosition.y,faceDataExtra.leftEyePosition.y) < 10.00){
        }else{
            Log.d(TAG, "FaceCheckData Left Eye Y")
            return false
        }
        if (differenceCalculator(faceData.rightEyePosition.x,faceDataExtra.rightEyePosition.x) < 10.00){
        }else{
            Log.d(TAG, "FaceCheckData Right Eye X")
            return false
        }
        if (differenceCalculator(faceData.rightEyePosition.y,faceDataExtra.rightEyePosition.y) < 10.00){
        }else{
            Log.d(TAG, "FaceCheckData Right Eye Y")
            return false
        }
        if (differenceCalculator(faceData.nosePosition.x,faceDataExtra.nosePosition.x) < 10.00){
        }else{
            Log.d(TAG, "FaceCheckData Nose X")
            return false
        }
        if (differenceCalculator(faceData.nosePosition.y,faceDataExtra.nosePosition.y) < 10.00){
        }else{
            Log.d(TAG, "FaceCheckData Nose Y")
            return false
        }

        return true
       /* return differenceCalculator(faceData.leftEyePosition.x,faceDataExtra.leftEyePosition.x) < 15.00 &&
                differenceCalculator(faceData.leftEyePosition.y,faceDataExtra.leftEyePosition.y) < 15.00 &&
                differenceCalculator(faceData.rightEyePosition.x,faceDataExtra.rightEyePosition.x) < 15.00 &&
                differenceCalculator(faceData.rightEyePosition.y,faceDataExtra.rightEyePosition.y) < 15.00 &&
                differenceCalculator(faceData.nosePosition.x,faceDataExtra.nosePosition.x) < 15.00 &&
                differenceCalculator(faceData.nosePosition.y,faceDataExtra.nosePosition.y) < 15.00
*/

        /*return differenceCalculator(faceData.boundTop,faceDataExtra.boundTop) < 25.00
        differenceCalculator(faceData.leftEarPosition.x,faceDataExtra.leftEarPosition.x) < 20.00 &&
                differenceCalculator(faceData.leftEarPosition.y,faceDataExtra.leftEarPosition.y) < 20.00 &&
                differenceCalculator(faceData.rightEarPosition.x,faceDataExtra.rightEarPosition.x) < 20.00 &&
                differenceCalculator(faceData.rightEarPosition.y,faceDataExtra.rightEarPosition.y) < 20.00 */

    }

    private fun differenceCalculator(firstValue: Number, secondValue: Number):Double{

        val top: Double = (firstValue.toDouble()-secondValue.toDouble()).absoluteValue
        val bottom = ((firstValue.toDouble()+secondValue.toDouble())/2).absoluteValue
        val difference = (top/bottom)*100
        Log.d(TAG, "differenceCalculator: $difference")
        return difference

    }

    private fun getExecutor(): Executor? {
        return ContextCompat.getMainExecutor(this)
    }

}