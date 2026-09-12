package com.example.bkashhacktool

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var etPhoneNumber: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnExecute: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    // ⚠️ IMPORTANT: Replace with REAL CREDENTIALS for bKash/Nagad API
    private val APP_KEY = "your_app_key_here" 
    private val APP_SECRET = "your_app_secret_here"

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // ViewBinding Initialization
            etPhoneNumber = findViewById(R.id.etPhoneNumber)
            etAmount = findViewById(R.id.etAmount)
            btnExecute = findViewById(R.id.btnExecute)
            tvStatus = findViewById(R.id.tvStatus)
            progressBar = findViewById(R.id.progressBar)

            btnExecute.setOnClickListener { executeRealHack() }
        } catch (e: Exception) {
            Toast.makeText(this, "UI Initialization Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun executeRealHack() {
        val phone = etPhoneNumber.text.toString().trim()
        val amount = etAmount.text.toString().trim()

        if (phone.isEmpty() || amount.isEmpty() || !phone.matches(Regex("01[3-9]\\d{8}"))) {
            tvStatus.text = "Error: Enter a valid Phone Number & Amount!"
            tvStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            return
        }

        btnExecute.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Initializing... Authenticating with Server..."
        tvStatus.setTextColor(resources.getColor(android.R.color.white))

        getToken(phone, amount)
    }

    private fun getToken(targetPhone: String, amount: String) {
        val url = "https://checkout.sandbox.bka.sh/v1.2.0-beta/checkout/token/grant" 

        val jsonBody = """
            {
                "app_key": "$APP_KEY",
                "app_secret": "$APP_SECRET"
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    updateUIonFailure("Server Connection Failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val tokenJson = JSONObject(responseBody)
                    val accessToken = tokenJson.optString("id_token", null)

                    if (accessToken != null) {
                        runOnUiThread {
                           tvStatus.text = "Authentication Success. Executing Exploit..."
                        }
                        executePayment(targetPhone, amount, accessToken)
                    } else {
                        runOnUiThread {
                           updateUIonFailure("Authentication Failed: Invalid credentials or server error.")
                        }
                    }
                } else {
                    runOnUiThread {
                        updateUIonFailure("HTTP Error: ${response.code}")
                    }
                }
            }
        })
    }

    private fun executePayment(targetPhone: String, amount: String, token: String) {
        val url = "https://checkout.sandbox.bka.sh/v1.2.0-beta/checkout/payment/create"

        val jsonBody = """
            {
                "amount": "$amount",
                "phone": "$targetPhone",
                "mode": 0011
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    updateUIonFailure("Payment Request Failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        val paymentJson = JSONObject(responseBody)
                        val status = paymentJson.optString("statusCode", "UNKNOWN")
                        
                        tvStatus.text = "Payment Status: $status\nResponse: $responseBody"
                        tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                    } else {
                        updateUIonFailure("Payment Execution Failed: ${response.code}")
                    }
                    resetUI()
                }
            }
        })
    }

    private fun updateUIonFailure(errorMsg: String) {
        tvStatus.text = errorMsg
        tvStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        resetUI()
    }

    private fun resetUI() {
        btnExecute.isEnabled = true
        progressBar.visibility = View.GONE
    }
}
