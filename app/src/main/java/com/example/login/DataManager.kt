package com.example.login

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.telephony.cdma.CdmaCellLocation
import android.telephony.gsm.GsmCellLocation
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

object DataManager {
    private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private const val TAG = "DataManager"
    private var locationCallback: LocationCallback? = null
    private var locationUpdatesCount = 0
    private const val MAX_LOCATION_UPDATES = 1

    private var fileCounter = 1
    private val maxFileCount = 10
    internal var fileName = "Signal_data_$fileCounter.txt"

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var rotationVector: Sensor? = null

    private var lastAccelerometerReading: FloatArray? = null
    private var lastMagnetometerReading: FloatArray? = null
    private var lastRotationVectorReading: FloatArray? = null

    private const val PREFS_NAME = "FileUploadPrefs"
    private const val KEY_PENDING_COUNT = "pending_count"
    private const val KEY_SUCCESS_COUNT = "success_count"
    private const val RECORDING_PREFS_NAME = "app_prefs"
    private const val KEY_IS_RECORDING = "is_recording"

    var isOrientationReady = false
    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)

    @RequiresApi(Build.VERSION_CODES.O)
    fun getLocation(activity: Context, state: MainActivity.MainActivityState) {
        val context = activity.applicationContext
        Log.d(MainActivity.TAG, "getLocation() called")

//        if (ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                activity as Activity,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                LOCATION_PERMISSION_REQUEST_CODE
//            )
//            return
//        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(MainActivity.TAG, "No permission to access coarse location")
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let it@{
                Log.d(MainActivity.TAG, "Location received: Lat=${location.latitude}, Lon=${location.longitude}, Alt=${location.altitude}")
                state.Altitude = location.altitude.toString()
                if (isOrientationReady) {
                    val adjustedLocation = adjustLocationWithOrientation(location)
                    state.Latitude = adjustedLocation.latitude.toString()
                    state.Longtitude = adjustedLocation.longitude.toString()
                } else {
                    state.Latitude = location.latitude.toString()
                    state.Longtitude = location.longitude.toString()
                }
            }
        }.addOnFailureListener { e ->
            Log.e(MainActivity.TAG, "Failed to get last known location", e)
        }

        if (locationUpdatesCount < MAX_LOCATION_UPDATES) {
            locationUpdatesCount++
            fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        if (isOrientationReady) {
                            val adjustedLocation = adjustLocationWithOrientation(location)
                            state.Latitude = adjustedLocation.latitude.toString()
                            state.Longtitude = adjustedLocation.longitude.toString()
                            state.Altitude = adjustedLocation.altitude.toString()
                            Log.d(
                                MainActivity.TAG,
                                "Location updated: Lat=${state.Latitude}, Lon=${state.Longtitude}, Alt=${state.Altitude}"
                            )
                        } else {
                            state.Latitude = location.latitude.toString()
                            state.Longtitude = location.longitude.toString()
                            state.Altitude = location.altitude.toString()
                            Log.d(
                                MainActivity.TAG,
                                "Location updated (no orientation): Lat=${state.Latitude}, Lon=${state.Longtitude}, Alt=${state.Altitude}"
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(MainActivity.TAG, "Failed to get current location", e)
                }
        } else {
            Log.w(TAG, "Maximum number of location updates reached.")
        }

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            sensorListener,
            magnetometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            sensorListener,
            rotationVector,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }


    internal val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    lastAccelerometerReading = event.values.clone()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    lastMagnetometerReading = event.values.clone()
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    lastRotationVectorReading = event.values.clone()
                }
            }

            if (lastAccelerometerReading != null && lastMagnetometerReading != null) {
                SensorManager.getRotationMatrix(
                    rotationMatrix,
                    null,
                    lastAccelerometerReading,
                    lastMagnetometerReading
                )
                isOrientationReady = true
            } else if (lastRotationVectorReading != null) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, lastRotationVectorReading)
                isOrientationReady = true
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }
    }

    internal fun adjustLocationWithOrientation(location: Location): Location {
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimuth = Math.toRadians(orientationAngles[0].toDouble()) // Угол поворота вокруг оси Z (азимут)

        val adjustedLatitude = location.latitude + Math.sin(azimuth) * 0.00001 //смещение на 1 метр
        val adjustedLongitude = location.longitude + Math.cos(azimuth) * 0.00001 //смещение на 1 метр

        val adjustedLocation = Location(location)
        adjustedLocation.latitude = adjustedLatitude
        adjustedLocation.longitude = adjustedLongitude

        return adjustedLocation
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getCellInfo(context: Context, state: MainActivity.MainActivityState) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(MainActivity.TAG, "Нет разрешений READ_PHONE_STATE или ACCESS_COARSE_LOCATION для получения cell info")

            if (context is Activity) {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(
                        android.Manifest.permission.READ_PHONE_STATE,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    1
                )
            } else {
                Log.e(MainActivity.TAG, "Контекст не является экземпляром Activity, невозможно запросить разрешения")
                Toast.makeText(context, "Нет доступных данных, проверьте настройки GPS", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (state.Latitude.isEmpty() || state.Longtitude.isEmpty()) {
            Log.d(MainActivity.TAG, "Location data is missing, cannot get cell info.")
            return
        }

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cellInfoList = telephonyManager.allCellInfo

        if (cellInfoList != null) {
            processCellInfo(cellInfoList, state)
        } else {
            Log.d(MainActivity.TAG, "cellInfoList is null, GPS может быть отключен или нет доступных данных cell info")
        }
    }

    @SuppressLint("MissingPermission")
    private fun processCellInfo(cellInfoList: List<CellInfo>, state: MainActivity.MainActivityState) {
        val cellInfoDataByType = mutableMapOf<String, MutableList<CellInfoData>>(
            "CDMA" to mutableListOf(),
            "GSM" to mutableListOf(),
            "LTE" to mutableListOf(),
            "WCDMA" to mutableListOf(),
            "NR" to mutableListOf()
        )

        for (cellInfo in cellInfoList) {
            val cellData = when (cellInfo) {
                is CellInfoLte -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    extractLteCellInfoToJson(cellInfo)
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
                is CellInfoGsm -> extractGsmCellInfoToJson(cellInfo)
                is CellInfoWcdma -> extractWcdmaCellInfoToJson(cellInfo)
                is CellInfoCdma -> extractCdmaCellInfoToJson(cellInfo)
                else -> {
                    Log.d(TAG, "Unknown cell info type: ${cellInfo.javaClass.simpleName}")
                    null
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (cellInfo is CellInfoNr) {
                    val cellData = extractNrCellInfoToJson(cellInfo)
                    if (cellData != null) {
                        cellInfoDataByType["NR"]?.add(cellData)
                    }
                }
            }

            if (cellData != null) {
                val cellType = when (cellInfo) {
                    is CellInfoLte -> "LTE"
                    is CellInfoGsm -> "GSM"
                    is CellInfoWcdma -> "WCDMA"
                    is CellInfoCdma -> "CDMA"
                    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr) {
                        "NR"
                    } else {
                        "Unknown"
                    }
                }
                cellInfoDataByType[cellType]?.add(cellData)
            }
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val currentDateAndTime = sdf.format(Date())

        val messageToData2 = MessageToData2(
            jwt = state.JwtToken,
            UUID = state.Uuid,
            time = currentDateAndTime,
            latitude = state.Latitude.toDoubleOrNull() ?: 0.0,
            longitude = state.Longtitude.toDoubleOrNull() ?: 0.0,
            altitude = state.Altitude.toDoubleOrNull() ?: 0.0,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            hardware = Build.HARDWARE,
            product = Build.PRODUCT,
            board = Build.BOARD,
            operator = state.Operator,
            cdma = CdmaData(cellInfoDataByType["CDMA"] ?: emptyList()),
            gsm = GsmData(cellInfoDataByType["GSM"] ?: emptyList()),
            wcdma = WcdmaData(cellInfoDataByType["WCDMA"] ?: emptyList()),
            lte = LteData(cellInfoDataByType["LTE"] ?: emptyList()),
            nr = NRData(cellInfoDataByType["NR"] ?: emptyList())
        )

        state.messageToData2 = messageToData2

        val hasLocationData = messageToData2.latitude != 0.0 && messageToData2.longitude != 0.0
        val hasCellInfoData = cellInfoDataByType.values.any { it.isNotEmpty() }

        if (hasLocationData && hasCellInfoData) {
            val jsonMessageToData2 = Json.encodeToString(messageToData2)
            val maxLogLength = 4000
            for (i in jsonMessageToData2.indices step maxLogLength) {
                val chunk = jsonMessageToData2.substring(i, minOf(i + maxLogLength, jsonMessageToData2.length))
                Log.d(TAG, "CellInfo JSON (part ${i / maxLogLength}): $chunk")
            }
            if (state.isSendingCellInfoData) {
                MainActivity.networkManager.sendMessageToData2ToServer(messageToData2) { success ->
                    if (success) {
                        Log.d(TAG, "CellInfo SENT TO SERVER")
                    } else {
                        Log.e(TAG, "FAILED TO SEND CellInfo")
                    }
                }
            }
        } else {
            Log.d(TAG, "No cell info data to send to server or location data is missing")
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun extractLteCellInfoToJson(info: CellInfoLte): CellInfoData? {
        return try {
            val signalStrength = info.cellSignalStrength
            val identity = info.cellIdentity
            val timestamp = System.currentTimeMillis()

            CellInfoData(
                type = "LTE",
                timestamp = timestamp,
                registered = info.isRegistered,
                mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    identity.mccString?.toIntOrNull()?.takeIf { it != CellInfo.UNAVAILABLE }?.toLong()
                } else {
                    identity.mcc.takeIf { it != CellInfo.UNAVAILABLE }?.toLong()
                },
                mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    identity.mncString?.toIntOrNull()?.takeIf { it != CellInfo.UNAVAILABLE }?.toLong()
                } else {
                    identity.mnc.takeIf { it != CellInfo.UNAVAILABLE }?.toLong()
                },
                ci = identity.ci.takeIf { it != CellInfo.UNAVAILABLE }?.toLong(),
                pci = identity.pci.takeIf { it != CellInfo.UNAVAILABLE }?.toLong(),
                tac = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    identity.tac.takeIf { it != CellInfo.UNAVAILABLE }?.toLong()
                } else {
                    null
                },
                earfcn = identity.earfcn.takeIf { it != CellInfo.UNAVAILABLE }?.toLong(),
                bandwidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.bandwidth.takeIf { it != CellInfo.UNAVAILABLE }?.toLong() else null,
                rsrp = signalStrength.rsrp.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                rssi= if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    signalStrength.rssi.takeIf {it != Integer.MAX_VALUE }?.toLong()
                } else {
                    null
                },
                rsrq = signalStrength.rsrq.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                rssnr = signalStrength.rssnr.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                cqi = signalStrength.cqi.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                timingAdvance = signalStrength.timingAdvance.takeIf { it != Integer.MAX_VALUE }
                    ?.toLong(),
                level = signalStrength.level.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                asuLevel = signalStrength.asuLevel.takeIf { it != Integer.MAX_VALUE } ?: 0
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error encoding LTE cell info to JSON: ", e)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun extractGsmCellInfoToJson(info: CellInfoGsm): CellInfoData? {
        return try {
            val signalStrength = info.cellSignalStrength
            val identity = info.cellIdentity
            val timestamp = System.currentTimeMillis()
            CellInfoData(
                type = "GSM",
                timestamp = timestamp,
                registered = info.isRegistered,
                mcc = identity.mcc.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                mnc = identity.mnc.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                lac = identity.lac.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                cid = identity.cid.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                arfcn = identity.arfcn.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                bsic = identity.bsic.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                rssi = signalStrength.dbm.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                bitErrorRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signalStrength.bitErrorRate.takeIf { it != Integer.MAX_VALUE }?.toLong() else null,
                timingAdvance = signalStrength.timingAdvance.takeIf { it != Integer.MAX_VALUE }?.toLong(),

                level = signalStrength.level.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                asuLevel = signalStrength.asuLevel.takeIf { it != Integer.MAX_VALUE } ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding GSM cell info to JSON: ", e)
            null
        }
    }

    private fun extractWcdmaCellInfoToJson(info: CellInfoWcdma): CellInfoData? {
        return try {
            val signalStrength = info.cellSignalStrength
            val identity = info.cellIdentity
            val timestamp = System.currentTimeMillis()

            CellInfoData(
                type = "WCDMA",
                timestamp = timestamp,
                registered = info.isRegistered,
                mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    identity.mccString?.toIntOrNull()?.takeIf { it != CellInfo.UNAVAILABLE }?.toLong()
                } else {
                    identity.mcc.takeIf { it != CellInfo.UNAVAILABLE }?.toLong()
                },
                mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    identity.mncString?.toIntOrNull()?.takeIf { it != CellInfo.UNAVAILABLE }?.toLong()
                } else {
                    identity.mnc.takeIf { it != CellInfo.UNAVAILABLE }?.toLong()
                },
                lac = identity.lac.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                cid = identity.cid.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                psc = identity.psc.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                uarfcn = identity.uarfcn.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                rscp = signalStrength.dbm.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                level = signalStrength.level.takeIf { it != Integer.MAX_VALUE }?.toLong(),

                asuLevel = signalStrength.asuLevel.takeIf { it != Integer.MAX_VALUE } ?: 0,
                ecNo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    signalStrength.ecNo.takeIf { it != Integer.MAX_VALUE }?.toLong()
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding WCDMA cell info to JSON: ", e)
            null
        }
    }

    private fun extractCdmaCellInfoToJson(info: CellInfoCdma): CellInfoData? {
        return try {
            val signalStrength = info.cellSignalStrength
            val identity = info.cellIdentity
            val timestamp = System.currentTimeMillis()
            CellInfoData(
                type = "CDMA",
                timestamp = timestamp,
                registered = info.isRegistered,
                sid = identity.systemId.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                nid = identity.networkId.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                bsid = identity.basestationId.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                rssi = signalStrength.cdmaDbm.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                ecIo = signalStrength.cdmaEcio.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                evdoDbm = signalStrength.evdoDbm.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                evdoEcio = signalStrength.evdoEcio.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                evdoSnr = signalStrength.evdoSnr.takeIf { it != Integer.MAX_VALUE }?.toLong(),

                level = signalStrength.cdmaLevel.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                asuLevel = signalStrength.asuLevel.takeIf { it != Integer.MAX_VALUE } ?: 0,
                evdoLevel = signalStrength.evdoLevel.takeIf { it != Integer.MAX_VALUE } ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding CDMA cell info to JSON: ", e)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun extractNrCellInfoToJson(info: CellInfoNr): CellInfoData? {
        return try {
            val signalStrength = info.cellSignalStrength as CellSignalStrengthNr
            val identity = info.cellIdentity as CellIdentityNr
            val timestamp = System.currentTimeMillis()

            CellInfoData(
                type = "5G NR",
                timestamp = timestamp,
                registered = info.isRegistered,
                mcc = identity.mccString?.toLongOrNull()?.takeIf { it != 2147483647L },
                mnc = identity.mncString?.toLongOrNull()?.takeIf { it != 2147483647L },
                nci = identity.nci.takeIf { it.toInt() != Integer.MAX_VALUE }?.toLong(),
                pci = identity.pci.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                tac = identity.tac.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                nrarfcn = identity.nrarfcn.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                csiRsrp = signalStrength.csiRsrp.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                csiRsrq = signalStrength.csiRsrq.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                csiSinr = signalStrength.csiSinr.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                ssRsrp = signalStrength.ssRsrp.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                ssRsrq = signalStrength.ssRsrq.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                ssSinr = signalStrength.ssSinr.takeIf { it != Integer.MAX_VALUE }?.toLong(),

                asuLevel = signalStrength.asuLevel.takeIf { it != Integer.MAX_VALUE } ?: 0,
                csiCqiTableIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    signalStrength.csiCqiTableIndex.takeIf { it != Integer.MAX_VALUE }?.toLong()
                } else null,
                ssRsrpDbm = signalStrength.dbm.takeIf { it != Integer.MAX_VALUE }?.toLong(),
                timingAdvanceMicros = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    signalStrength.timingAdvanceMicros.takeIf { it != Integer.MAX_VALUE }?.toLong()
                } else null,
                csiCqiReport = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    signalStrength.csiCqiReport?.map { it.toLong().takeIf { it != 2147483647L } ?: 0 }
                } else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding NR cell info to JSON: ", e)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun getSignalStrength(state: MainActivity.MainActivityState) {
        val context = state.context
        if (PermissionUtils.checkPhoneStatePermission(context) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val cellInfoList = telephonyManager.allCellInfo
            if (cellInfoList.isNullOrEmpty()) {
                setEmptyCellInfo(state)
            } else {
                for (info in cellInfoList) {
                    if (info is CellInfoLte) {
                        extractLteCellInfo(state, info, telephonyManager)
                        break
                    }
                }
            }
        } else {
            setNoPermissionCellInfo(state)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun extractLteCellInfo(state: MainActivity.MainActivityState, info: CellInfoLte, telephonyManager: TelephonyManager) {
        val context = state.context
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val cellSignalStrengthLte = info.cellSignalStrength
            state.Rsrp = "${cellSignalStrengthLte.rsrp} dBm"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                state.Rssi = "${cellSignalStrengthLte.rssi} dBm"
            }
            state.Rsrq = "${cellSignalStrengthLte.rsrq} dB"
            state.Rssnr = "${cellSignalStrengthLte.rssnr} dB"
            state.Cqi = "${cellSignalStrengthLte.cqi}"
            state.Bandwidth = "${telephonyManager.dataNetworkType}"
            state.Cellid = when (val cellLocation = telephonyManager.cellLocation) {
                is GsmCellLocation -> cellLocation.cid.toString()
                is CdmaCellLocation -> cellLocation.baseStationId.toString()
                else -> "Cell ID not available"
            }
            state.Mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.cellIdentity.mccString ?: "N/A"
            } else {
                "N/A"
            }
            state.Mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.cellIdentity.mncString ?: "N/A"
            } else {
                "N/A"
            }
            state.Lac = try {
                (telephonyManager.cellLocation as? GsmCellLocation)?.lac?.toString() ?: "N/A"
            } catch (e: Exception) {
                "N/A"
            }
            state.Tac =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    info.cellIdentity.tac.toString()
                } else {
                    "N/A"
                }
            state.Pci = info.cellIdentity.pci.toString()
            state.Earfcn = info.cellIdentity.earfcn.toString()
            state.Ci = info.cellIdentity.ci.toString()
            state.NetworkType = telephonyManager.networkType.toString()
            state.SignalStrength = cellSignalStrengthLte.dbm.toString()
            state.BitErrorRate = "N/A"
            state.TimingAdvance =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cellSignalStrengthLte.timingAdvance.toString()
                } else {
                    "N/A"
                }
            state.Band = getBandFromEarfcn(info.cellIdentity.earfcn)
            state.Operator = telephonyManager.networkOperatorName
            state.Technology = "LTE"
            logCellInfo(state)
        } else {
            setNoPermissionCellInfo(state)
        }
    }

    private fun setEmptyCellInfo(state: MainActivity.MainActivityState) {
        state.Rsrp = "CellInfo list is empty"
        state.Rssi = "CellInfo list is empty"
        state.Rsrq = "CellInfo list is empty"
        state.Rssnr = "CellInfo list is empty"
        state.Cqi = "CellInfo list is empty"
        state.Bandwidth = "CellInfo list is empty"
        state.Cellid = "Cell Info not available"
        state.Mcc = "N/A"
        state.Mnc = "N/A"
        state.Lac = "N/A"
        state.Tac = "N/A"
        state.Pci = "N/A"
        state.Earfcn = "N/A"
        state.Ci = "N/A"
        state.NetworkType = "N/A"
        state.SignalStrength = "N/A"
        state.BitErrorRate = "N/A"
        state.TimingAdvance = "N/A"
        state.Band = "N/A"
        state.Operator = "N/A"
        state.Technology = "N/A"
    }

    private fun setNoPermissionCellInfo(state: MainActivity.MainActivityState) {
        state.Rsrp = "No READ_PHONE_STATE permission"
        state.Rssi = "No READ_PHONE_STATE permission"
        state.Rsrq = "No READ_PHONE_STATE permission"
        state.Rssnr = "No READ_PHONE_STATE permission"
        state.Cqi = "No READ_PHONE_STATE permission"
        state.Bandwidth = "No READ_PHONE_STATE permission"
        state.Cellid = "No READ_PHONE_STATE permission"
        state.Mcc = "No READ_PHONE_STATE permission"
        state.Mnc = "No READ_PHONE_STATE permission"
        state.Lac = "No READ_PHONE_STATE permission"
        state.Tac = "No READ_PHONE_STATE permission"
        state.Pci = "No READ_PHONE_STATE permission"
        state.Earfcn = "No READ_PHONE_STATE permission"
        state.Ci = "No READ_PHONE_STATE permission"
        state.NetworkType = "No READ_PHONE_STATE permission"
        state.SignalStrength = "No READ_PHONE_STATE permission"
        state.BitErrorRate = "No READ_PHONE_STATE permission"
        state.TimingAdvance = "No READ_PHONE_STATE permission"
        state.Band = "No READ_PHONE_STATE permission"
        state.Operator = "No READ_PHONE_STATE permission"
        state.Technology = "No READ_PHONE_STATE permission"
    }

    private fun logCellInfo(state: MainActivity.MainActivityState) {
        Log.d(MainActivity.TAG, "RSRP value: ${state.Rsrp}")
        Log.d(MainActivity.TAG, "Rssi value: ${state.Rssi}")
        Log.d(MainActivity.TAG, "Rsrq value: ${state.Rsrq}")
        Log.d(MainActivity.TAG, "Rssnr value: ${state.Rssnr}")
        Log.d(MainActivity.TAG, "Cqi value: ${state.Cqi}")
        Log.d(MainActivity.TAG, "Bandwidth value: ${state.Bandwidth}")
        Log.d(MainActivity.TAG, "Cell ID value: ${state.Cellid}")
        Log.d(MainActivity.TAG, "MCC: ${state.Mcc}")
        Log.d(MainActivity.TAG, "MNC: ${state.Mnc}")
        Log.d(MainActivity.TAG, "LAC: ${state.Lac}")
        Log.d(MainActivity.TAG, "TAC: ${state.Tac}")
        Log.d(MainActivity.TAG, "PCI: ${state.Pci}")
        Log.d(MainActivity.TAG, "EARFCN: ${state.Earfcn}")
        Log.d(MainActivity.TAG, "CI: ${state.Ci}")
        Log.d(MainActivity.TAG, "Network Type: ${state.NetworkType}")
        Log.d(MainActivity.TAG, "Signal Strength: ${state.SignalStrength}")
        Log.d(MainActivity.TAG, "Bit Error Rate: ${state.BitErrorRate}")
        Log.d(MainActivity.TAG, "Timing Advance: ${state.TimingAdvance}")
        Log.d(MainActivity.TAG, "Band: ${state.Band}")
        Log.d(MainActivity.TAG, "Operator: ${state.Operator}")
        Log.d(MainActivity.TAG, "Technology: ${state.Technology}")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun saveCellInfoToJsonFile(context: Context, messageToData2: MessageToData2) {
        withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences(RECORDING_PREFS_NAME, Context.MODE_PRIVATE)
                val isRecording = sharedPreferences.getBoolean(KEY_IS_RECORDING, true)

                if (!isRecording) {
                    Log.d(TAG, "Recording is paused, skipping save")
                    return@withContext
                }

                val signalDataDir = File(context.getExternalFilesDir(null), "Signal_data")
                if (!signalDataDir.exists() && !signalDataDir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory: ${signalDataDir.absolutePath}")
                    return@withContext
                }

                val file = File(signalDataDir, fileName)
                val hasData = messageToData2.cdma.cellInfoList.isNotEmpty() ||
                        messageToData2.gsm.cellInfoList.isNotEmpty() ||
                        messageToData2.wcdma.cellInfoList.isNotEmpty() ||
                        messageToData2.lte.cellInfoList.isNotEmpty() ||
                        messageToData2.nr.cellInfoList.isNotEmpty()

                val hasLocationData = messageToData2.latitude != 0.0 && messageToData2.longitude != 0.0

                if (messageToData2.jwt.isEmpty() || messageToData2.UUID.isEmpty()) {
                    Log.d(TAG, "Skipping save: jwt or UUID is empty")
                    return@withContext
                }

                if (hasData && hasLocationData) {
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

                        if (messageToData2.cdma.cellInfoList.isNotEmpty()) {
                            put("cdma", Json.encodeToJsonElement(messageToData2.cdma.cellInfoList))
                        }
                        if (messageToData2.wcdma.cellInfoList.isNotEmpty()) {
                            put("wcdma", Json.encodeToJsonElement(messageToData2.wcdma.cellInfoList))
                        }
                        if (messageToData2.gsm.cellInfoList.isNotEmpty()) {
                            put("gsm", Json.encodeToJsonElement(messageToData2.gsm.cellInfoList))
                        }
                        if (messageToData2.lte.cellInfoList.isNotEmpty()) {
                            put("lte", Json.encodeToJsonElement(messageToData2.lte.cellInfoList))
                        }
                        if (messageToData2.nr.cellInfoList.isNotEmpty()) {
                            put("nr", Json.encodeToJsonElement(messageToData2.nr.cellInfoList))
                        }
                    }

                    val outputStream = FileOutputStream(file, true)
                    val jsonMessageToData2 = modifiedJson.toString() + "\n"
                    outputStream.write(jsonMessageToData2.toByteArray())
                    outputStream.close()

                    Log.d(TAG, "JSON to be saved:\n$jsonMessageToData2")

                    val fileSize = file.length()
                    Log.d(TAG, "Current file size: ${getReadableFileSize(fileSize)}")

                    if (fileSize >= 1_048_576) { // 1 МБ
                        sendFileWithRetry(context, file) { success ->
                            if (success) {
                                Log.d(TAG, "File sent successfully (from save function)")
                            } else {
                                Log.e(TAG, "Failed to send file (from save function)")
                            }
                        }

                        fileCounter = (fileCounter % maxFileCount) + 1
                        fileName = "Signal_data_$fileCounter.txt"
                    }

                    Log.d(TAG, "CellInfo saved to file: ${file.absolutePath}")
                } else {
                    Log.d(TAG, "Skipping empty CellInfo data")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error saving CellInfo to file: ", e)
            }
        }
    }

    internal suspend fun sendFileWithRetry(context: Context, file: File, onComplete: (Boolean) -> Unit) {
        var success = false
        var attempts = 0
        while (!success && attempts < 5) {
            success = try {
                sendFileToServer(file)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending file to server on attempt ${attempts + 1}: ", e)
                false
            }

            if (!success) {
                attempts++
                delay(5000)
            }
        }

        val (pendingCount, successCount) = getFileCounters(context)
        if (success) {
            Log.d(TAG, "File sent successfully: ${file.absolutePath}")
            file.delete()
            updateFileCounters(context, pendingCount, successCount + 1)
        } else {
            Log.e(TAG, "Failed to send file after 5 attempts: ${file.absolutePath}")
            updateFileCounters(context, pendingCount + 1, successCount)
        }
        onComplete(success)
    }

    private suspend fun sendFileToServer(file: File): Boolean {
        return suspendCancellableCoroutine { continuation ->
            var isCallbackHandled = false

            val callback = { success: Boolean ->
                if (!isCallbackHandled) {
                    isCallbackHandled = true
                    if (continuation.isActive) {
                        continuation.resume(success)
                    }
                }
            }

            MainActivity.networkManager.sendMessageToServerFromFile(file.absolutePath) { success ->
                callback(success)
            }

            continuation.invokeOnCancellation {
                if (!isCallbackHandled) {
                    isCallbackHandled = true
                    MainActivity.networkManager.webSocket?.close(1000, "Cancelled")
                    callback(false)
                }
            }
        }
    }

    private fun getReadableFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
    fun updateFileCounters(context: Context, pendingCount: Int, successCount: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt(KEY_PENDING_COUNT, pendingCount)
            putInt(KEY_SUCCESS_COUNT, successCount)
            apply()
        }
    }

    fun getFileCounters(context: Context): Pair<Int, Int> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pendingCount = sharedPreferences.getInt(KEY_PENDING_COUNT, 0)
        val successCount = sharedPreferences.getInt(KEY_SUCCESS_COUNT, 0)
        return Pair(pendingCount, successCount)
    }

    private fun getBandFromEarfcn(earfcn: Int): String {
        return when (earfcn) {
            in 1..600 -> "Band 1 (2100 MHz)"
            in 1200..1950 -> "Band 3 (1800 MHz)"
            in 1951..2650 -> "Band 4 (1700/2100 MHz AWS)"
            in 2750..3450 -> "Band 7 (2600 MHz)"
            in 3600..4150 -> "Band 8 (900 MHz)"
            in 6600..7100 -> "Band 20 (800 DD)"
            in 9210..9660 -> "Band 28 (700 APT)"
            in 9770..10280 -> "Band 38 (TD 2600)"
            in 25700..26200 -> "Band 41 (TD 2500)"
            in 65536..67535 -> "Band 71 (600 MHz)"
            else -> "Unknown"
        }
    }
}