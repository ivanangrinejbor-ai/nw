package org.catrobat.catroid.paintroid.ui.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.catrobat.catroid.R

import org.catrobat.catroid.paintroid.common.MEDIA_GALLEY_URL
import org.catrobat.catroid.paintroid.web.MediaGalleryWebViewClient
import org.catrobat.catroid.paintroid.web.MediaGalleryWebViewClient.WebClientCallback

class CatroidMediaGalleryFragment : Fragment(), WebClientCallback {
    private var webView: WebView? = null
    private var listener: MediaGalleryListener? = null

    fun setMediaGalleryListener(listener: MediaGalleryListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_pocketpaint_webview, container, false)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.webview)
        webView?.apply {
            settings.javaScriptEnabled = true
            webViewClient = MediaGalleryWebViewClient(this@CatroidMediaGalleryFragment)
            settings.userAgentString = "Catrobat"
            loadUrl(MEDIA_GALLEY_URL)
            setDownloadListener { url, _, _, _, _ ->
                listener?.showProgressDialog()
                Glide.with(this@CatroidMediaGalleryFragment)
                    .asBitmap()
                    .load(url)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            listener?.bitmapLoadedFromSource(resource)
                            listener?.dismissProgressDialog()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            listener?.dismissProgressDialog()
                        }
                    })
                finish()
            }
        }
    }

    override fun onDestroy() {
        webView?.setDownloadListener(null)
        webView?.destroy()
        super.onDestroy()
    }

    override fun finish() {
        requireActivity().supportFragmentManager.popBackStack()
    }

    interface MediaGalleryListener {
        fun bitmapLoadedFromSource(loadedBitmap: Bitmap)

        fun showProgressDialog()

        fun dismissProgressDialog()
    }
}
