package com.example.login

import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
    val Latitude: String,
    val longitude: String,
    val rsrp: String,
    val Rssi: String,
    val Rsrq: String,
    val Rssnr: String,
    val Cqi: String,
    val Bandwidth: String,
    val Cellid: String,
    val timestamp: String
)

@Serializable
data class MessageData(
    val jwt: String,
    val uuid: String,
    val time: String,
    val latitude: Double,
    val Longitude: Double,
    val rsrp: Long,
    val rssi: Long,
    val rsrq: Long,
    val rssnr: Long,
    val cqi: Long,
    val bandwidth: Long,
    val cellID: Long,
    val physcellid: Long
)

@Serializable
data class RegisterResponse(
    val email: String,
    val jwt: String,
    val message: String,
    val uuid: String
)

@Serializable
data class AuthResponse(
    val email: String,
    val jwt: String,
    val uuid: String
)
@Serializable
data class AppTrafficData(
    val appName: String,
    val packageName: String,
    val totalBytes: Long,
    val mobileBytes: Long,
    val wifiBytes: Long,
    val rxBytes: Long,
    val txBytes: Long
)

data class TotalTrafficData(
    val totalBytes: Long,
    val mobileBytes: Long,
    val wifiBytes: Long
)

@Serializable
data class CellInfoData(
    val type: String,
    val timestamp: Long,
    val registered: Boolean,
    val mcc: Long? = null,
    val mnc: Long? = null,
    val lac: Long? = null,
    val cid: Long? = null,
    val arfcn: Long? = null,
    val bsic: Long? = null,
    val ci: Long? = null,
    val pci: Long? = null,
    val tac: Long? = null,
    val earfcn: Long? = null,
    val bandwidth: Long? = null,
    val rsrp: Long? = null,
    val rssi: Long? = null,
    val rsrq: Long? = null,
    val rssnr: Long? = null,
    val cqi: Long? = null,
    val timingAdvance: Long? = null,
    val psc: Long? = null,
    val uarfcn: Long? = null,
    val rscp: Long? = null,
    val ecno: Long? = null,
    val sid: Long? = null,
    val nid: Long? = null,
    val bsid: Long? = null,
    val ecIo: Long? = null,
    val evdoDbm: Long? = null,
    val evdoEcio: Long? = null,
    val evdoSnr: Long? = null,
    val nci: Long? = null,
    val nrarfcn: Long? = null,
    val csiRsrp: Long? = null,
    val csiRsrq: Long? = null,
    val csiSinr: Long? = null,
    val ssRsrp: Long? = null,
    val ssRsrq: Long? = null,
    val ssSinr: Long? = null,
    val level: Long? = null,
    val parametersUseForLevel: Long? = null,
    val cqiTableIndex: Long? = null,
    val bitErrorRate: Long? = null,
    val operator: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val asuLevel: Int? = null,
    val ecNo: Long? = null,
    val evdoLevel: Int? = null,
    val csiCqiTableIndex: Long? = null,
    val ssRsrpDbm: Long? = null,
    val timingAdvanceMicros: Long? = null,
    val csiCqiReport: List<Long?>? = null
)

@Serializable
data class CdmaData(val cellInfoList: List<CellInfoData> = emptyList())

@Serializable
data class GsmData(val cellInfoList: List<CellInfoData> = emptyList())

@Serializable
data class WcdmaData(val cellInfoList: List<CellInfoData> = emptyList())
@Serializable
data class LteData(val cellInfoList: List<CellInfoData> = emptyList())

@Serializable
data class NRData(val cellInfoList: List<CellInfoData> = emptyList())

@Serializable
data class MessageToData2(
    val jwt: String,
    val UUID: String,
    val time: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val manufacturer: String? = null,
    val model: String? = null,
    val androidVersion: String? = null,
    val hardware: String? = null,
    val product: String? = null,
    val board: String? = null,
    val operator: String? = null,
    val cdma: CdmaData = CdmaData(),
    val wcdma: WcdmaData = WcdmaData(),
    val gsm: GsmData = GsmData(),
    val lte: LteData = LteData(),
    val nr: NRData = NRData()
)