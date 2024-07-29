package com.iwedia.cltv.config

import com.google.gson.Gson
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.entities.*
import com.iwedia.cltv.config.entities.json.ConfigParamFont
import com.iwedia.cltv.config.entities.json.JsonSceneConfigParams
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

object ConfigJsonParser {

    /**
     * Load json config for scene
     */
    fun loadJsonConfig(sceneId: Int, configFileId: Int): SceneConfig {

        val raw: InputStream =
            ReferenceApplication.applicationContext().resources.openRawResource(configFileId)
        val rd: Reader = BufferedReader(InputStreamReader(raw))
        val gson = Gson()
        val jsonFile: JsonSceneConfigParams = gson.fromJson(rd, JsonSceneConfigParams::class.java)

        var configList = mutableListOf<ConfigParam>()
        var sceneName = ReferenceWorldHandler.getSceneName(sceneId)
        var sceneTitle = jsonFile.title!!

        jsonFile.items!!.forEach { item ->
            var id = item.id!!.toInt()
            var name = item.name!!

            ////////////////////////////////////////
            /// VISIBILITY
            ////////////////////////////////////////
            var visibility = item.visibility
            var visibilityParam = ConfigParamVisibility(
                id,
                name,
                ConfigParamVisibility.getVisibility(visibility!!)
            )
            configList.add(visibilityParam)


            ////////////////////////////////////////
            // SOURCE
            ////////////////////////////////////////

            var source = item.source

            if (source!!.contains("jpg") || source.contains("png") || source.contains("gif")) {
                var sourceConfigParam = ConfigParamImage(id, name, source)
                configList.add(sourceConfigParam)
            } else {
                if(source == "company_name") {
                    val companyName = ConfigCompanyDetailsManager.getCompanyDetailsInfo(source)

                    var sourceConfigParam = ConfigParamText(id, name, companyName)
                    configList.add(sourceConfigParam)
                } else if(source == "welcome_message") {
                    val welcomeMessage = ConfigCompanyDetailsManager.getCompanyDetailsInfo(source)

                    var sourceConfigParam = ConfigParamText(id, name, welcomeMessage)
                    configList.add(sourceConfigParam)
                } else if(source == "company_logo") {
                    val companyLogoPath = ConfigCompanyDetailsManager.getCompanyDetailsInfo(source)
                    
                    var sourceConfigParam = ConfigParamImage(id, name, companyLogoPath)
                    configList.add(sourceConfigParam)
                } else {
                    var sourceConfigParam = ConfigParamText(id, name, source)
                    configList.add(sourceConfigParam)
                }
            }


            ////////////////////////////////////////
            // ALIGNMENT
            ////////////////////////////////////////
            var alignment = ConfigParamAlignment(
                id,
                name,
                item.alignmentX!!,
                item.alignmentY!!
            )
            configList.add(alignment)

            ////////////////////////////////////////
            // SIZE
            ////////////////////////////////////////
            var width = item.width
            var height = item.height
            var size = ConfigParamSize(id, name, width!!, height!!)
            configList.add(size)

            ////////////////////////////////////////
            // BACKGROUND (color)
            ////////////////////////////////////////
            var background = item.background!!
            var bgcolorParam =
                ConfigParamColor(
                    id,
                    name,
                    ConfigColorManager.getColor(background),
                    ConfigParamColor.ColorType.BACKGROUND_COLOR
                )
            configList.add(bgcolorParam)

            ////////////////////////////////////////
            // MARGINS
            ////////////////////////////////////////
            var left = item.leftMargin
            var right = item.rightMargin
            var top = item.topMargin
            var bottom = item.bottomMargin
            var margins = ConfigParamMargins(id, name, left!!, right!!, top!!, bottom!!)
            configList.add(margins)


            ////////////////////////////////////////
            // CUSTOM DATA
            ////////////////////////////////////////
            var data = item.data

            data!!.keys.forEach { key ->
                if (key == "textColor" || key == "tintColor") {
                    var colorName = data[key]!!
                    var color = ConfigColorManager.getColor(colorName)
                    var colorParam =
                        ConfigParamColor(id, name, color, ConfigParamColor.ColorType.TEXT_COLOR)
                    configList.add(colorParam)
                } else if (key == "textSize") {
                    var textSize = data[key]!!
                    var textSizeParam = ConfigParamTextSize(id, name, textSize)
                    configList.add(textSizeParam)
                } else if (key == "secondTintColor") {
                    var colorName = data[key]!!
                    var color = ConfigColorManager.getColor(colorName)
                    var colorParam =
                        ConfigParamColor(
                            id,
                            name,
                            color,
                            ConfigParamColor.ColorType.ANIMATION_BACKGROUND_COLOR
                        )
                    configList.add(colorParam)
                } else if (key == "progressColor") {
                    var colorName = data[key]!!
                    var color = ConfigColorManager.getColor(colorName)
                    var colorParam =
                        ConfigParamColor(
                            id,
                            name,
                            color,
                            ConfigParamColor.ColorType.PROGRESS_BAR_COLOR
                        )
                    configList.add(colorParam)
                } else if (key == "font") {
                    var fontName = data[key]!!
                    var font = ConfigFontManager.getFont(fontName)
                    var fontParam = ConfigParamFont(id, name, font)
                    configList.add(fontParam)
                }
            }

            ////////////////////////////////////////
            // RAIL DATA
            ////////////////////////////////////////
            var railData = item.railData
            railData!!.forEach { item ->
                var railConfigItem = ConfigRailParam(id, item.name!!, item.id!!)
                configList.add(railConfigItem)
            }
        }

        return SceneConfig(
            sceneId,
            sceneName,
            sceneTitle,
            configList
        )
    }
}