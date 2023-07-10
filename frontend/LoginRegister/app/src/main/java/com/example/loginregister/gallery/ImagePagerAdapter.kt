package com.example.loginregister.gallery

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.example.loginregister.R
import com.github.chrisbanes.photoview.PhotoView

class ImagePagerAdapter(private val context: Context, private val imageList: List<Image>) : PagerAdapter() {

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return imageList.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.pager_item, container, false) // pager_item should contain your PhotoView

        val imageView = view.findViewById<PhotoView>(R.id.full_screen_image_view)
        Glide.with(context).load(imageList[position].url).into(imageView)

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ImageView)
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    fun updateImageUri(imagePosition: Int, editedImageUri: String?) {
        if (editedImageUri != null) {
            imageList[imagePosition].url = editedImageUri
        }
        notifyDataSetChanged()
    }

}
