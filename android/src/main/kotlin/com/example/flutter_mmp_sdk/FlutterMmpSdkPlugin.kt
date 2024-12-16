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
import org.json.JSONObject
import java.io.IOException

class FlutterMmpSdkPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var applicationContext: Context
    private lateinit var channel: MethodChannel

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
                if (eventType != null && clickId != null && tid != null) {
                    trackEvent(eventType, clickId, tid)
                } else {
                    result.error("INVALID_ARGUMENTS", "Missing eventType, clickId, or tid", null)
                }
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    private fun initializeSdk() {
        val referrerClient = InstallReferrerClient.newBuilder(applicationContext).build()

        try {
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        val response: ReferrerDetails = referrerClient.installReferrer
                        val referrerUrl = response.installReferrer
                        Log.d("FlutterMmpSdk", "Install Referrer URL: $referrerUrl")

                        val params = referrerUrl.split("&").associate {
                            val keyValue = it.split("=")
                            keyValue[0] to keyValue.getOrNull(1).orEmpty()
                        }

                        val clickId = params["referrer"]?.split(",")?.getOrNull(0)
                        val tid = params["referrer"]?.split(",")?.getOrNull(1)

                        Log.d("FlutterMmpSdk", "Parsed Click ID: $clickId, TID: $tid")

                        if (clickId != null && tid != null) {
                            sendInstallationData(clickId, tid)
                        } else {
                            Log.e("FlutterMmpSdk", "Missing clickId or tid in referrer")
                        }
                    } else {
                        Log.e("FlutterMmpSdk", "Install Referrer Setup Failed: Response Code $responseCode")
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    Log.w("FlutterMmpSdk", "Install Referrer Service Disconnected.")
                }
            })
        } catch (e: Exception) {
            Log.e("FlutterMmpSdk", "Failed to initialize Install Referrer: ${e.message}")
        }
    }

    private fun sendInstallationData(clickId: String, tid: String) {
        val params = mapOf(
            "click_id" to clickId,
            "tid" to tid,
            "device_model" to Build.MODEL,
            "android_version" to Build.VERSION.RELEASE,
            "advertising_id" to getAdvertisingId() // Optional, can be implemented separately
        )

        HttpHelper.post("https://magnetcents.co.in/mmpsdk/store_install.php", params)
    }

    private fun trackEvent(eventType: String, clickId: String, tid: String) {
        val params = mapOf(
            "event_type" to eventType,
            "click_id" to clickId,
            "tid" to tid
        )
        HttpHelper.post("https://magnetcents.co.in/mmpsdk/store_event.php", params)
    }

    private fun getAdvertisingId(): String {
        // Implement advertising ID retrieval if necessary
        return "unknown"
    }
}

object HttpHelper {
    private val client = OkHttpClient()

    fun post(url: String, params: Map<String, String>) {
        val formBody = FormBody.Builder().apply {
            params.forEach { (key, value) -> add(key, value) }
        }.build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HttpHelper", "HTTP Request Failed: ${e.message}")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("HttpHelper", "HTTP Request Failed: Code ${it.code}")
                    } else {
                        Log.d("HttpHelper", "HTTP Response: ${it.body?.string()}")
                    }
                }
            }
        })
    }
}
