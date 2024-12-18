package com.example.flutter_mmp_sdk

import android.content.Context
import android.os.Build
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import okhttp3.*
import java.io.IOException

class FlutterMmpSdkPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var applicationContext: Context
    private lateinit var channel: MethodChannel
    private var baseUrl: String = "https://magnetcents.co.in/mmpsdk"

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "flutter_mmp_sdk")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "initialize" -> {
                initializeSdk()
                result.success(null)
            }
            "trackEvent" -> {
                val eventType = call.argument<String>("eventType")
                val clickId = call.argument<String>("clickId")
                val tid = call.argument<String>("tid")
                if (!eventType.isNullOrEmpty() && !clickId.isNullOrEmpty() && !tid.isNullOrEmpty()) {
                    trackEvent(eventType, clickId, tid)
                    result.success("Event tracked successfully")
                } else {
                    result.error("INVALID_ARGUMENTS", "Missing eventType, clickId, or tid", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    private fun initializeSdk() {
        val referrerClient = InstallReferrerClient.newBuilder(applicationContext).build()

        try {
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            val response: ReferrerDetails = referrerClient.installReferrer
                            val referrerUrl = response.installReferrer
                            Log.d("FlutterMmpSdk", "Install Referrer URL: $referrerUrl")

                            // Correctly parse the referrer key
                            val uri = android.net.Uri.parse("https://?$referrerUrl")
                            val referrerString = uri.getQueryParameter("referrer")
                            Log.d("FlutterMmpSdk", "Referrer String: $referrerString")

                            // Extract click_id and tid from the referrer value
                            var clickId: String? = null
                            var tid: String? = null

                            if (!referrerString.isNullOrEmpty()) {
                                val referrerParams = referrerString.split(",")
                                clickId = referrerParams.getOrNull(0)
                                tid = referrerParams.getOrNull(1)
                            }

                            Log.d("FlutterMmpSdk", "Parsed Click ID: $clickId, TID: $tid")

                            // Log before calling sendInstallationData
                            if (!clickId.isNullOrEmpty() && !tid.isNullOrEmpty()) {
                                Log.d("FlutterMmpSdk", "Calling sendInstallationData with Click ID: $clickId and TID: $tid")
                                sendInstallationData(clickId, tid)
                                Log.d("FlutterMmpSdk", "sendInstallationData called successfully.")
                            } else {
                                Log.e("FlutterMmpSdk", "Missing clickId or tid in referrer")
                            }
                        }
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                            Log.e("FlutterMmpSdk", "Install Referrer API not supported on this device")
                        }
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                            Log.e("FlutterMmpSdk", "Install Referrer Service unavailable")
                        }
                        else -> {
                            Log.e("FlutterMmpSdk", "Unexpected response code: $responseCode")
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    Log.w("FlutterMmpSdk", "Install Referrer Service disconnected")
                }
            })
        } catch (e: Exception) {
            Log.e("FlutterMmpSdk", "Error initializing Install Referrer: ${e.message}")
        }
    }


    private fun sendInstallationData(clickId: String, tid: String) {
        val params = mapOf(
            "click_id" to clickId,
            "tid" to tid,
            "device_model" to Build.MODEL,
            "android_version" to Build.VERSION.RELEASE,
            "advertising_id" to getAdvertisingId()
        )

        HttpHelper.post("$baseUrl/store_install.php", params)
    }

    private fun trackEvent(eventType: String, clickId: String, tid: String) {
        val params = mapOf(
            "event_type" to eventType,
            "click_id" to clickId,
            "tid" to tid
        )
        HttpHelper.post("$baseUrl/store_event.php", params)
    }

    private fun getAdvertisingId(): String {
        // Placeholder for Google Play Services Advertising ID
        return "unknown"
    }
}
object HttpHelper {
    private val client = OkHttpClient()

    fun post(url: String, params: Map<String, String>) {
        val formBody = FormBody.Builder().apply {
            params.forEach { (key, value) ->
                Log.d("HttpHelper", "POST Parameter: $key = $value") // Log each parameter
                add(key, value)
            }
        }.build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .post(formBody)
            .build()

        Log.d("HttpHelper", "Sending POST request to: $url with parameters $params")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HttpHelper", "HTTP Request Failed: ${e.message}")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string()
                    Log.d("HttpHelper", "HTTP Response Code: ${it.code}")
                    Log.d("HttpHelper", "HTTP Response Body: $responseBody")

                    if (!it.isSuccessful) {
                        Log.e("HttpHelper", "HTTP Request Failed with Code: ${it.code}")
                    } else {
                        Log.d("HttpHelper", "HTTP Request Successful!")
                    }
                }
            }
        })
    }
}
