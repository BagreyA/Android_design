package com.example.login

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


class NetworkManager<Context>(private val context: Context, private val serverUrl: String, private val webSocketEndpoint: String) {
    internal var webSocket: WebSocket? = null
    private var isWebSocketConnected = false
    companion object {
        private const val TAG = "NetworkManager"
    }

    private val httpClient = OkHttpClient.Builder()
        .pingInterval(5, TimeUnit.SECONDS)
        .build()

    fun registerUser(email: String, password: String, onComplete: (RegisterResponse?) -> Unit) {
        val jsonBody = Json.encodeToString(mapOf("email" to email, "password" to password))
        val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$serverUrl/api/user/register")
            .post(requestBody)
            .addHeader("Accept", "application/json")
            .build()

        Log.d(TAG, "Sending register request: ${request.body?.toString()}")

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to register user", e)
                onComplete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        try {
                            val responseBody = response.body?.string() ?: ""
                            val json = Json { ignoreUnknownKeys = true }
                            val registerResponse = json.decodeFromString<RegisterResponse>(responseBody)
                            Log.d(TAG, "Register response: $registerResponse")
                            (context as? Activity)?.runOnUiThread {
                                onComplete(registerResponse)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse register response: ${e.message}", e)
                            (context as? Activity)?.runOnUiThread {
                                onComplete(null)
                            }
                        }
                    } else {
                        val errorBody = response.body?.string()
                        Log.e(TAG, "Failed to register user: ${response.code} - $errorBody")
                        (context as? Activity)?.runOnUiThread {
                            onComplete(null)
                        }
                    }
                }
            }
        })
    }
    fun verifyToken(token: String, onComplete: (Boolean) -> Unit) {
        val request = Request.Builder()
            .url("$serverUrl/api/jwt/verify")
            .header("Authorization", "Bearer $token")
            .post("".toRequestBody(null))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to verify token", e)
                onComplete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to verify token: ${response.code}")
                        onComplete(false)
                        return@use
                    }
                    val responseBody = response.body?.string()
                    val jsonResponse = Json.decodeFromString<Map<String, String>>(responseBody ?: "")
                    val result = jsonResponse["result"]
                    onComplete(result == "valid")
                }
            }
        })
    }

    fun authenticateUser(email: String, password: String, token: String, onComplete: (AuthResponse?) -> Unit) {
        verifyToken(token) { isValid ->
            if (isValid) {
                val jsonBody = Json.encodeToString(mapOf("email" to email, "password" to password))
                val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$serverUrl/api/user/auth")
                    .header("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Failed to authenticate user (1)", e)
                        onComplete(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!response.isSuccessful) {
                                if (response.code == 401) {
                                    Log.e(TAG, "Failed to authenticate user: Unauthorized")
                                } else {
                                    Log.e(TAG, "Failed to authenticate user (2): ${response.code}")
                                }
                                onComplete(null)
                                return@use
                            }
                            val responseBody = response.body?.string()
                            try {
                                val json = Json { ignoreUnknownKeys = true }
                                val authResponse = json.decodeFromString<AuthResponse>(responseBody ?: "")
                                Log.d(TAG, "Auth response: $authResponse")
                                (context as? Activity)?.runOnUiThread {
                                    onComplete(authResponse)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to parse auth response: ${e.message}")
                                (context as? Activity)?.runOnUiThread {
                                    onComplete(null)
                                }
                            }
                        }
                    }
                })
            } else {
                Log.e(TAG, "Token is invalid, cannot authenticate user")
                onComplete(null)
            }
        }
    }

    fun authenticateForTraffic(email: String, password: String): Deferred<AuthResponse?> = CoroutineScope(
        Dispatchers.IO).async {
        var authResponse: AuthResponse? = null
        val jsonBody = Json.encodeToString(mapOf("email" to email, "password" to password))
        val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$serverUrl/api/user/auth")
            .post(requestBody)
            .build()

        try {
            val response = httpClient.newCall(request).execute()

            response.use {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    try {
                        authResponse = Json { ignoreUnknownKeys = true }.decodeFromString<AuthResponse>(responseBody ?: "")
                        Log.d(TAG, "Auth response: $authResponse")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse auth response: ${e.message}")
                    }
                } else {
                    Log.e(TAG, "Failed to authenticate for traffic data: ${response.code}")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to authenticate for traffic data", e)
        }

        authResponse
    }

    fun sendTrafficDataToServer(jwt: String, trafficData: List<AppTrafficData>) {
        val state = MainActivity.state

        if (state.Uuid.isNullOrEmpty() || jwt.isNullOrEmpty()) {
            Log.e(TAG, "Cannot send traffic data: UUID or JWT is missing.")
            return
        }

        val top10TrafficData = trafficData.sortedByDescending { it.totalBytes }.take(10)

        val modifiedJson = buildJsonObject {
            put("jwt", jwt)
            put("uuid", MainActivity.state.Uuid)
            put("trafficData", Json.encodeToJsonElement(top10TrafficData.map { appData ->
                buildJsonObject {
                    put("appName", appData.appName)
                    put("totalBytes", appData.totalBytes / 1024)
                    put("mobileBytes", appData.mobileBytes / 1024)
                    put("wifiBytes", appData.wifiBytes / 1024)
                    put("rxBytes", appData.rxBytes / 1024)
                    put("txBytes", appData.txBytes / 1024)
                }
            }))
        }
        val jsonBody = modifiedJson.toString()

        val endpoint = "/api/user/postapptrafic"

        if (webSocket == null) {
            Log.e(TAG, "WebSocket is not initialized, attempting to connect...")

            val request = Request.Builder()
                .url("$serverUrl$endpoint")
                .header("Authorization", "Bearer $jwt")
                .build()

            this.webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected successfully for Traffic Data")
                    webSocket.send(jsonBody)
                    Log.d(TAG, "Sent Traffic Data: $jsonBody")
                    (context as? MainActivity)?.showSendingIndicator()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "Received message from server (Traffic Data): $text")
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket connection closing (Traffic Data): $code $reason")
                    this@NetworkManager.webSocket = null
                    this@NetworkManager.isWebSocketConnected = false
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket connection closed (Traffic Data): $code $reason")
                    this@NetworkManager.webSocket = null
                    this@NetworkManager.isWebSocketConnected = false
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Failed to connect WebSocket for Traffic Data", t)
                }
            })
            this.isWebSocketConnected = this.webSocket != null
        } else if (this.isWebSocketConnected) {
            Log.d(TAG, "Sending Traffic Data through existing WebSocket connection")
            this.webSocket?.send(jsonBody)
            Log.d(TAG, "Sent Traffic Data: $jsonBody")
            (context as? MainActivity)?.showSendingIndicator()
        } else {
            Log.e(TAG, "WebSocket is not connected, cannot send Traffic Data")
        }
    }

    fun sendMessageToData2ToServer(messageToData2: MessageToData2, onComplete: ((Boolean) -> Unit)? = null) {
        val endpoint = "/api/sockets/thermalmap"

        val modifiedJson = buildJsonObject {
            put("jwt", messageToData2.jwt)
            put("UUID", messageToData2.UUID)
            put("time", messageToData2.time)
            put("latitude", messageToData2.latitude)
            put("longitude", messageToData2.longitude)
            put("altitude", messageToData2.altitude)
            put("manufacturer", messageToData2.manufacturer)
            put("model", messageToData2.model)
            put("androidVersion", messageToData2.androidVersion)
            put("hardware", messageToData2.hardware)
            put("product", messageToData2.product)
            put("board", messageToData2.board)
            put("operator", messageToData2.operator)
            put("cdma", Json.encodeToJsonElement(messageToData2.cdma.cellInfoList))
            put("wcdma", Json.encodeToJsonElement(messageToData2.wcdma.cellInfoList))
            put("gsm", Json.encodeToJsonElement(messageToData2.gsm.cellInfoList))
            put("lte", Json.encodeToJsonElement(messageToData2.lte.cellInfoList))
            put("nr", Json.encodeToJsonElement(messageToData2.nr.cellInfoList))
        }

        val jsonBody = modifiedJson.toString()

        if (webSocket == null) {
            Log.e(TAG, "WebSocket is not initialized, attempting to connect...")

            val request = Request.Builder()
                .url("$serverUrl$endpoint")
                .header("Authorization", "Bearer ${messageToData2.jwt}")
                .build()

            this.webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected successfully for CellInfo")
                    this@NetworkManager.webSocket?.send(jsonBody)
                    Log.d(TAG, "Sent MessageToData2: $jsonBody")
                    onComplete?.invoke(true)
                    (context as? MainActivity)?.showSendingIndicator()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "Received message from server CellInfo: $text")
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket connection closing CellInfo: $code $reason")
                    this@NetworkManager.webSocket = null
                    this@NetworkManager.isWebSocketConnected = false
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket connection closed (CellInfo): $code $reason")
                    this@NetworkManager.webSocket = null
                    this@NetworkManager.isWebSocketConnected = false
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Failed to connect WebSocket for CellInfo", t)
                    onComplete?.invoke(false)
                }
            })
            this.isWebSocketConnected = this.webSocket != null
        } else if (this.isWebSocketConnected) {
            Log.d(TAG, "Sending CellInfo through existing WebSocket connection")
            this.webSocket?.send(jsonBody)
            Log.d(TAG, "Sent CellInfo: $jsonBody")
            onComplete?.invoke(true)
            (context as? MainActivity)?.showSendingIndicator()
        } else {
            Log.e(TAG, "WebSocket is not connected, cannot send CellInfo")
            onComplete?.invoke(false)
        }
    }

    fun sendMessageToServerFromFile(filePath: String, onComplete: ((Boolean) -> Unit)? = null) {
        val endpoint = "/api/sockets/thermalmap"

        val jsonBody = try {
            File(filePath).readText()
        } catch (e: IOException) {
            Log.e(TAG, "Error reading JSON from file: $filePath", e)
            onComplete?.invoke(false)
            return
        }

        var isCompleted = false
        val safeOnComplete = { result: Boolean ->
            if (!isCompleted) {
                isCompleted = true
                onComplete?.invoke(result)
            }
        }

        if (webSocket == null || !isWebSocketConnected) {
            Log.e(TAG, "WebSocket is not initialized or not connected, attempting to connect...")

            val request = Request.Builder()
                .url("$serverUrl$endpoint")
                .header("Authorization", "Bearer ${MainActivity.state.JwtToken}")
                .build()

            this.webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected successfully for CellInfo (from file)")
                    sendJsonBody(jsonBody, safeOnComplete)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "Received message from server CellInfo: $text")
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket connection closing CellInfo: $code $reason")
                    this@NetworkManager.webSocket = null
                    this@NetworkManager.isWebSocketConnected = false
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket connection closed (CellInfo): $code $reason")
                    this@NetworkManager.webSocket = null
                    this@NetworkManager.isWebSocketConnected = false
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Failed to connect WebSocket for CellInfo", t)
                    safeOnComplete(false)
                }
            })
            this.isWebSocketConnected = this.webSocket != null
        } else {
            Log.d(TAG, "Sending CellInfo through existing WebSocket connection")
            sendJsonBody(jsonBody, safeOnComplete)
        }
    }

    private fun sendJsonBody(jsonBody: String, onComplete: (Boolean) -> Unit) {
        try {
            this.webSocket?.send(jsonBody)
            Log.d(TAG, "Sent CellInfo (from networkmanager): $jsonBody")
            onComplete(true)
            (context as? MainActivity)?.showSendingIndicator()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send WebSocket message", e)
            onComplete(false)
        }
    }

    /* Способ отправки JSON на сервер с помощью multipart/form-data   */
//    fun sendMessageToServerFromFile(filePath: String, onComplete: ((Boolean) -> Unit)? = null) {
//        val endpoint = "/api/sockets/thermalmap"
//        val file = File(filePath)
//
//        // Проверка, существует ли файл
//        if (!file.exists()) {
//            Log.e(TAG, "File not found: $filePath")
//            onComplete?.invoke(false)
//            return
//        }
//
//        // Использование правильного MIME-типа для передачи файла
//        val requestBody = MultipartBody.Builder()
//            .setType(MultipartBody.FORM)
//            .addFormDataPart("file", file.name, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))  // Более общий тип
//            .build()
//
//        val request = Request.Builder()
//            .url("$serverUrl$endpoint")
//            .post(requestBody)
//            .build()
//
//        // Отправка запроса
//        httpClient.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                // Логируем ошибку при отправке
//                Log.e(TAG, "Failed to send file to server", e)
//                // Убедитесь, что onComplete вызывается в случае ошибки
//                onComplete?.invoke(false)
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                // Проверка кода ответа
//                if (response.isSuccessful) {
//                    Log.d(TAG, "File sent to server successfully")
//                    onComplete?.invoke(true)
//                    (context as? MainActivity)?.showSendingIndicator()
//                } else {
//                    // Логируем ошибку сервера
//                    Log.e(TAG, "Failed to send file to server: ${response.code} ${response.message}")
//                    onComplete?.invoke(false)
//                }
//                // Закрытие ответа для освобождения ресурсов
//                response.close()
//            }
//        })
//    }

    fun connectWebSocket(
        jwt: String,
        onOpen: (WebSocket, Response) -> Unit = { _, _ -> },
        onMessage: (WebSocket, String) -> Unit = { _, _ -> },
        onClosed: (WebSocket, Int, String) -> Unit = { _, _, _ -> },
        onFailure: (WebSocket, Throwable, Response?) -> Unit = { _, _, _ -> }
    ): WebSocket? {
        val request = Request.Builder()
            .url("$serverUrl$webSocketEndpoint")
            .header("Authorization", "Bearer $jwt")
            .build()

        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onOpen(webSocket, response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessage(webSocket, text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onClosed(webSocket, code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onFailure(webSocket, t, response)
            }
        }

        return httpClient.newWebSocket(request, webSocketListener)
    }

}
