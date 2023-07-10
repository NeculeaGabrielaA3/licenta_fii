package com.example.loginregister.imageprocessing.editor

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.loginregister.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class SaturationControlFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var percentageTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var yourBitmap: Bitmap
    private lateinit var photoEditingActivity: PhotoEditingActivity
    private lateinit var adjustedBitmap: Bitmap
    private lateinit var smallerBitmap: Bitmap
    private lateinit var yourLastFilter: String
    private var percentage: Int = 50


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PhotoEditingActivity) {
            photoEditingActivity = context
        }
    }

    fun updateSeekBar(percentage: Int){
        seekBar.progress = percentage
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_saturation_control, container, false)
        seekBar = view.findViewById(R.id.saturation_seekbar)
        imageView = activity?.findViewById(R.id.imageView)!!
        percentageTextView = view.findViewById(R.id.percentage_textview)

        yourBitmap = photoEditingActivity.getLastBitmap()
        val originalWidth = yourBitmap.width
        val originalHeight = yourBitmap.height
        val scaleFactor = 0.5

        val newWidth = (originalWidth * scaleFactor).toInt()
        val newHeight = (originalHeight * scaleFactor).toInt()

        smallerBitmap = Bitmap.createScaledBitmap(yourBitmap, newWidth, newHeight, true)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        adjustedBitmap = withContext(Dispatchers.Default) { adjustSaturation(smallerBitmap, progress) }
                        imageView.setImageBitmap(adjustedBitmap)
                        percentage = progress

                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

                yourLastFilter = photoEditingActivity.getLastFilter()
                if(yourLastFilter != "Saturation"){
                    yourBitmap = photoEditingActivity.getLastBitmap()
                    val originalWidth = yourBitmap.width
                    val originalHeight = yourBitmap.height
                    val scaleFactor = 0.5

                    val newWidth = (originalWidth * scaleFactor).toInt()
                    val newHeight = (originalHeight * scaleFactor).toInt()

                    smallerBitmap = Bitmap.createScaledBitmap(yourBitmap, newWidth, newHeight, true)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                lifecycleScope.launch(Dispatchers.Main) {
                    val fullResBitmap = withContext(Dispatchers.Default) { adjustSaturation(yourBitmap, percentage) }
                    imageView.setImageBitmap(fullResBitmap)
                        photoEditingActivity.addImageVersion(fullResBitmap)
                        photoEditingActivity.addImageSaturationPercentage(percentage)
                        photoEditingActivity.addImageFilter("Saturation")
                }
            }
        })

        return view

    }

    fun adjustSaturation(originalBitmap: Bitmap, percentage: Int): Bitmap {
        val bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val src = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, src)

        // Convert the image to HSV
        val hsv = Mat()
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_RGB2HSV)

        // Split the channels
        val hsvChannels: MutableList<Mat> = ArrayList()
        Core.split(hsv, hsvChannels)

        // Get the saturation channel
        val saturation = hsvChannels[1]

        // Convert saturation to float
        val saturationF = Mat()
        saturation.convertTo(saturationF, CvType.CV_32F)

        // Adjust the saturation - suppose "value" is from your SeekBar
        Core.multiply(saturationF, Scalar(percentage / 100.0), saturationF)

        // Convert back to 8-bit
        saturationF.convertTo(saturation, CvType.CV_8UC1)

        // Merge the channels back together
        hsvChannels[1] = saturation
        Core.merge(hsvChannels, hsv)

        // Convert back to RGB for displaying
        Imgproc.cvtColor(hsv, src, Imgproc.COLOR_HSV2RGB)

        // Convert the result to Bitmap
        Utils.matToBitmap(src, bitmap)
        return bitmap
    }
}
