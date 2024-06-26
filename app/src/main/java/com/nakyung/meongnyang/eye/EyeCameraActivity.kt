package com.nakyung.meongnyang.eye

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.Camera
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import com.nakyung.meongnyang.databinding.EyeActivityCameraBinding
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.*
import java.io.File
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage

class EyeCameraActivity : AppCompatActivity(), SurfaceHolder.Callback, Camera.PictureCallback {
    private var mModule: Module? = null

    private lateinit var binding: EyeActivityCameraBinding

    private var surfaceHolder: SurfaceHolder? = null
    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = EyeActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermission {
            setupSurfaceHolder()
            startCamera()

            // 갤러리에서 선택 클릭했을 때
            binding.selectGallery.setOnClickListener {
                openGallery()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkPermission(logic: () -> Unit) {
        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                // 권한이 허용되었을 때
                override fun onPermissionGranted() {
                    logic()
                }

                // 권한이 거부됐을 때
                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    Toast.makeText(this@EyeCameraActivity, "권한 거부됨", Toast.LENGTH_SHORT).show()
                }
            })
            .setRationaleMessage("카메라 권한이 필요한 서비스입니다.")
            .setDeniedMessage("카메라 권한을 허용해 주세요! [설정] > [앱 및 알림] > [고급] > [앱 권한]")
            .setDeniedCloseButtonText("닫기")
            .setGotoSettingButtonText("설정")
            .setPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .check()
    }

    private fun setupSurfaceHolder() {
        binding.surfaceView.visibility = View.VISIBLE
        surfaceHolder = binding.surfaceView.holder
        binding.surfaceView.holder.addCallback(this)
        setBtnClick()
    }

    private fun setBtnClick() {
        binding.resultBtn.setOnClickListener {
            captureImage()
        }
    }

    private fun captureImage() {
        if (camera != null) {
            camera!!.takePicture(null, null, this)
        }
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        startCamera()
    }

    private fun startCamera() {
        camera = Camera.open()
        camera!!.setDisplayOrientation(90)
        try {
            setCamFocusMode() // 자동 초점 기능 설정
            camera!!.setPreviewDisplay(surfaceHolder)
            camera!!.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
        resetCamera()
    }

    private fun resetCamera() {
        if (surfaceHolder!!.surface == null) {
            // Return if preview surface does not exist
            return
        }

        // Stop if preview surface is already running.
        camera!!.stopPreview()
        try {
            // Set preview display
            camera!!.setPreviewDisplay(surfaceHolder)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Start the camera preview...
        camera!!.startPreview()
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        releaseCamera()
    }

    private fun releaseCamera() {
        camera!!.stopPreview()
        camera!!.release()
        camera = null
    }

    override fun onPictureTaken(bytes: ByteArray, camera: Camera) {
        saveImage(bytes)
        sendImg(bytes)
        resetCamera()
    }

    // resultActivity로 사진 보내주기
    private fun sendImg(bytes: ByteArray) {
        val classList = mutableMapOf<Int, String>()
        classList[0] = "결막염"
        classList[1] = "궤양성각막질환"
        classList[2] = "백내장"
        classList[3] = "비궤양성각막질환"
        classList[4] = "안구무증상"

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // 224x224로 크기 조절
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // 90도 회전시키기
        val matrix = Matrix()
        matrix.postRotate(0f)

        val resizedBitmap = Bitmap.createBitmap(resized, 0, 0, resized.width, resized.height, matrix, true)
        camera!!.startPreview()
        val inputImg = TensorImage.fromBitmap(resizedBitmap)
        val normalization = imageProcess(inputImg) // normalization
        val resultImg = normalization!!.bitmap

        val stream = ByteArrayOutputStream()
        resultImg.compress(Bitmap.CompressFormat.PNG, 100, stream)

        //val byte = stream.toByteArray()

        //saveImage(byte) // 압축한 거 저장해 보기

        // model
        if (mModule == null) {
            mModule = LiteModuleLoader.load(assetFilePath(this, "model2.ptl"))
        }

        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resultImg,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        val outputTensor = mModule!!.forward(IValue.from(inputTensor)).toTensor()
        val scores = outputTensor.dataAsFloatArray

        var maxScore = -Float.MAX_VALUE
        var maxScoreIdx = -1
        for (i in scores.indices) {
            if (scores[i] > maxScore) {
                maxScore = scores[i]
                maxScoreIdx = i
            }
        }

        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 60, stream)
        val bytes = stream.toByteArray()

        val intent = Intent(this, EyeResultActivity::class.java)
        if (maxScoreIdx < 0 || maxScoreIdx > 6) {
            intent.putExtra("result", "noResult")
        } else {
            intent.putExtra("result", classList[maxScoreIdx].toString())
        }
        intent.putExtra("image", bytes)
        startActivity(intent)
    }

    private fun saveImage(bytes: ByteArray) {
        val outStream: FileOutputStream
        try {
            // 이미지 저장하기
            val fileName = "MEONGNYANG_" + System.currentTimeMillis() + ".jpg"
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                fileName
            )
            outStream = FileOutputStream(file)
            outStream.write(bytes)
            outStream.close()
            Toast.makeText(this@EyeCameraActivity, "촬영한 사진이 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // 갤러리 열기
    private val OPEN_GALLERY = 1
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = ("image/*")
        startActivityForResult(intent, OPEN_GALLERY)
    }

    fun assetFilePath(context: Context, assetName: String?): String? {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName!!).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    // 갤러리에서 사진 가져오기
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val classList = mutableMapOf<Int, String>()
        classList[0] = "결막염"
        classList[1] = "궤양성각막질환"
        classList[2] = "백내장"
        classList[3] = "비궤양성각막질환"
        classList[4] = "안구무증상"
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_GALLERY) {
                var currentImageUrl: Uri? = data?.data

                try {
                    // resultActivity에 이미지 보내기
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, currentImageUrl)
                    val stream = ByteArrayOutputStream()
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 2 // 1/2 사이즈로 보여주기
                    val width = bitmap.width
                    val height = bitmap.height
                    val newWidth = 224
                    val newHeight = 224
                    val scaleWidth = (newWidth.toFloat()) / width
                    val scaleHeight = (newHeight.toFloat()) / height

                    val matrix = Matrix()
                    matrix.postScale(scaleWidth, scaleHeight)
                    matrix.postRotate(90f)

                    val resizedBitmap: Bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
                    val inputImg = TensorImage.fromBitmap(resizedBitmap)
                    val normalization = imageProcess(inputImg)
                    val resultImg = normalization!!.bitmap

                    // model
                    if (mModule == null) {

                        mModule = LiteModuleLoader.load(assetFilePath(this, "model2.ptl"))
                    }

                    val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                        resultImg,
                        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB
                    )

                    val outputTensor = mModule!!.forward(IValue.from(inputTensor)).toTensor()
                    val scores = outputTensor.dataAsFloatArray

                    var maxScore = -Float.MAX_VALUE
                    var maxScoreIdx = -1
                    for (i in scores.indices) {
                        if (scores[i] > maxScore) {
                            maxScore = scores[i]
                            maxScoreIdx = i
                        }
                    }
                    Log.d("model: scores[0]", (scores[0] * 100.0f).toString())
                    Log.d("model: scores[1]", (scores[1] * 100.0f).toString())
                    Log.d("model: scores[2]", (scores[2] * 100.0f).toString())
                    Log.d("model: scores[3]", (scores[3] * 100.0f).toString())
                    Log.d("model: scores[4]", (scores[4] * 100.0f).toString())
                    Log.d("model: maxScore", maxScore.toString())
                    Log.d("model: number", (maxScoreIdx+1).toString())

                    resizedBitmap.compress(Bitmap.CompressFormat.PNG, 60, stream)
                    val bytes = stream.toByteArray()

                    val intent = Intent(this, EyeResultActivity::class.java)
                    intent.putExtra("image", bytes)
                    intent.putExtra("result", classList[maxScoreIdx].toString())
                    startActivity(intent)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Log.d("ActivityResult", "something wrong")
            }
        }
    }

    // 자동 초점 기능
    private fun setCamFocusMode() {
        if (null == camera) {
            return
        }

        val parameters: Camera.Parameters = camera!!.parameters
        val focusModes: List<String> = parameters.supportedFocusModes
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }
        camera!!.parameters = parameters
    }


    private fun grayScale(orgBitmap: Bitmap): Bitmap {
        val width = orgBitmap.width
        val height = orgBitmap.height
        val bmpGrayScale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444)
        val canvas = Canvas(bmpGrayScale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorMatrixFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorMatrixFilter
        canvas.drawBitmap(orgBitmap, 0f, 0f, paint)
        return bmpGrayScale
    }

    // 이미지 전처리
    private fun imageProcess(input: TensorImage): TensorImage? {
        val PROBABILITY_MEAN = 0.23486832f
        val PROBABILITY_STD = 0.23325847f

        val imageProcessor: ImageProcessor = ImageProcessor.Builder()
            .add(NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD))
            .build()

        return imageProcessor.process(input)
    }
}