package com.example.findmyface

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import java.io.IOException
import android.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var preview: CameraSourcePreview
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var cameraSource: CameraSource


    private val RC_HANDLE_GMS = 9001
    private val RC_HANDLE_CAMERA_PERM = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        graphicOverlay = findViewById(R.id.faceOverlay)
        preview = findViewById(R.id.preview)

        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if(rc == PackageManager.PERMISSION_GRANTED){
            createCameraSource()
        }

    }

    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        preview.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource.release()
    }

    private fun createCameraSource() {
        val faceDetector: FaceDetector = FaceDetector.Builder(applicationContext)
            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS).build()
        faceDetector.setProcessor(MultiProcessor.Builder(GraphicFaceTrackerFactory()).build())
        cameraSource =
            CameraSource.Builder(applicationContext, faceDetector).setRequestedPreviewSize(640, 400)
                .setFacing(CameraSource.CAMERA_FACING_BACK).setRequestedFps(30F).setAutoFocusEnabled(true).build()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        }
    }

    private fun startCameraSource() {
        val code =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
        if (code == ConnectionResult.SUCCESS) {
            try {
                preview.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                cameraSource.release()
            }
        } else {
            GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS).show()
        }
    }

    private inner class GraphicFaceTrackerFactory : MultiProcessor.Factory<Face> {

        override fun create(face: Face): Tracker<Face> {
            return GraphicFaceTracker(graphicOverlay)
        }
    }

    private class GraphicFaceTracker(val overlay: GraphicOverlay) : Tracker<Face>() {

        private val faceGraphic: FaceGraphic = FaceGraphic(overlay)

        override fun onNewItem(faceId: Int, item: Face) {
            overlay.id = faceId
        }

        override fun onUpdate(results: Detector.Detections<Face>, face: Face?) {
            overlay.add(faceGraphic)
            faceGraphic.updateFace(face)
        }

        override fun onMissing(p0: Detector.Detections<Face>?) {
            overlay.remove(faceGraphic)
        }

        override fun onDone() {
            overlay.remove(faceGraphic)
        }
    }
}
