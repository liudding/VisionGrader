package com.linkstar.visiongrader

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.jiangdg.ausbc.utils.ToastUtils
import com.linkstar.visiongrader.VisionGraderApplication.Companion.context
import com.linkstar.visiongrader.common.utils.ImageDetectionProperties
import com.linkstar.visiongrader.common.utils.OpenCvNativeBridge
import com.linkstar.visiongrader.data.LoginRepository
import com.linkstar.visiongrader.data.UserDataStore
import com.linkstar.visiongrader.data.UserDataStore.dataStore
import com.linkstar.visiongrader.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import kotlin.math.max
import kotlin.math.min


//import org.op

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val nativeClass = OpenCvNativeBridge()


    private fun checkIsValid(approx: MatOfPoint2f, points: Array<Point>, stdSize: Size): Boolean {
        // TODO: 检测有无遮挡

        val previewWidth = stdSize.width.toFloat()
        val previewHeight = stdSize.height.toFloat()

        val resultWidth = max(previewWidth - points[0].x.toFloat(), previewWidth - points[1].x.toFloat()) -
                min(previewWidth - points[2].x.toFloat(), previewWidth - points[3].x.toFloat())

        val resultHeight = max(points[1].y.toFloat(), points[2].y.toFloat()) -
                min(points[0].y.toFloat(), points[3].y.toFloat())

        val imgDetectionPropsObj = ImageDetectionProperties(previewWidth.toDouble(), previewHeight.toDouble(),
            points[0], points[1], points[2], points[3], resultWidth.toInt(), resultHeight.toInt())

        return !imgDetectionPropsObj.isNotValidImage(approx)

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        System.loadLibrary("opencv_java4")

        Log.d("Main", "log test")

        // Load the PNG image from resources or file path
        val bitmap = BitmapFactory.decodeResource(resources, R.raw.test)
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        Log.d("cv", "============================")
        val largestQuad = nativeClass.detectLargestQuadrilateral(mat)


        if (largestQuad != null) {
            Log.d("cv", largestQuad.points[0].toString())
            Log.d("cv", largestQuad.points[2].toString())

            val valid = checkIsValid(largestQuad.contour, largestQuad.points, mat.size())
            Log.d("cv", "valid: $valid")
        }



        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        lifecycleScope.launch {
            context.dataStore.data.first()
            // You should also handle IOExceptions here.
        }

        UserDataStore.accessTokenFlow.asLiveData().observe(this, Observer {
            if (it == null) {
                navController.navigate(R.id.LoginFragment)
            }
        })

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.LoginFragment) {
                binding.toolbar.navigationIcon = null
            }
        }


//        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }

//        requestPermissions()

        val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED || hasStoragePermission != PermissionChecker.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
                ToastUtils.show(R.string.permission_tip)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, NOTIFICATION_SERVICE,

                        READ_MEDIA_IMAGES),
                    REQUEST_CAMERA
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, NOTIFICATION_SERVICE),
                    REQUEST_CAMERA
                )
            }

//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(WRITE_EXTERNAL_STORAGE),
//                REQUEST_STORAGE
//            )
            return
        }

//        System.loadLibrary("opencv_java4")
//        OpenCVLoader.initDebug()

    }

    fun requestPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // android 11  且 不是已经被拒绝
            // 先判断有没有权限
            ToastUtils.show("1")
//            if (!Environment.isExternalStorageManager()) {
                ToastUtils.show("2")
//                val intent: Intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                intent.data = Uri.parse("package:$packageName")
//                startActivityForResult(intent, 1024)

                // Initialize the ActivityResultLauncher
//                val launcher = registerForActivityResult(
//                    ActivityResultContracts.RequestPermission()
//                ) { isGranted ->
//                    if (isGranted) {
//                        // Permission granted or settings modified
//                        // Handle the desired action here
//                        ToastUtils.show("permission granted")
//                    } else {
//                        // Permission denied or settings not modified
//                        // Handle the denial scenario here
//                        ToastUtils.show("permission denied")
//                        openAppSettings()
//                    }
//                }

//                requestStoragePermission(launcher)

                // Request permission to write system settings
//                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                intent.data = Uri.parse("package:${packageName}")
//                settingsLauncher.launch(intent)
//            }
        }
    }

    private fun requestStoragePermission(launcher: ActivityResultLauncher<String>) {
        when {
            PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE
            ) == PermissionChecker.PERMISSION_GRANTED -> {
                // Permission already granted
                // Perform storage-related operation here
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                WRITE_EXTERNAL_STORAGE
            ) -> {
                // Permission denied previously but not permanently
                // Explain the need for permission to the user
                // Show a rationale dialog if needed
                ToastUtils.show("pppp")
            }
            else -> {
                // Permission denied permanently or first-time request
                // Request storage permission via ActivityResultLauncher
                launcher.launch(WRITE_EXTERNAL_STORAGE)
            }
        }
    }


    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 1024 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            // 检查是否有权限
//            if (Environment.isExternalStorageManager()) {
//                isRefuse = false
//                // 授权成功
//            } else {
//                isRefuse = true
//                // 授权失败
//            }
//        }
//    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_logout -> {
                lifecycleScope.launch {
                    val loginRepository = LoginRepository()
                    loginRepository.logout()
                }

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("Main", "onRequestPermissionsResult")

        when (requestCode) {
            REQUEST_CAMERA -> {
                val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
                Log.d("Main", "onRequestPermissionsResult: $hasCameraPermission")
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
            }
            REQUEST_STORAGE -> {
                val hasCameraPermission =
                    PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
                // todo
            }
            else -> {
            }
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 0
        private const val REQUEST_STORAGE = 1
    }
}