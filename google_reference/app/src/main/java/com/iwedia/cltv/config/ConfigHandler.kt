package com.iwedia.cltv.config

import android.content.res.ColorStateList
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.iwedia.cltv.*
import com.iwedia.cltv.config.entities.*
import com.iwedia.cltv.config.entities.json.ConfigParamFont
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus

/**
 * Runtime configuration handler
 * @author Veljko Ilkic
 */
object ConfigHandler {

    private var configMap = mutableMapOf<Int, SceneConfig>()
    val TAG = javaClass.simpleName
    var isSetupStarted = false
    private lateinit var oemCustomizationObserver: ContentObserver

    @RequiresApi(Build.VERSION_CODES.R)
    fun setup(moduleProvider: ModuleProvider, languageCode: String, callback: AsyncReceiver) {

        if (isSetupStarted) {
            return
        }

        isSetupStarted = true

        if (!BuildConfig.FLAVOR.contains("mal_service"))
            moduleProvider.getUtilsModule().runOemCustomization(moduleProvider.getUtilsModule().jsonPath)

        //setup brand fonts manager
        ConfigFontManager.setup(moduleProvider.getUtilsModule())

        // setup config strings manager
        ConfigStringsManager.setup(languageCode)

        ConfigurableKeysManager.setup()
        var configurableKeysObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                ConfigurableKeysManager.setup()
                InformationBus.submitEvent(Event(Events.UPDATED_CONFIG_KEYS))
            }
        }
        ReferenceApplication.applicationContext().contentResolver.registerContentObserver(
            ContentProvider.CONFIGURABLE_KEYS_URI,
            true,
            configurableKeysObserver
        )


        //fti select input scene config
        var ftiSelectInputSceneConfig = ConfigJsonParser.loadJsonConfig(
            ReferenceWorldHandler.SceneId.FTI_SELECT_INPUT_SCAN,
            R.raw.fti_select_input_scene_json
        )
        registerSceneConfigParam(ftiSelectInputSceneConfig)

        //fti finish scan scene config
        var ftiFinishScanSceneConfig = ConfigJsonParser.loadJsonConfig(
            ReferenceWorldHandler.SceneId.FTI_FINISH_SCAN,
            R.raw.fti_finish_scene_json
        )
        registerSceneConfigParam(ftiFinishScanSceneConfig)

        //info banner scene config
        var infoBannerConfig = ConfigJsonParser.loadJsonConfig(
            ReferenceWorldHandler.SceneId.INFO_BANNER,
            R.raw.infobanner_scene_json
        )
        registerSceneConfigParam(infoBannerConfig)

        //for you scene widget
        var forYouWidgetSceneConfig = ConfigJsonParser.loadJsonConfig(
            ReferenceWorldHandler.WidgetId.FOR_YOU,
            R.raw.for_you_json
        )
        registerSceneConfigParam(forYouWidgetSceneConfig)

        //zap banner scene widget
        var zapBannerSceneConfig = ConfigJsonParser.loadJsonConfig(
            ReferenceWorldHandler.WidgetId.ZAP_BANNER,
            R.raw.zap_banner_scene_json
        )
        registerSceneConfigParam(zapBannerSceneConfig)

        //recordings scene widget
        var recordingsWidgetSceneConfig = ConfigJsonParser.loadJsonConfig(
            ReferenceWorldHandler.WidgetId.RECORDINGS,
            R.raw.recordings_json
        )
        registerSceneConfigParam(recordingsWidgetSceneConfig)

        //channel list scene widget
        var channelListWidgetSceneConfig = ConfigJsonParser.loadJsonConfig(
            ReferenceWorldHandler.WidgetId.CHANNEL_LIST,
            R.raw.channel_list_scene_json
        )
        registerSceneConfigParam(channelListWidgetSceneConfig)

        registerOemCustomizationObserver(moduleProvider)
        callback.onSuccess()
    }

    // Registers oem customization database observer
    private fun registerOemCustomizationObserver(moduleProvider: ModuleProvider) {
        oemCustomizationObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onChange(selfChange: Boolean) {
                //setup brand color manager
                ConfigColorManager.setup(moduleProvider.getUtilsModule())

                //setup brand fonts manager
                ConfigFontManager.setup(moduleProvider.getUtilsModule())

                //setup company info manager
                ConfigCompanyDetailsManager.setup(moduleProvider.getUtilsModule())
            }
        }
        ReferenceApplication.applicationContext().contentResolver.registerContentObserver(
            ContentProvider.OEM_CUSTOMIZATION_URI,
            true,
            oemCustomizationObserver
        )
    }

    /**
     * Register scene config param
     */
    fun registerSceneConfigParam(sceneConfigParam: SceneConfig) {
        configMap[sceneConfigParam.sceneId] = sceneConfigParam
    }

    /**
     * Get scene config param
     */
    fun getSceneConfigParam(sceneId: Int): SceneConfig? {
        return configMap[sceneId]
    }

    /**
     * Apply config param on view
     */
    fun applyConfig(view: View, configParam: ConfigParam) {

        if (configParam is ConfigParamFont) {
            resolveFont(view, configParam)
        }

        if (configParam is ConfigParamVisibility) {
            resolveVisibility(view, configParam)
        }

        if (configParam is ConfigParamText) {
            resolveText(view, configParam)
        }

        if (configParam is ConfigParamImage) {
            resolveImageSrc(view, configParam)
        }

        if (configParam is ConfigParamSize) {
            resolveSize(view, configParam)
        }

        if (configParam is ConfigParamAlignment) {
            resolveAlignment(view, configParam)
        }

        if (configParam is ConfigParamMargins) {
            resolveMargins(view, configParam)
        }

        if (configParam is ConfigParamColor) {
            resolveColor(view, configParam)
        }

        if (configParam is ConfigParamTextSize) {
            resolveTextSize(view, configParam)
        }
    }

    private fun resolveFont(view: View, configParam: ConfigParamFont) {
        if (view is TextView) {
            view.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                configParam.fontResource!!
            )
        } else if (view is Button) {
            view.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                configParam.fontResource!!
            )
        }
    }

    private fun resolveVisibility(view: View, configParam: ConfigParamVisibility) {
        if (configParam.visibility == ConfigParamVisibility.Visibility.VISIBLE) {
            view.visibility = View.VISIBLE
        } else if (configParam.visibility == ConfigParamVisibility.Visibility.INVISIBLE) {
            view.visibility = View.INVISIBLE
        } else if (configParam.visibility == ConfigParamVisibility.Visibility.GONE) {
            view.visibility = View.GONE
        }
    }

    private fun resolveText(view: View, configParam: ConfigParamText) {
        if (view is TextView) {
            view.text = configParam.text
        } else if (view is Button) {
            view.text = configParam.text
        }
    }

    private fun resolveImageSrc(view: View, configParam: ConfigParamImage) {
        if (view is ImageView) {
            if (configParam.image.isEmpty()) {
                return
            }

            Utils.loadImage(configParam.image, view, object : AsyncReceiver {
                override fun onFailed(error: Error?) {
                }

                override fun onSuccess() {
                }
            })
        }
    }

    private fun resolveSize(view: View, configParam: ConfigParamSize) {
        if (view.parent is RelativeLayout) {

            val params = view.layoutParams

            when (configParam.sizeX) {
                "wrap_content" -> {
                    params.width = RelativeLayout.LayoutParams.WRAP_CONTENT
                }
                "match_parent" -> {
                    params.width = RelativeLayout.LayoutParams.MATCH_PARENT
                }
                else -> {
                    if (configParam.sizeX != "") {
                        params.width = Utils.convertDpToPixel(configParam.sizeX.toDouble()).toInt()
                    }
                }
            }

            when (configParam.sizeY) {
                "wrap_content" -> {
                    params.height = RelativeLayout.LayoutParams.WRAP_CONTENT
                }
                "match_parent" -> {
                    params.height = RelativeLayout.LayoutParams.MATCH_PARENT
                }
                else -> {
                    if (configParam.sizeY != "") {
                        params.height = Utils.convertDpToPixel(configParam.sizeY.toDouble()).toInt()
                    }
                }
            }

            view.layoutParams = params
        } else if (view.parent is ConstraintLayout) {
            var layoutParams = view.layoutParams

            when (configParam.sizeX) {
                "wrap_content" -> {
                    layoutParams.width = RelativeLayout.LayoutParams.WRAP_CONTENT
                }
                "match_parent" -> {
                    layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
                }
                else -> {
                    if (configParam.sizeX != "") {
                        layoutParams.width =
                            Utils.convertDpToPixel(configParam.sizeX.toDouble()).toInt()
                    }
                }
            }

            when (configParam.sizeY) {
                "wrap_content" -> {
                    layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT
                }
                "match_parent" -> {
                    layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT
                }
                else -> {
                    if (configParam.sizeY != "") {
                        layoutParams.height =
                            Utils.convertDpToPixel(configParam.sizeY.toDouble()).toInt()
                    }
                }
            }
            view.layoutParams = layoutParams
        }
    }

    private fun resolveAlignment(view: View, configParam: ConfigParamAlignment) {

        if (view.parent is ConstraintLayout) {
            val constraintLayout = view.parent as ConstraintLayout
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)

            if (configParam.alignmentX == ConfigParamAlignment.AlignmentX.CENTER) {
                constraintSet.connect(
                    view.id, // the ID of the widget to be constrained
                    ConstraintSet.START, // the side of the widget to constrain
                    ConstraintSet.PARENT_ID, // the id of the widget to constrain to
                    ConstraintSet.START // the side of widget to constrain to
                )

                // set check box constraint end to end of parent
                constraintSet.connect(
                    view.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END
                )
            }

            if (configParam.alignmentX == ConfigParamAlignment.AlignmentX.LEFT) {
                // set check box constraint end to end of parent
                constraintSet.connect(
                    view.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START
                )
            }

            if (configParam.alignmentX == ConfigParamAlignment.AlignmentX.RIGHT) {
                // set check box constraint end to end of parent
                constraintSet.connect(
                    view.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END
                )
            }

            if (configParam.alignmentY == ConfigParamAlignment.AlignmentY.CENTER) {
                //   params.addRule(RelativeLayout.CENTER_HORIZONTAL)
                constraintSet.connect(
                    view.id, // the ID of the widget to be constrained
                    ConstraintSet.TOP, // the side of the widget to constrain
                    ConstraintSet.PARENT_ID, // the id of the widget to constrain to
                    ConstraintSet.TOP // the side of widget to constrain to
                )

                // set check box constraint end to end of parent
                constraintSet.connect(
                    view.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM
                )
            }

            if (configParam.alignmentY == ConfigParamAlignment.AlignmentY.TOP) {
                // set check box constraint end to end of parent
                constraintSet.connect(
                    view.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP
                )
            }

            if (configParam.alignmentY == ConfigParamAlignment.AlignmentY.BOTTOM) {
                // set check box constraint end to end of parent
                constraintSet.connect(
                    view.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM
                )
            }

            constraintSet.applyTo(constraintLayout);
        } else if (view.parent is RelativeLayout) {

            var params = view.layoutParams

            if (configParam.alignmentX == ConfigParamAlignment.AlignmentX.CENTER) {
                if (params is RelativeLayout.LayoutParams) {
                    params.addRule(RelativeLayout.CENTER_HORIZONTAL)
                }
            }

            if (configParam.alignmentX == ConfigParamAlignment.AlignmentX.LEFT) {
                if (params is RelativeLayout.LayoutParams) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_START)
                }
            }

            if (configParam.alignmentX == ConfigParamAlignment.AlignmentX.RIGHT) {
                if (params is RelativeLayout.LayoutParams) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_END)
                }
            }

            if (configParam.alignmentY == ConfigParamAlignment.AlignmentY.CENTER) {
                if (params is RelativeLayout.LayoutParams) {
                    params.addRule(RelativeLayout.CENTER_VERTICAL)
                }
            }

            if (configParam.alignmentY == ConfigParamAlignment.AlignmentY.TOP) {
                if (params is RelativeLayout.LayoutParams) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                }
            }

            if (configParam.alignmentY == ConfigParamAlignment.AlignmentY.BOTTOM) {
                if (params is RelativeLayout.LayoutParams) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                }
            }

            view.layoutParams = params
        }
    }

    private fun resolveMargins(view: View, configParam: ConfigParamMargins) {
        if (view.parent is RelativeLayout) {
            val layoutParams = view.layoutParams as RelativeLayout.LayoutParams

            if (configParam.marginLeft != "") {
                layoutParams.marginStart =
                    Utils.convertDpToPixel(configParam.marginLeft.toDouble()).toInt()
            }

            if (configParam.marginTop != "") {
                layoutParams.topMargin =
                    Utils.convertDpToPixel(configParam.marginTop.toDouble()).toInt()
            }

            if (configParam.marginRight != "") {
                layoutParams.marginEnd =
                    Utils.convertDpToPixel(configParam.marginRight.toDouble()).toInt()
            }

            if (configParam.marginBottom != "") {
                layoutParams.rightMargin =
                    Utils.convertDpToPixel(configParam.marginBottom.toDouble()).toInt()
            }
            view.layoutParams = layoutParams
        } else if (view.parent is ConstraintLayout) {
            val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams

            if (configParam.marginLeft != "") {
                layoutParams.marginStart =
                    Utils.convertDpToPixel(configParam.marginLeft.toDouble()).toInt()
            }

            if (configParam.marginTop != "") {
                layoutParams.topMargin =
                    Utils.convertDpToPixel(configParam.marginTop.toDouble()).toInt()
            }

            if (configParam.marginRight != "") {
                layoutParams.marginEnd =
                    Utils.convertDpToPixel(configParam.marginRight.toDouble()).toInt()
            }

            if (configParam.marginBottom != "") {
                layoutParams.bottomMargin =
                    Utils.convertDpToPixel(configParam.marginBottom.toDouble()).toInt()
            }
            view.layoutParams = layoutParams
        }
    }

    private fun resolveTextSize(view: View, configParam: ConfigParamTextSize) {
        if (view is TextView) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, configParam.fontSize.toFloat())
        } else if (view is Button) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, configParam.fontSize.toFloat())
        }
    }

    private fun resolveColor(view: View, configParam: ConfigParamColor) {

        if (view is TextView) {
            if (configParam.type == ConfigParamColor.ColorType.TEXT_COLOR) {
                view.setTextColor(Color.parseColor(configParam.color))
            } else if (configParam.type == ConfigParamColor.ColorType.BACKGROUND_COLOR) {
                if (configParam.color.isNotEmpty()) {
                    view.setBackgroundColor(Color.parseColor(configParam.color))
                }
            }
        } else if (view is Button) {
            if (configParam.color.isNotEmpty()) {
                view.setBackgroundColor(Color.parseColor(configParam.color))
            }
        }

        if (view is LottieAnimationView) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "resolveColor: LOTTIE $view  ${configParam.color}")

            view.addValueCallback(
                KeyPath("**"), LottieProperty.COLOR_FILTER
            ) {
                PorterDuffColorFilter(
                    Color.parseColor(configParam.color),
                    PorterDuff.Mode.SRC_ATOP
                );
            }
        }

        if (view is ProgressBar) {
            if (configParam.type == ConfigParamColor.ColorType.PROGRESS_BAR_COLOR) {
                view.progressTintList = ColorStateList.valueOf(Color.parseColor(configParam.color))
            }
        }
    }
}