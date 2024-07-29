package com.iwedia.cltv.platform.model.fast_backend_utils

/**
 * Error class to hold error data.
 *
 * @author Abhilash M R
 */
class ApiError(
    val statusCode: Int,
    val message: String,
    val details: String?
)