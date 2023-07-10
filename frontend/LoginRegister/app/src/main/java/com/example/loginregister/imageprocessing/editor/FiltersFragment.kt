package com.example.loginregister.imageprocessing.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginregister.R
import kotlinx.coroutines.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


class FiltersFragment : Fragment(), FilterAdapter.OnFilterClickListener{

    private lateinit var recyclerView: RecyclerView
    private lateinit var filterAdapter: FilterAdapter
    private lateinit var imageView: ImageView
    private lateinit var yourBitmap: Bitmap
    private lateinit var photoEditingActivity: PhotoEditingActivity

    private val filters = listOf("Grayscale", "Sepia", "Vignette", "Pixelate", "Oil Painting", "Pencil Effect")
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PhotoEditingActivity) {
            photoEditingActivity = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filters, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        filterAdapter = FilterAdapter(filters, this)
        recyclerView.adapter = filterAdapter

        val bundle = arguments
        val data = bundle?.getString("imageUri")

        yourBitmap = uriToBitmap(requireContext(), Uri.parse(data))

        return view
    }

    override fun onFilterClick(filter: String) {

        if (photoEditingActivity.getLastFilter() == filter) return

        imageView = activity?.findViewById(R.id.imageView)!!
        yourBitmap = photoEditingActivity.getLastBitmap()

        when(filter) {
            "Grayscale" -> {
                convertToGrayscale(yourBitmap)
            }
            "Sepia" -> {
                applySepiaFilter(yourBitmap)
            }
            "Vignette" -> {
                applyVignetteFilter(yourBitmap)
            }
            "Pixelate" -> {
                applyPixelateFilter(yourBitmap, 10)
            }
            "Oil Painting" -> {
                applyOilPaintingFilter(yourBitmap, 11)
            }
            "Pencil Effect" -> {
                applyPencilEffectFilter(yourBitmap)
            }
        }
    }

    private suspend fun convertImage(originalBitmap: Bitmap) = coroutineScope.async(Dispatchers.IO) {

        var newBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (newBitmap.config != Bitmap.Config.ARGB_8888) {
            newBitmap = newBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val mat = Mat()
        Utils.bitmapToMat(newBitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, resultBitmap)

        return@async resultBitmap
    }.await()

    private suspend fun applySepiaFilterF(originalBitmap: Bitmap) = coroutineScope.async(Dispatchers.IO) {
        var newBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (newBitmap.config != Bitmap.Config.ARGB_8888) {
            newBitmap = newBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        val mat = Mat(newBitmap.height, newBitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(newBitmap, mat)

        val sepiaKernel = Mat(4, 4, CvType.CV_32F)
        sepiaKernel.put(0, 0, /*R*/ 0.393, 0.769, 0.189, 0.0)
        sepiaKernel.put(1, 0, /*G*/ 0.349, 0.686, 0.168, 0.0)
        sepiaKernel.put(2, 0, /*B*/ 0.272, 0.534, 0.131, 0.0)
        sepiaKernel.put(3, 0, /*A*/ 0.0, 0.0, 0.0, 1.0)

        Core.transform(mat, mat, sepiaKernel)

        val sepiaBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, sepiaBitmap)

        return@async sepiaBitmap
    }.await()

    private suspend fun applyPixelateFilterF(originalBitmap: Bitmap, pixelSize: Int) = coroutineScope.async(Dispatchers.IO) {
        var newBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (newBitmap.config != Bitmap.Config.ARGB_8888) {
            newBitmap = newBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        val mat = Mat()
        Utils.bitmapToMat(newBitmap, mat)

        val smallMat = Mat()
        val largeMat = Mat()
        Imgproc.resize(mat, smallMat, Size(), 1.0 / pixelSize, 1.0 / pixelSize, Imgproc.INTER_LINEAR)
        Imgproc.resize(smallMat, largeMat, mat.size(), 0.0, 0.0, Imgproc.INTER_NEAREST)

        val pixelateBitmap = Bitmap.createBitmap(largeMat.cols(), largeMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(largeMat, pixelateBitmap)

        return@async pixelateBitmap

    }.await()

    private suspend fun applyVignetteFilterF(originalBitmap: Bitmap) =
        coroutineScope.async(Dispatchers.IO) {
        val newBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

        val mat = Mat(newBitmap.height, newBitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(newBitmap, mat)

        val cols = mat.cols()
        val rows = mat.rows()

        val kernelX = Imgproc.getGaussianKernel(cols, cols / 2.0)
        val kernelY = Imgproc.getGaussianKernel(rows, rows / 2.0)

        val kernelXY = Mat()
        Core.gemm(kernelY, kernelX.t(), 1.0, Mat(), 0.0, kernelXY)

        val minMax = Core.minMaxLoc(kernelXY)
        val minVal = minMax.minVal
        val maxVal = minMax.maxVal

        Core.subtract(kernelXY, Scalar(minVal), kernelXY)
        Core.multiply(kernelXY, Scalar(255.0 / (maxVal - minVal)), kernelXY)

        val vignetteMat = Mat()
        kernelXY.convertTo(vignetteMat, CvType.CV_8UC1)

        Imgproc.cvtColor(vignetteMat, vignetteMat, Imgproc.COLOR_GRAY2BGR)
        Imgproc.cvtColor(vignetteMat, vignetteMat, Imgproc.COLOR_BGR2BGRA)

        Core.multiply(mat, vignetteMat, mat, 1.0 / 255.0)

        val vignetteBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, vignetteBitmap)

        return@async vignetteBitmap
    }.await()

    private suspend fun applyOilPaintingFilterF(originalBitmap: Bitmap, size: Int) = coroutineScope.async(Dispatchers.IO) {
        var newBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (newBitmap.config != Bitmap.Config.ARGB_8888) {
            newBitmap = newBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        val mat = Mat(newBitmap.height, newBitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(newBitmap, mat)

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)

        val result = Mat()
        val processed = Mat()

        // Erode and dilate to enhance edges
        Imgproc.erode(mat, processed, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size((size * 2 + 1).toDouble(), (size * 2 + 1).toDouble())))
        Imgproc.dilate(processed, processed, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size((size * 2 + 1).toDouble(), (size * 2 + 1).toDouble())))

        // Blur to reduce color palette
        Imgproc.medianBlur(processed, result, size)

        // Convert back to ARGB
        Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGBA)

        val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(result, resultBitmap)

        return@async resultBitmap
    }.await()

    private suspend fun applyPencilEffectF(originalBitmap: Bitmap) = coroutineScope.async(Dispatchers.IO) {
        var newBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (newBitmap.config != Bitmap.Config.ARGB_8888) {
            newBitmap = newBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        val mat = Mat(newBitmap.height, newBitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(newBitmap, mat)

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)

        val result = Mat()
        val edges = Mat()

        // Applying a bilateral filter to reduce color palette
        Imgproc.bilateralFilter(mat, result, 9, 9.0 * 2, 9.0 / 2, Core.BORDER_DEFAULT)

        // Applying an edge detection to create a mask with edges
        Imgproc.Canny(mat, edges, 10.0, 70.0)

        // Converting the edges to 3 channels image
        Imgproc.cvtColor(edges, edges, Imgproc.COLOR_GRAY2BGR)

        // Convert the result to 3 channels before bitwise_and operation
        Imgproc.cvtColor(result, result, Imgproc.COLOR_GRAY2BGR)

        // Blending the stylized image with the edges mask
        Core.bitwise_and(result, edges, result)

        val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(result, resultBitmap)

        return@async resultBitmap
    }.await()

    private fun convertToGrayscale(originalBitmap: Bitmap) {
        coroutineScope.launch {
            try {
                val image = convertImage(originalBitmap)

                imageView.setImageBitmap(image)
                photoEditingActivity.addImageVersion(image)
                photoEditingActivity.setLastImageWithoutBrightness(image)
                photoEditingActivity.addImageFilter("Grayscale")
            } catch (e: Exception) {

                Log.e("OpenCV", "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun applySepiaFilter(originalBitmap: Bitmap) {
        coroutineScope.launch {
            try {
                val image = applySepiaFilterF(originalBitmap)
                imageView.setImageBitmap(image)
                photoEditingActivity.addImageVersion(image)
                photoEditingActivity.setLastImageWithoutBrightness(image)
                photoEditingActivity.addImageFilter("Sepia")

            } catch (e: Exception) {
                Log.e("OpenCV", "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun applyPixelateFilter(originalBitmap: Bitmap, pixelSize: Int) {
        coroutineScope.launch {
            try {
                val image = applyPixelateFilterF(originalBitmap, pixelSize)

                imageView.setImageBitmap(image)
                photoEditingActivity.addImageVersion(image)
                photoEditingActivity.setLastImageWithoutBrightness(image)
                photoEditingActivity.addImageFilter("Pixelate")

            } catch (e: Exception) {
                Log.e("OpenCV", "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun applyOilPaintingFilter(originalBitmap: Bitmap, k: Int) {
        coroutineScope.launch {
            try {
                val image = applyOilPaintingFilterF(originalBitmap, k)

                imageView.setImageBitmap(image)
                photoEditingActivity.addImageVersion(image)
                photoEditingActivity.setLastImageWithoutBrightness(image)
                photoEditingActivity.addImageFilter("Oil Painting")

            } catch (e: Exception) {
                Log.e("OpenCV", "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun applyPencilEffectFilter(originalBitmap: Bitmap) {
        coroutineScope.launch {
            try {
                val image = applyPencilEffectF(originalBitmap)
                imageView.setImageBitmap(image)
                photoEditingActivity.addImageVersion(image)
                photoEditingActivity.setLastImageWithoutBrightness(image)
                photoEditingActivity.addImageFilter("Pencil Effect")

            } catch (e: Exception) {
                Log.e("OpenCV", "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun applyVignetteFilter(originalBitmap: Bitmap) {
        coroutineScope.launch {
            try {
                val image = applyVignetteFilterF(originalBitmap)
                imageView.setImageBitmap(image)
                photoEditingActivity.addImageVersion(image)
                photoEditingActivity.setLastImageWithoutBrightness(image)
                photoEditingActivity.addImageFilter("Vignette")
            } catch (e: Exception) {
                Log.e("OpenCV", "Error: ${e.localizedMessage}")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageView = activity?.findViewById(R.id.imageView)!!

    }

    private fun uriToBitmap(context: Context, selectedFileUri: Uri): Bitmap {
        return try {
            if(Build.VERSION.SDK_INT < 28) { // If Android version is less than Pie (9.0)
                MediaStore.Images.Media.getBitmap(
                    context.contentResolver,
                    selectedFileUri
                )
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, selectedFileUri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_background)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        init {
            if (!OpenCVLoader.initDebug()) {
                Log.d("OpenCV", "OpenCV not loaded")
            } else {
                Log.d("OpenCV", "OpenCV loaded")
            }
        }
    }
}
