package com.iwedia.cltv.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.hardware.usb.UsbManager
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.channel.TunerType
import core_entities.Error
import data_type.GList
import listeners.AsyncReceiver
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*


class Utils {
    companion object {
        /**
         * Cached android display metrics
         */
        private var metrics: DisplayMetrics? = null
        val TAG = Utils::class.java.simpleName
        /**
         * Load channel logo path
         *
         * @param channelName
         * @param channelId
         * @return
         */

        fun isATVChannel(tvChannel: TvChannel?): Boolean {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "isATVChannel: tunerType=${tvChannel?.tunerType}  name=${tvChannel?.tunerType?.name}")
            return if (tvChannel != null)
                (tvChannel.tunerType == TunerType.ANALOG_TUNER_TYPE)
            else
                false
        }

        fun loadLogoUrl(channelName: String, channelId: Long): String? {
            val channelLogoUri = TvContract.buildChannelLogoUri(channelId)
            val fileName = "/" + channelName.replace(' ', '_') + channelId.toString()
            try {
                val inputStream: InputStream =
                    ReferenceApplication.applicationContext().contentResolver.openInputStream(
                        channelLogoUri
                    )!!
                val outputStream: OutputStream =
                    FileOutputStream(
                        File(
                            ReferenceApplication.applicationContext().filesDir,
                            fileName
                        )
                    )
                copy(inputStream, outputStream)
                inputStream.close()
                outputStream.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return ReferenceApplication.applicationContext().filesDir.toString() + fileName
        }

        /**
         * Copy input stream into the output stream
         *
         * @param is input stream
         * @param os output stream
         * @throws IOException
         */
        @Throws(IOException::class)
        private fun copy(`is`: InputStream, os: OutputStream) {
            val buffer = ByteArray(1024)
            var len: Int
            while (`is`.read(buffer).also { len = it } != -1) {
                os.write(buffer, 0, len)
            }
        }

        fun getChannelLogoBitmap(context: Context, channelLogoPath: String): Bitmap? {

            var logoBytes: ByteArray = ByteArray(1024)

            try {
                val fd: AssetFileDescriptor =
                    context.contentResolver.openAssetFileDescriptor(
                        Uri.parse(channelLogoPath),
                        "rw"
                    )!!

                val os: OutputStream = fd.createOutputStream()
                os.write(logoBytes)
                os.close()
                fd.close()

                return BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size)
            } catch (e: IOException) {
                // Handle error cases.
            }

            return null
        }

        /**
         * loadWithGlide is method which is used in loadImage().
         * * Explanation why .error() must be used here: https://bumptech.github.io/glide/doc/debugging.html#you-cant-start-or-clear-loads-in-requestlistener-or-target-callbacks
         * @author Boris Tirkajla
         */
        private fun loadWithGlide(
            imageView: ImageView,
            imagePath: Any?,
            shouldCompressImage: Boolean,
            onSuccess: () -> Unit,
            onFailure: () -> Unit,
            isFadeEnabled: Boolean
        ){
            Glide
                .with(ReferenceApplication.applicationContext())
                .load(imagePath)
                .override(if (shouldCompressImage) 500 else 0)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .centerInside()
                .transition(if(isFadeEnabled) DrawableTransitionOptions.withCrossFade(150) else DrawableTransitionOptions())
                .listener(object : RequestListener<Drawable>{
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        onFailure()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        onSuccess()
                        return false
                    }
                })
                .error(Glide.with(ReferenceApplication.applicationContext()).load(imagePath))
                .into(imageView)
        }

        /**
         * Used for loading images from the resources.
         */
        fun loadImage(
            context: Context? = null,
            view: ImageView,
            resource: Int,
            onSuccess: () -> Unit = {},
            onFailure: () -> Unit = {},
            onComplete: () -> Unit = {}

        ) {
            Glide.with(context ?: view.context) //java.lang.IllegalArgumentException: You cannot start a load for a destroyed activity - Here it was important to send the current context of the app
                .load(resource)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .centerInside()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        onFailure()
                        onComplete()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        onSuccess()
                        onComplete()
                        return false
                    }
                })
                .into(view)
        }

        /**
         * @param isFadeEnabled default value is true.
         * * If set to true, photo will slowly appear in view which was passed. That transition lasts 150 ms.
         * * If set to false, photo will immediately appear when it is ready to be shown.
         * @param shouldCompressImage used to define whether image should be compressed or not, by default it is compressed to have faster loading. shouldCompressImage = false is used in CustomDetails class to load Channel image - if compressed image won't have proper quality.
         */
        @SuppressLint("ResourceType")
        fun loadImage(path: String, view: ImageView,  callback: AsyncReceiver? = null, shouldCompressImage: Boolean = true, isFadeEnabled: Boolean = true) {

            if (path.isNotEmpty()) {
                if (path.contains("http")) {

                    loadWithGlide(
                        imageView = view,
                        imagePath = path,
                        shouldCompressImage = shouldCompressImage,
                        onSuccess = {
                            callback?.onSuccess()
                        },
                        onFailure = {
                            callback?.onFailed(Error(-111, "Image not loaded"))
                        },
                        isFadeEnabled = isFadeEnabled
                    )
                } else {
                    if (path.contains("android.resource:")) {
                        val uri = Uri.parse(path)

                        loadWithGlide(
                            imageView = view,
                            imagePath = uri,
                            shouldCompressImage = shouldCompressImage,
                            onSuccess = {
                                callback?.onSuccess()
                            },
                            onFailure = {
                                callback?.onFailed(Error(-111, "Image not loaded"))
                            },
                            isFadeEnabled = isFadeEnabled
                        )
                    }

                    //Images from tif database
                    if (path.contains("content")) {
                        try {
                            // to catch the exception - so we can check weather we can load this file
                            ReferenceApplication.applicationContext().getContentResolver()
                                .openInputStream(Uri.parse(path))?.close()

                            loadWithGlide(
                                imageView = view,
                                imagePath = path,
                                shouldCompressImage = shouldCompressImage,
                                onSuccess = {
                                    callback?.onSuccess()
                                },
                                onFailure = {
                                    callback?.onFailed(Error(-111, "Image not loaded"))
                                },
                                isFadeEnabled = isFadeEnabled
                            )
                        } catch (e: SQLiteException) {
                            // this comes when api level is less than 29
                            callback?.onFailed(Error(-111, "Image not loaded"))
                        } catch (e: FileNotFoundException) {
                            // it comes from some side loaded stream (file.isExist() is missing to cover this in some case)
                            callback?.onFailed(Error(-111, "Image not exist"))
                        } catch (e: Exception) {
                            // for safe build
                            callback?.onFailed(Error(-111, "Image not loaded"))
                        }
                        return
                    }

                    if (path.contains("no_image")) {
                        view.setBackgroundColor(Color.TRANSPARENT)
                        return
                    }

                    var logoImagePath = File(path)
                    if (logoImagePath != null && logoImagePath.exists()) {
                        val uri = Uri.fromFile(logoImagePath)

                        loadWithGlide(
                            imageView = view,
                            imagePath = logoImagePath,
                            shouldCompressImage = shouldCompressImage,
                            onSuccess = {
                                callback?.onSuccess()
                            },
                            onFailure = {
                                callback?.onFailed(Error(-111, "Image not loaded"))
                            },
                            isFadeEnabled = isFadeEnabled
                        )
                    } else {
                        if (path.contains("raw")) {
                            view.setImageResource(ReferenceApplication.applicationContext().resources.getIdentifier(path.split(".").get(2),"raw", ReferenceApplication.applicationContext().packageName))
                        } else {
                            var bitmap =
                                getChannelLogoBitmap(
                                    ReferenceApplication.applicationContext(),
                                    path
                                )
                            if (bitmap == null) {
                                callback?.onFailed(Error(-111, "Invalid image."))
                                return
                            }

                            view.setImageBitmap(bitmap)
                            callback?.onSuccess()
                        }
                    }
                }
            } else {
                callback?.onFailed(Error(-111, "Image not loaded"))
            }
        }


        fun getTvInputName(input: TvInputInfo): String {

            var retval: String =
                input.serviceInfo.loadLabel(ReferenceApplication.applicationContext().packageManager) as String
            if (retval.lowercase() == "tuner") {
                retval = "Tv Channels"
            }

            return retval
        }

        fun convertDpToPixel(dp: Double): Double {
            if (metrics == null) {
                metrics = ReferenceApplication.applicationContext().resources.displayMetrics
            }

            return dp * (metrics!!.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
        }

        /**
         * Is data type
         *
         *
         * Check if object is requested class type
         *
         * @param object object
         * @param clazz  class
         * @return is data type
         */
        fun isDataType(obj: Any?, clazz: Class<*>): Boolean {
            if (obj != null) {
                if (obj.javaClass == clazz) {
                    return true
                }
                if (obj.javaClass.isAssignableFrom(clazz)) {
                    return true
                }
                if (clazz.isAssignableFrom(obj.javaClass)) {
                    return true
                }
            }
            return false
        }

        /**
         * Is list data type
         *
         *
         * Check if objects in list are requested class type
         *
         * @param object list object
         * @param clazz  class type
         * @return is list data type
         */
        fun isListDataType(`object`: Any?, clazz: Class<*>): Boolean {
            if (`object` is List<*>) {
                if (!`object`.isEmpty()) {
                    return isDataTypeFromGAndroidUtils(`object`[0], clazz)
                } else {
                    true
                }
            } else if (`object` is GList<*>) {
                if (`object`.value.size > 0) {
                    return isDataTypeFromGAndroidUtils(`object`.value[0], clazz)
                } else {
                    true
                }
            }

            return false
        }

        /**
         * this method is copied from GAndroidUtils and it is temporary solution, if removed and used directly from GAndroid in isListDataType class sometimes java.lang.NoClassDefFoundError: com.iwedia.guide.android.tools.GAndroidUtils will occur.
         */
        private fun isDataTypeFromGAndroidUtils(obj: Any?, clazz: Class<*>): Boolean {
            if (obj != null) {
                if (obj.javaClass == clazz) {
                    return true
                }
                if (obj.javaClass.isAssignableFrom(clazz)) {
                    return true
                }
                if (clazz.isAssignableFrom(obj.javaClass)) {
                    return true
                }
            }
            return false
        }

        /**
         * Get dimension
         */
        fun getDimens(dimen: Int): Float {
            return ReferenceApplication.applicationContext().resources.getDimension(dimen)
        }

        private val PACKAGE_NAME = BuildConfig.APPLICATION_ID
        fun restartApp(){
            ReferenceApplication.applicationContext().let { context->
                if (isAppOnForeground(context, PACKAGE_NAME)) {
                    var launchIntent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
                    if (launchIntent == null) {
                        launchIntent =
                            context.packageManager.getLeanbackLaunchIntentForPackage(PACKAGE_NAME)
                    }
                    launchIntent?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(it)
                        Runtime.getRuntime().exit(0)
                    }
                } else {
                    ReferenceApplication.getActivity().finishAffinity()
                }
            }
        }

        /**
         * Checks if the application is on foreground
         *
         * @param context           context
         * @param appPackageName    application package name
         * @return true if the application is on foreground
         */
        private fun isAppOnForeground(context: Context, appPackageName: String): Boolean {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return false
            for (appProcess in appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == appPackageName) {
                    return true
                }
            }
            return false
        }


        /**
         * Get dimension in pixel size
         */
        fun getDimensInPixelSize(dimen: Int): Int {
            return ReferenceApplication.applicationContext().resources.getDimensionPixelSize(dimen)
        }

        /**
         * Get string from resources
         */
        fun getString(id: Int): String {
            return ReferenceApplication.applicationContext().getString(id)
        }

        /**
         * Get time string from seconds
         *
         * @param totalSeconds Total seconds
         * @return Time string for example "07:34:22"
         */
        fun getTimeStringFromSeconds(totalSeconds: Long): String? {
            var hours = totalSeconds / 3600 % 24
            var minutes = totalSeconds / 60 % 60
            var seconds = totalSeconds % 60
            if (totalSeconds < 0) {
                hours = 0
                minutes = 0
                seconds = 0
            }
            return if (hours > 0) {
                (if (hours < 10) "0$hours" else hours).toString() + ":" + (if (minutes < 10) "0$minutes" else minutes) + ":" + if (seconds < 10) "0$seconds" else seconds
            } else {
                (if (minutes < 10) "0$minutes" else minutes).toString() + ":" + if (seconds < 10) "0$seconds" else seconds
            }
        }

        fun clickAnimation(view: View) {
            val mAnimation: Animation = AlphaAnimation(1f, 0f)
            mAnimation.setDuration(20)
            mAnimation.setInterpolator(LinearInterpolator())
            mAnimation.setRepeatMode(Animation.REVERSE)
            mAnimation.setRepeatCount(1)
            view.startAnimation(mAnimation)
        }


        fun setAllParentsClip(view: View, enabled: Boolean) {
            var view = view
            while (view.parent != null && view.parent is ViewGroup) {
                val viewGroup = view.parent as ViewGroup
                viewGroup.clipChildren = enabled
                viewGroup.clipToPadding = enabled
                view = viewGroup
            }
        }

        fun viewClickAnimation(view: View?, animationListener: AnimationListener) {
            view?.let {
                it.clearAnimation()
                val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f)
                val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f)

                scaleDownY.repeatCount = 1
                scaleDownX.repeatCount = 1

                scaleDownY.repeatMode = ValueAnimator.REVERSE
                scaleDownX.repeatMode = ValueAnimator.REVERSE

                scaleDownX.duration = 200
                scaleDownY.duration = 200


                val scaleDownAnim = AnimatorSet()
                scaleDownAnim.play(scaleDownX).with(scaleDownY)
                scaleDownAnim.start()

                scaleDownAnim.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(scaleDown: Animator) {
                    }

                    override fun onAnimationEnd(scaleDown: Animator) {
                        if (animationListener != null) {
                            animationListener.onAnimationEnd()
                            it.clearAnimation()
                        }
                    }

                    override fun onAnimationCancel(scaleDown: Animator) {
                        TODO("Not yet implemented")
                    }

                    override fun onAnimationRepeat(scaleDown: Animator) {
                        TODO("Not yet implemented")
                    }
                })
            }

        }

        fun focusAnimation(view: View, preventClip:Boolean=false, scale: Scale = Scale.DEFAULT) {
            if(!preventClip) setAllParentsClip(view, false)
            view.animate()
                .scaleY(scale.value)
                .scaleX(scale.value)
                .setDuration(150)
                .setInterpolator(LinearOutSlowInInterpolator())
                .start()
        }

        fun focusAnimation(
            view: MaterialCardView,
            scaleX: Scale,
            scaleY: Scale,
            elevation: Float,
            duration: Long = 150L
        ) {
            view.animate()
                .scaleY(scaleY.value)
                .scaleX(scaleX.value)
                .setUpdateListener {
                    if (it.animatedFraction > view.cardElevation / elevation) {
                        // if this check is not done there would be blink effect because fraction starts from the 0f and 0*something gives 0 - so everytime elevation would start from 0 which creates blink effect
                        view.cardElevation = elevation * it.animatedFraction
                    }
                }
                .setDuration(duration)
                .setInterpolator(LinearOutSlowInInterpolator())
                .start()
        }


        /**
         * method used to animate View's height.
         */
        fun animateContentHeight(view: View, currentHeight: Int, newHeight: Int, duration: Long = 500L) : Animator{
            val slideAnimator = ValueAnimator.ofInt(currentHeight, newHeight).setDuration(duration)
            // We use an update listener which listens to each tick and manually updates the height of the view
            slideAnimator.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                view.layoutParams.height = value
                view.requestLayout()
            }
            // We use an animationSet to play the animation
            val animationSet = AnimatorSet()
            animationSet.interpolator = AccelerateDecelerateInterpolator()
            animationSet.play(slideAnimator)
            animationSet.start()
            return animationSet
        }

        enum class Scale(val value: Float){
            NORMAL(1f),
            DEFAULT(1.05f),
            EVENT_ITEM(1.165f),
            BUTTON(1.025f),
            CUSTOM_CARD(1.2f)
        }

        fun unFocusAnimation(view: View) {
            view.animate().scaleY(1.0f).scaleX(1.0f).setDuration(100).start()
            view.clearAnimation()
        }

        fun fadeInAnimation(view: View, durationInt: Long = 100) {
            view.animate().alpha(1f).setDuration(durationInt)
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator) {
                        view.visibility = View.INVISIBLE
                    }

                    override fun onAnimationEnd(p0: Animator) {
                        view.visibility = View.VISIBLE
                    }

                    override fun onAnimationCancel(p0: Animator) {
                    }

                    override fun onAnimationRepeat(p0: Animator) {
                    }

                })
        }

        fun fadeInAnimationForEmptyButtons(view: View, durationInt: Long = 100) {
            val scale = ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f)
            scale.fillAfter = true
            scale.duration = durationInt
            view.startAnimation(scale)
        }

        /**
         * Check if device has internet connection
         */
        fun checkForInternet(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (connectivityManager != null) {
                val capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                        return true
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                        return true
                    }
                }
            }

            return false
        }

        /**
         * Make gradient
         *
         * @param view        View
         * @param type        Type
         * @param orientation Orientation
         * @param startColor  Start color
         * @param centerColor Center color
         * @param endColor    End color
         */
        fun makeGradient(
            view: View,
            type: Int,
            orientation: GradientDrawable.Orientation?,
            startColor: Int,
            centerColor: Int,
            endColor: Int,
            centerX: Float,
            centerY: Float
        ) {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.gradientType = type
            gradientDrawable.orientation = orientation
            val colors = intArrayOf(startColor, centerColor, endColor)
            gradientDrawable.colors = colors
            gradientDrawable.setGradientCenter(centerX, centerY)

            view.background = gradientDrawable
        }

        fun makeGradient(
            view: View,
            type: Int,
            orientation: GradientDrawable.Orientation?,
            firstQuarter: Int,
            secondQuarter: Int,
            thirdQuarter: Int,
            fourthQuarter: Int,
            centerX: Float,
            centerY: Float
        ) {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.gradientType = type
            gradientDrawable.orientation = orientation
            val colors = intArrayOf(firstQuarter, secondQuarter, thirdQuarter, fourthQuarter)
            gradientDrawable.colors = colors
            gradientDrawable.setGradientCenter(centerX, centerY)

            view.background = gradientDrawable
        }

        fun makeGradient(
            view: View,
            type: Int,
            orientation: GradientDrawable.Orientation?,
            startColor: Int,
            endColor: Int,
            centerX: Float,
            centerY: Float
        ) {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.gradientType = type
            gradientDrawable.orientation = orientation
            val colors = intArrayOf(startColor, endColor)
            gradientDrawable.colors = colors
            gradientDrawable.setGradientCenter(centerX, centerY)

            view.background = gradientDrawable
        }

        /**
         * Add alpha
         *
         * @param originalColor Original hex color string
         * @param alpha         Alpha from 0 to 1
         * @return Nex hex color string including alpha
         */
        fun addAlpha(originalColor: String, alpha: Double): String? {
            var originalColor = originalColor
            val alphaFixed = Math.round(alpha * 255)
            var alphaHex = java.lang.Long.toHexString(alphaFixed)
            if (alphaHex.length == 1) {
                alphaHex = "0$alphaHex"
            }
            originalColor = originalColor.replace("#", "#$alphaHex")
            return originalColor
        }


        fun similarity(s1: String, s2: String): Double {
            var longer = s1.lowercase()
            var shorter = s2.lowercase()
            if (s1.length < s2.length) {
                longer = s2
                shorter = s1
            }
            val longerLength = longer.length
            return if (longerLength == 0) {
                1.0 /* both strings have zero length */
            } else (longerLength - getLevenshteinDistance(
                longer,
                shorter
            )) / longerLength.toDouble()
        }

        /**
         * LevenshteinDistance
         * copied from https://commons.apache.org/proper/commons-lang/javadocs/api-2.5/src-html/org/apache/commons/lang/StringUtils.html#line.6162
         */
        private fun getLevenshteinDistance(s: String?, t: String?): Int {
            var s = s
            var t = t
            require(!(s == null || t == null)) { "Strings must not be null" }
            var n = s.length // length of s
            var m = t.length // length of t
            if (n == 0) {
                return m
            } else if (m == 0) {
                return n
            }
            if (n > m) {
                // swap the input strings to consume less memory
                val tmp: String = s
                s = t
                t = tmp
                n = m
                m = t.length
            }
            var p = IntArray(n + 1) //'previous' cost array, horizontally
            var d = IntArray(n + 1) // cost array, horizontally
            var _d: IntArray //placeholder to assist in swapping p and d

            // indexes into strings s and t
            var i: Int // iterates through s
            var j: Int // iterates through t
            var t_j: Char // jth character of t
            var cost: Int // cost
            i = 0
            while (i <= n) {
                p[i] = i
                i++
            }
            j = 1
            while (j <= m) {
                t_j = t[j - 1]
                d[0] = j
                i = 1
                while (i <= n) {
                    cost = if (s[i - 1] == t_j) 0 else 1
                    // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                    d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost)
                    i++
                }

                // copy current distance counts to 'previous row' distance counts
                _d = p
                p = d
                d = _d
                j++
            }

            // our last action in the above loop was to switch d and p, so p now
            // actually has the most recent cost counts
            return p[n]
        }

        /**
         * Checks is USB device connected
         *
         * @return  true if the USB device is connected
         */
        fun isUsbConnected(): Boolean {
            var usbManager = ReferenceApplication.applicationContext()
                .getSystemService(Context.USB_SERVICE) as UsbManager
            return usbManager.deviceList.size > 0
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun findStoragePercentage(): Int{
            val storageManager: StorageManager = ReferenceApplication.applicationContext()
                .getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolumes: MutableList<StorageVolume> = storageManager.storageVolumes

            for (storageVolume in storageVolumes){
                var volumePath = storageVolume.directory
                if (volumePath != null){
                    if (!(storageVolume.directory.toString().contains("emulated"))) {     //invert for system memory
                        val statFs: StatFs = StatFs(volumePath.toString())
                        val availableBytes = statFs.availableBytes
                        val totalBytes = statFs.totalBytes
                        return 100 - (availableBytes.div(totalBytes.toFloat()) * 100).toInt()
                    }
                }
            }
            return 0
        }

        fun fileExists(context: Context, filename: String?): Boolean {
            val file = context.getFileStreamPath(filename)
            return if (file == null || !file.exists()) {
                false
            } else true
        }

        /**
         * Retrieve the preview programs associated with the given channel ID or, if ID is null,
         * return all programs associated with any channel.
         */
        @SuppressLint("RestrictedApi")
        fun getPreviewPrograms(context: Context, channelId: Long? = null): List<PreviewProgram> {
            val programs: MutableList<PreviewProgram> = mutableListOf()

            try {
                val cursor = context.contentResolver.query(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    PreviewProgram.PROJECTION,
                    null,
                    null,
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        val program = PreviewProgram.fromCursor(cursor)
                        if (channelId == null || channelId == program.channelId) {
                            programs.add(program)
                        }
                    } while (cursor.moveToNext())
                }
                cursor?.close()

            } catch (exc: IllegalArgumentException) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"GET PROGRAMS ERROR TAG", "Error retrieving preview programs", exc)
            }

            return programs
        }

        /**
         * Return the 12HR format time from the 24HR format
         */
        fun convert12Hr(time: String): String {
            val hh = time.slice(0..1).toInt()
            val hh_12: String
            val meridian: String
            if (hh < 12) {
                meridian = "AM"
                hh_12 = hh.toString()
            } else if (hh == 12) {
                meridian = "PM"
                hh_12 = "12"
            } else if (hh == 24) {
                meridian = "AM"
                hh_12 = "00"
            } else {
                meridian = "PM"
                hh_12 = (hh - 12).toString()
            }
            return "$hh_12${time.slice(2..4)}$meridian"
        }

        /**
         * returns context
         */
        fun getContext(): Context {
            return ReferenceApplication.applicationContext()
        }

        /**
         * Return the 12HR format time from the 24HR format of ReferenceTvEvents
         */
        fun convert12HrEventTime(time: String): String {
            if (time == "No start time defined") {
                return "No start time defined"
            }

            var startTime: String = if (time.length > 13) {
                time.slice(6..10)
            } else {
                time.slice(0..4)
            }

            var endTime: String = if (time.length > 13) {
                time.slice(6..10)
            } else {
                time.slice(8..12)
            }

            startTime = convert12Hr(startTime)
            endTime = convert12Hr(endTime)

            if (time.length > 13) {
                return "$startTime${time.slice(11..13)}$endTime"
            }
            return "$startTime${time.slice(5..7)}$endTime"
        }

        @Throws(XmlPullParserException::class, IOException::class)
        fun parseXML(parser: XmlPullParser): HashMap<String, String> {
            var stringsHashMap: HashMap<String, String> = HashMap()
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                var name: String
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> stringsHashMap
                    XmlPullParser.START_TAG -> {
                        name = parser.name
                        if (name == "string") {
                            stringsHashMap.put(
                                parser.getAttributeValue(null, "name"),
                                parser.nextText()
                            )
                        }
                    }
                }
                eventType = parser.next()
            }
            return stringsHashMap
        }


        /**
         * @param dropZone
         * @param draggedView
         * @return true if dropzone rect completely contains this dragged rect
         * */
        fun containsView(dropZone: View, draggedView: View?): Boolean {
            if (draggedView == null) return false

            // Create the Rect for the view where items will be dropped
            val pointA = IntArray(2)
            dropZone.getLocationOnScreen(pointA)
            val rectA =
                Rect(pointA[0], pointA[1], pointA[0] + dropZone.width, pointA[1] + dropZone.height)

            // Create the Rect for the view been dragged
            val pointB = IntArray(2)
            draggedView.getLocationOnScreen(pointB)
            val rectB = Rect(
                pointB[0],
                pointB[1],
                pointB[0] + draggedView.width,
                pointB[1] + draggedView.height
            )

            // Check if the dropzone currently contains the dragged view
            return rectA.contains(rectB)
        }

        fun isEvaluationFlavour(): Boolean {
            return BuildConfig.BUILD_TYPE.contains("eval")
        }

        fun isAtscFlavour(): Boolean {
            return BuildConfig.FLAVOR.contains("atsc")
        }

        /**
         * Returns the Evaluation Licence Expired.
         * startDate ,is the Build time, when apk is generated.
         * endDate , is Current system time.
         */
        fun isEvaluationLicenceExpired(startDate: Date, endDate: Date): Boolean {
            val startCalendar = Calendar.getInstance()
            startCalendar.time = startDate

            val endCalendar = Calendar.getInstance()
            endCalendar.time = endDate

            val diffYear = endCalendar[Calendar.YEAR] - startCalendar[Calendar.YEAR]
            val diffMonth = endCalendar[Calendar.MONTH] - startCalendar[Calendar.MONTH]

            return diffYear <= -1 || diffYear >= 1 || diffMonth <= -6 || diffMonth >= 6
        }

        /**
         * Check licence key
         */
        @SuppressLint("Range")
        fun checkLicenceFile(context: Context): Boolean {
            //READ LICENCE FROM DATABASE
            var activeLicence = getLicence(context)

            val `is`: InputStream = context.assets.open("licence_keys")
            val br = BufferedReader(InputStreamReader(`is`, StandardCharsets.UTF_8))
            var str: String?
            while (br.readLine().also { str = it } != null) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkLicenceFile: ####### SUPPORTED LICENCE $str")
                if (str.equals(activeLicence)) {
                    return true
                }
            }

            br.close()
            return false
        }

        @SuppressLint("Range")
        fun getLicence(context: Context): String {
            var activeLicence = ""
            val contentResolver: ContentResolver = context.contentResolver
            var cursor = contentResolver.query(
                Uri.parse("content://${"com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider"}/${"oem_customization"}"),
                null,
                null,
                null,
                null
            )
            if (cursor!!.count > 0) {
                cursor.moveToFirst()
                if (cursor.getString(cursor.getColumnIndex("licence_key")) != null) {
                    activeLicence = cursor.getString(cursor.getColumnIndex(
                        "licence_key"))
                }
                cursor.close()
            }

            return activeLicence
        }

        fun getUTF8String(strData: String): String {
            val byteArr = strData.toByteArray(Charsets.UTF_8)
            return String(byteArr, Charsets.UTF_8).replace("\uFFFD", "\'")
        }

        fun getStartEndDate(currentTime: Long): Pair<String, String> {
            var calendar = Calendar.getInstance()
            calendar.time = Date(currentTime)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            var startDate = calendar.time
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            var endDate = calendar.time
           return  startDate.time.toString() to endDate.time.toString()
        }

        fun getAudioChannelStringArray() : Array<String> {
            var audioChannelList = ReferenceApplication.applicationContext().resources.getStringArray(R.array.audio_channels)
            for (i in audioChannelList.indices) {
                if (!TextUtils.isEmpty(audioChannelList[i]) && audioChannelList[i].endsWith("#CH")) {
                    try {
                        audioChannelList[i] = audioChannelList[i].split("#").toTypedArray()[0]
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return audioChannelList
        }

        //check if a keycode passed is of a digit or not
        fun checkIfDigit(keyCode: Int): Boolean {
            when(keyCode){
                KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9,
                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9-> return true
            }
            return false
        }

        /**
         * Animates the background color of a [MaterialCardView] from a start color to an end color.
         *
         * @param startColor The starting color of the animation. Defaults to the card's current background color.
         * @param endColor The target color to which the background color will be animated.
         * @param duration The duration of the color animation in milliseconds. Defaults to 150 milliseconds.
         *
         * @return A [ValueAnimator] instance that can be used for additional customization or tracking the animation.
         */
        fun MaterialCardView.animateBackgroundColor(
            startColor: Int = cardBackgroundColor.defaultColor,
            endColor: Int,
            duration: Long = 150L
        ): ValueAnimator {
            // Create a color animator that interpolates between startColor and endColor
            val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)

            // Add a listener to update the card's background color during the animation
            colorAnimator.addUpdateListener { animator ->
                val animatedColor = animator.animatedValue as Int
                setCardBackgroundColor(animatedColor)
            }

            // Set the duration and interpolator for the color animation
            colorAnimator.duration = duration
            colorAnimator.interpolator = AccelerateDecelerateInterpolator()

            // Start the color animation
            colorAnimator.start()

            // Return the color animator, allowing further customization or tracking if needed
            return colorAnimator
        }
    }

}