package com.linkstar.visiongrader.ui.scanner


import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.sidesheet.SideSheetDialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.CaptureMediaView
import com.jiangdg.ausbc.widget.IAspectRatio
import com.linkstar.visiongrader.R
import com.linkstar.visiongrader.common.extensions.toBitmap
import com.linkstar.visiongrader.common.utils.ImageDetectionProperties
import com.linkstar.visiongrader.common.utils.OpenCvNativeBridge
import com.linkstar.visiongrader.databinding.FragmentScannerBinding
import com.linkstar.visiongrader.databinding.FragmentScannerScannedListItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.time.Instant
import kotlin.math.max
import kotlin.math.min


class ScannerFragment : CameraFragment(), CaptureMediaView.OnViewClickListener {

    companion object {
        private const val TIME_POST_PICTURE = 1500L
        private const val DEFAULT_TIME_POST_PICTURE = 1500L
    }

    private val viewModel: ScannerViewModel by viewModels<ScannerViewModel>()

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!


    private lateinit var sideSheetDialog: SideSheetDialog


    private val nativeClass = OpenCvNativeBridge()

    val formats = listOf(
        BarcodeFormat.CODABAR,
        BarcodeFormat.CODE_128,
        // Add other barcode formats as needed
    )


    private  var barcodeReader: MultiFormatReader = MultiFormatReader().apply {
        setHints(mapOf(
            DecodeHintType.PURE_BARCODE to formats,
            DecodeHintType.TRY_HARDER to true
        ))

//        setResultPointCallback(ResultPointCallback { }) // Optional callback for result points
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

//        setEnableGLES(false)    // disable opengl
//        setRawImage(true)      // capture image from the origin preview frame
        updateResolution(2592, 1944)

        SoundUtils.init(requireContext())

//        mCameraView.setAspectRatio(200, 400)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)


        sideSheetDialog = SideSheetDialog(requireContext())
        sideSheetDialog.setContentView(R.layout.scanner_side_sheet)


        val recyclerView = binding.scannedList
        val adapter =  ScannedItemAdapter()
        recyclerView.adapter =  adapter
        viewModel.scannedList.observe(viewLifecycleOwner) {
            adapter.submitList(it)

            recyclerView.smoothScrollToPosition(0)
        }

        return binding.root
    }

    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup? {
        return _binding?.cameraViewContainer
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View? {
        if (_binding == null) {
            _binding = FragmentScannerBinding.inflate(inflater, container, false)
        }
        return _binding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel
    }

//    override fun getDefaultCamera(): UsbDevice? {
//        return super.getDefaultCamera()
//    }


    override fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(2048)
            .setPreviewHeight(1536)
            .setRenderMode(CameraRequest.RenderMode.OPENGL) // camera render mode
            .setDefaultRotateType(RotateType.ANGLE_0) // rotate camera image when opengl mode
            .setAudioSource(CameraRequest.AudioSource.NONE) // set audio source
//            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_YUYV) // set preview format, MJPEG recommended
            .setAspectRatioShow(false) // asp    ect render,default is true
            .setCaptureRawImage(true) // capture raw image picture when opengl mode
            .setRawPreviewData(true)  // preview raw image when opengl mode
            .create()
    }

    override fun onCameraState(self: MultiCameraClient.ICamera,
                               code: ICameraStateCallBack.State,
                               msg: String?) {
//        ToastUtils.show("onCameraState: $code")
        when (code) {
            ICameraStateCallBack.State.OPENED -> handleCameraOpened()
            ICameraStateCallBack.State.CLOSED -> handleCameraClosed()
            ICameraStateCallBack.State.ERROR -> handleCameraError(null)
        }
    }

    private fun handleCameraError(msg: String?) {
//        mViewBinding.uvcLogoIv.visibility = View.VISIBLE
        ToastUtils.show("camera opened error: $msg")
    }

    private fun handleCameraClosed() {
//        mViewBinding.uvcLogoIv.visibility = View.VISIBLE
//        mViewBinding.frameRateTv.visibility = View.GONE
        ToastUtils.show("camera closed success")
    }

    private var frames = 0

    private fun handleCameraOpened() {
        getCurrentCamera()?.addPreviewDataCallBack( object : IPreviewDataCallBack {
            override fun onPreviewData(
                data: ByteArray?,
                width: Int,
                height: Int,
                format: IPreviewDataCallBack.DataFormat
            ) {
                Log.i("scanner", "onPreviewData")
                if (data == null) {
                    return
                }

                frames ++
                if (frames % 2 == 0) {
                    return
                }


                val barcode = scanBarcode(data, width, height)
                if (barcode != null) {
                    requireActivity().runOnUiThread{
                        viewModel.setCurrentStudent(barcode.text)
                    }

                }

//                if (binding.currentScanningStudent.text == "") {
//                    requireActivity().runOnUiThread{
//                        ToastUtils.show("请先扫描学生条形码")
//                    }
//                    return
//                }

                GlobalScope.launch(Dispatchers.Default) {
                    doScan(data, width, height, format)
                }

            }
        })
    }

    private suspend fun doScan(data: ByteArray, width: Int, height: Int, format: IPreviewDataCallBack.DataFormat) {
        try {
            val mat: Mat?
            if (format == IPreviewDataCallBack.DataFormat.RGBA) {
                var bytes: ByteArray = data
                if (width * height * 4 != data.size) {
                    // 多出 7 个
                    bytes = data.copyOfRange(7, data.size)
                }

                mat = Mat(height, width, CvType.CV_8UC4)
                mat.put(0, 0, bytes)
            } else {
                val matNv21 = Mat(height + height / 2, width, CvType.CV_8UC1)
                matNv21.put(0, 0, data)

                mat = Mat()
                Imgproc.cvtColor(matNv21, mat, Imgproc.COLOR_YUV2RGBA_NV21)
                matNv21.release()
            }

            val originalPreviewSize = mat.size()

            val mat2 = Mat()
            mat.copyTo(mat2)
            val largestQuad = nativeClass.detectLargestQuadrilateral(mat2)
            mat2.release()

            if (null != largestQuad) {
                if (checkIsValid(largestQuad.contour, largestQuad.points, originalPreviewSize)) {
                    requireActivity().runOnUiThread {
                        if (!isAutoCaptureScheduled) {
//                                scheduleAutoCapture()
                        }

                        binding.scanCanvasView.showShape(originalPreviewSize.width.toFloat(),
                            originalPreviewSize.height.toFloat(),
                            largestQuad.points)
                    }

                    try {
                        val images = takeCroppedImage(mat, largestQuad.points, false)
                        mat.release()

                        images?.let {
                            requireActivity().runOnUiThread {
                                viewModel.setCurrentStudent("test")
                                ToastUtils.show("扫描成功 ")
                                viewModel.addNewScanned(it)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            ToastUtils.show("error: $e")
                        }
                    }

                } else {
                    requireActivity().runOnUiThread {
                        binding.scanCanvasView.clearShape()
                    }
                    cancelAutoCapture()
                }

            } else {
                requireActivity().runOnUiThread {
                    clearAndInvalidateCanvas()
                }
            }
            mat.release()
        } catch (e: Exception) {
            clearAndInvalidateCanvas()
        }
    }

    private fun clearAndInvalidateCanvas() {
        binding.scanCanvasView.clearShape()
    }

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


    private fun scanBarcode(data: ByteArray, width: Int, height: Int): Result? {
        return try {
            val intArray = IntArray(data.size) { i -> data[i].toInt() }
            val source = RGBLuminanceSource(width, height, intArray)
            barcodeReader.decode(BinaryBitmap(HybridBinarizer(source)))
        } catch (e: Exception) {
            null
        }
    }

    private var autoCaptureTimer: CountDownTimer? = null
    private var millisLeft = 0L
    private var isAutoCaptureScheduled = false
    private var isCapturing = false

    private fun scheduleAutoCapture() {
        isAutoCaptureScheduled = true
        millisLeft = 0L
        autoCaptureTimer = object : CountDownTimer(DEFAULT_TIME_POST_PICTURE, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if (millisUntilFinished != millisLeft) {
                    millisLeft = millisUntilFinished
                }
            }

            override fun onFinish() {
                isAutoCaptureScheduled = false
                autoCapture()
            }
        }
        autoCaptureTimer?.start()
    }

    private fun autoCapture() {
        if (isCapturing)
            return
        cancelAutoCapture()
        takePicture()
    }

    fun takePicture() {
//        listener.scanSurfaceShowProgress()
        isCapturing = true


        val filename = "${Instant.now().toEpochMilli()}${(0..999).random()}"

        val path = requireContext().getExternalFilesDir("pics")?.absolutePath + File.separator + "$filename.png"

        captureImage(object : ICaptureCallBack {
            override fun onBegin() {

            }

            override fun onError(error: String?) {
                ToastUtils.show(error ?: "未知异常")
//                listener.scanSurfaceHideProgress()
//                Log.e(TAG, "${ErrorMessage.PHOTO_CAPTURE_FAILED.error}: ${exc.message}", exc)
//                listener.onError(DocumentScannerErrorModel(ErrorMessage.PHOTO_CAPTURE_FAILED, exc))
            }

            override fun onComplete(path: String?) {
                path?.let {
                    ToastUtils.show(it)
                }

//                listener.scanSurfaceHideProgress()

//                unbindCamera()

                clearAndInvalidateCanvas()
//                listener.scanSurfacePictureTaken()

                Handler().postDelayed({
                    isCapturing = false
                }, TIME_POST_PICTURE)
            }
        }, path)
    }


    private fun cancelAutoCapture() {
        if (isAutoCaptureScheduled) {
            isAutoCaptureScheduled = false
            autoCaptureTimer?.cancel()
        }
    }

    private fun takeCroppedImage(mat: Mat, points: Array<Point>, split: Boolean): ScannedImages? {
        val image = getCroppedImage(mat, points)

        if (image == null || image.empty() || image.width() == 0) {
            return null
        }

        if (!split) {
            val res = ScannedImages(mat.toBitmap(), image.toBitmap(), false)
            image.release()
            return res
        }

        val (left, right) = nativeClass.splitPagesBySpine(image)

//        nativeClass.processRealMagic(left)
//        nativeClass.processRealMagic(right)

        val res = ScannedImages(mat.toBitmap(),
            image.toBitmap(),
            true,
            left.toBitmap(),
            right.toBitmap())

        left.release()
        right.release()
        image.release()

        return res
    }


    private fun getCroppedImage(mat: Mat, points: Array<Point>): Mat? {
        try {
            val pointPadding = 0 // requireContext().resources.getDimension(R.dimen.zdc_point_padding).toInt()
            val x1: Float = (points[0].x.toFloat() + pointPadding)
            val x2: Float = (points[1].x.toFloat() + pointPadding)
            val x3: Float = (points[2].x.toFloat() + pointPadding)
            val x4: Float = (points[3].x.toFloat() + pointPadding)
            val y1: Float = (points[0].y.toFloat() + pointPadding)
            val y2: Float = (points[1].y.toFloat() + pointPadding)
            val y3: Float = (points[2].y.toFloat() + pointPadding)
            val y4: Float = (points[3].y.toFloat() + pointPadding)
            return nativeClass.getScanned(mat, x1, y1, x2, y2, x3, y3, x4, y4)
        } catch (e: java.lang.Exception) {
//            Log.e(TAG, DocumentScannerErrorModel.ErrorMessage.CROPPING_FAILED.error, e)
//            onError(DocumentScannerErrorModel(DocumentScannerErrorModel.ErrorMessage.CROPPING_FAILED, e))
            return null
        }
    }

    override fun getGravity(): Int = Gravity.TOP

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.subjectState.observe(viewLifecycleOwner) {
            binding.scanTarget.subject.text = it?.label ?: ""
        }
        viewModel.textbookState.observe(viewLifecycleOwner) {
            binding.scanTarget.textbook.text = it?.name ?: ""
        }
        viewModel.workbookState.observe(viewLifecycleOwner) {
            binding.scanTarget.workbook.text = it?.name ?: ""
        }

        viewModel.currentStudentState.observe(viewLifecycleOwner) {
            binding.currentScanningStudent.text = it ?: ""

//            it?.let {
//                ToastUtils.show("Student: $it")
//            }
        }


        binding.scanTarget.cellSubject.setOnClickListener {
            val list = viewModel.subjects.map {
                PickerFragment.Item(it.label, it.code)
            }

            val dialog = PickerFragment.newInstance(list, "选择科目")
            dialog.setOnItemSelectedListener(object : PickerFragment.OnItemSelectedListener {
                override fun onItemSelectedListener(position: Int) {
                    val item = list[position]
                    viewModel.subjectChanged(item.value)
                }

            })
            fragmentManager?.let { it1 -> dialog.show(it1, "SUBJECT_DIALOG") }
        }

        binding.scanTarget.cellTextbook.setOnClickListener {
            if (viewModel.textbooksState.value == null) {
                return@setOnClickListener
            }

            val list = viewModel.textbooksState.value!!.map {
                PickerFragment.Item(it.name, it.id)
            }

            val dialog = PickerFragment.newInstance(list, "选择教材")
            dialog.setOnItemSelectedListener(object : PickerFragment.OnItemSelectedListener {
                override fun onItemSelectedListener(position: Int) {
                    val item = list[position]
                    viewModel.textbookChanged(item.value)
                }

            })
            fragmentManager?.let { it1 -> dialog.show(it1, "TEXTBOOK_DIALOG") }
        }

        binding.scanTarget.cellWorkbook.setOnClickListener {
            if (viewModel.workbooksState.value == null) {
                return@setOnClickListener
            }

            val list = viewModel.workbooksState.value!!.map {
                PickerFragment.Item(it.name, it.id)
            }

            val dialog = PickerFragment.newInstance(list, "选择练习册")
            dialog.setOnItemSelectedListener(object : PickerFragment.OnItemSelectedListener {
                override fun onItemSelectedListener(position: Int) {
                    val item = list[position]
                    viewModel.workbookChanged(item.value)
                }

            })
            fragmentManager?.let { it1 -> dialog.show(it1, "WORKBOOK_DIALOG") }
        }


//        binding.testbtn.setOnClickListener{
//            SoundUtils.playBeep()
//        }


//        binding.chan.setOnClickListener {
//            findNavController().navigate(R.id.action_HomeFragment_to_ScannerFragment)
//        }

//        sideSheetDialog.show()

//        val dd = PickerFragment.newInstance(, "11")
//        fragmentManager?.let { dd.show(it, "aa") }


//        val subjectCell = sideSheetDialog.findViewById<View>(R.id.subject)
//        val dialog = PickerFragment()
//        fragmentManager?.let { dialog.show(it, "DD") }
//        subjectCell?.setOnClickListener(
//
//        )

//        ToastUtils.show("w " + binding.cameraViewContainer.width)
//
//        binding.scanCanvasView.showShape(1000f, 900f,
//            arrayOf(Point(50.0, 100.0), Point(800.0, 50.0),
//                 Point(800.0, 600.0), Point(100.0, 600.0))
//        )

    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
//        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
//            val imageUri: Uri? = data.data
//            // Display the image in the ImageView
//            binding.imageView.setImageURI(imageUri)
//        }
    }


//    @Suppress("DEPRECATION")
//    @Deprecated("Deprecated in Java")
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        inflater.inflate(R.menu.menu_scanner, menu)
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_change_workbook ->  {
//                sideSheetDialog.show()
//                return false
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }


    /**
     * 这个方法会让 返回键失效
     */
    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // Handle for example visibility of menu items
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_scanner, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Validate and handle the selected menu item
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onStart() {
        super.onStart()

        System.loadLibrary("opencv_java4")
    }

    override fun onResume() {
        super.onResume()


    }

    override fun onStop() {
        super.onStop()

        SoundUtils.release()
    }

    class ScannedItemAdapter :
        ListAdapter<String, ScannedItemViewHolder>(object : DiffUtil.ItemCallback<String>() {

            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedItemViewHolder {
            val binding = FragmentScannerScannedListItemBinding.inflate(LayoutInflater.from(parent.context))
            return ScannedItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ScannedItemViewHolder, position: Int) {
            holder.textview.text = getItem(position)
        }

    }

    class ScannedItemViewHolder(binding: FragmentScannerScannedListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

            val textview = binding.textview
    }

    override fun onViewClick(mode: CaptureMediaView.CaptureMode?) {
        TODO("Not yet implemented")
    }


}

