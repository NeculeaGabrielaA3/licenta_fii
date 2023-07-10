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
import org.opencv.core.*

class BrightnessControlFragment : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_brightness_control, container, false)
        seekBar = view.findViewById(R.id.brightness_seekbar)
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
                        adjustedBitmap = withContext(Dispatchers.Default) { adjustBrightness(smallerBitmap, progress) }
                        imageView.setImageBitmap(adjustedBitmap)
                        percentage = progress

                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                yourLastFilter = photoEditingActivity.getLastFilter()
                if(yourLastFilter != "Brightness"){
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
                    val fullResBitmap = withContext(Dispatchers.Default) { adjustBrightness(yourBitmap, percentage) }
                    imageView.setImageBitmap(fullResBitmap)
                    photoEditingActivity.addImageVersion(fullResBitmap)
                    photoEditingActivity.addImageBrightnessPercentage(percentage)
                    photoEditingActivity.addImageFilter("Brightness")
                }
            }
        })

        return view
    }

    fun adjustBrightness(originalBitmap: Bitmap, percentage: Int): Bitmap {
        var newBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (newBitmap.config != Bitmap.Config.ARGB_8888) {
            newBitmap = newBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        val mat = Mat(newBitmap.height, newBitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(newBitmap, mat)

        val scale = if (percentage >= 50) {
            1 + (percentage - 50) * 0.02
        } else {
            -(percentage * 0.02)
        }

        Core.convertScaleAbs(mat, mat, scale)

        val brightnessBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, brightnessBitmap)

        return brightnessBitmap

    }

}
