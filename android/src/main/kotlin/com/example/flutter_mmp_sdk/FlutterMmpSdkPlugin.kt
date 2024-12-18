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
  private var referrerClickId: String? = null
  private var referrerTid: String? = null

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
        if (!eventType.isNullOrEmpty()) {
          trackEvent(eventType)
          result.success("Event tracked successfully")
        } else {
          Log.e("FlutterMmpSdk", "Missing eventType.")
          result.error("INVALID_ARGUMENTS", "Missing eventType", null)
        }
      }
      else -> result.notImplemented()
    }
  }


  private fun initializeSdk() {
    val referrerClient = InstallReferrerClient.newBuilder(applicationContext).build()

    try {
      Log.d("FlutterMmpSdk", "Starting Install Referrer Client connection...")
      referrerClient.startConnection(object : InstallReferrerStateListener {
        override fun onInstallReferrerSetupFinished(responseCode: Int) {
          Log.d("FlutterMmpSdk", "Install Referrer Setup Finished with response code: $responseCode")

          when (responseCode) {
            InstallReferrerClient.InstallReferrerResponse.OK -> {
              val response: ReferrerDetails = referrerClient.installReferrer
              val referrerUrl = response.installReferrer
              Log.d("FlutterMmpSdk", "Install Referrer URL: $referrerUrl")

              val uri = android.net.Uri.parse("https://?$referrerUrl")
              val referrerString = uri.getQueryParameter("referrer")
              Log.d("FlutterMmpSdk", "Parsed Referrer String: $referrerString")

              if (!referrerString.isNullOrEmpty()) {
                val referrerParams = referrerString.split(",")
                referrerClickId = referrerParams.getOrNull(0)
                referrerTid = referrerParams.getOrNull(1)
              }

              Log.d("FlutterMmpSdk", "Parsed Click ID: $referrerClickId, TID: $referrerTid")

              if (!referrerClickId.isNullOrEmpty() && !referrerTid.isNullOrEmpty()) {
                sendInstallationData(referrerClickId!!, referrerTid!!)
              } else {
                Log.e("FlutterMmpSdk", "Missing Click ID or TID.")
              }
            }
            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
              Log.e("FlutterMmpSdk", "Install Referrer API not supported on this device.")
            }
            InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
              Log.e("FlutterMmpSdk", "Install Referrer Service unavailable.")
            }
            else -> {
              Log.e("FlutterMmpSdk", "Unexpected response code: $responseCode")
            }
          }
        }

        override fun onInstallReferrerServiceDisconnected() {
          Log.w("FlutterMmpSdk", "Install Referrer Service disconnected.")
        }
      })
    } catch (e: Exception) {
      Log.e("FlutterMmpSdk", "Error during Install Referrer initialization: ${e.message}")
    }
  }

  private fun sendInstallationData(clickId: String, tid: String) {
    Log.d("FlutterMmpSdk", "Preparing to send installation data...")

    val url = "$baseUrl/store_install.php"
    val client = OkHttpClient()

    val formBody = FormBody.Builder()
      .add("click_id", clickId)
      .add("tid", tid)
      .add("device_model", Build.MODEL)
      .add("android_version", Build.VERSION.RELEASE)
      .add("advertising_id", getAdvertisingId())
      .build()

    val request = Request.Builder()
      .url(url)
      .addHeader("Content-Type", "application/x-www-form-urlencoded")
      .post(formBody)
      .build()

    Log.d("FlutterMmpSdk", "Sending POST request to: $url with data: click_id=$clickId, tid=$tid")

    client.newCall(request).enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        Log.e("FlutterMmpSdk", "HTTP Request Failed: ${e.message}")
      }

      override fun onResponse(call: Call, response: Response) {
        response.use {
          val responseBody = it.body?.string()
          Log.d("FlutterMmpSdk", "HTTP Response Code: ${it.code}")
          Log.d("FlutterMmpSdk", "HTTP Response Body: $responseBody")

          if (!it.isSuccessful) {
            Log.e("FlutterMmpSdk", "HTTP Request Failed with Code: ${it.code}")
          } else {
            Log.d("FlutterMmpSdk", "Installation Data Successfully Sent!")
          }
        }
      }
    })
  }

  private fun trackEvent(eventType: String) {
    if (referrerClickId.isNullOrEmpty() || referrerTid.isNullOrEmpty()) {
      Log.e("FlutterMmpSdk", "Cannot track event. Missing Click ID or TID.")
      return
    }

    Log.d("FlutterMmpSdk", "Preparing to track event...")

    val url = "$baseUrl/store_event.php"
    val client = OkHttpClient()

    // Build the request body
    val formBody = FormBody.Builder()
      .add("event_type", eventType)
      .add("click_id", referrerClickId!!)
      .add("tid", referrerTid!!)
      .build()

    // Create the HTTP request
    val request = Request.Builder()
      .url(url)
      .addHeader("Content-Type", "application/x-www-form-urlencoded")
      .post(formBody)
      .build()

    Log.d("FlutterMmpSdk", "Sending POST request to: $url with data: event_type=$eventType, click_id=$referrerClickId, tid=$referrerTid")

    // Execute the HTTP request
    client.newCall(request).enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        Log.e("FlutterMmpSdk", "HTTP Request Failed: ${e.message}")
      }

      override fun onResponse(call: Call, response: Response) {
        response.use {
          val responseBody = it.body?.string()
          Log.d("FlutterMmpSdk", "HTTP Response Code: ${it.code}")
          Log.d("FlutterMmpSdk", "HTTP Response Body: $responseBody")

          if (!it.isSuccessful) {
            Log.e("FlutterMmpSdk", "HTTP Request Failed with Code: ${it.code}")
          } else {
            Log.d("FlutterMmpSdk", "Event Data Successfully Sent!")
          }
        }
      }
    })
  }


  private fun getAdvertisingId(): String {
    return "unknown" // Placeholder for Advertising ID logic
  }
}
object HttpHelper {
  private val client = OkHttpClient()

  fun post(url: String, params: Map<String, String>) {
    val formBody = FormBody.Builder().apply {
      params.forEach { (key, value) ->
        Log.d("HttpHelper", "POST Parameter: $key = $value")
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
            Log.d("HttpHelper", "Event Data Successfully Sent!")
          }
        }
      }
    })
  }
}
