package com.iwedia.cltv.platform.model.fast_backend_utils

import android.os.Build

object SystemPropertyHelper {

    private val brand: String = getBrand()
    private val productName: String = getProductName()
    private val deviceName: String = getDeviceName()

    // Method to get system property using reflection
    private fun getSystemProperty(key: String): String {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            get.invoke(systemProperties, key) as String
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // Method to get brand
    private fun getBrand(): String {
        return Build.BRAND ?: getSystemProperty("ro.product.brand")
    }

    // Method to get product name
    private fun getProductName(): String {
        return Build.PRODUCT ?: getSystemProperty("ro.product.name")
    }

    // Method to get device name
    private fun getDeviceName(): String {
        return Build.DEVICE ?: getSystemProperty("ro.product.device")
    }

    // Method to return properties as one string separated by hyphens
    fun getPropertiesAsString(): String {
        return "$brand-$productName-$deviceName"
    }
}