package com.iwedia.cltv.platform.model.fast_backend_utils

/**
 * Custom Exception class to handle API Errors.
 *
 * @author Abhilash M R
 */
class ApiException (
    val error: ApiError
) : Exception (error.message)