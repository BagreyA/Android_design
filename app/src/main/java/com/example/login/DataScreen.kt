@file:Suppress("DEPRECATION")

package com.example.login

//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.reflect.full.declaredMemberProperties


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(state: MainActivity.MainActivityState, onNavigateTo: (Int) -> Unit) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedChartType by rememberSaveable { mutableStateOf("RSRP") }

    val cellInfoDataByType = remember { mutableStateOf(mapOf<String, List<CellInfoData>>()) }

    val cellIdToColorLTE = remember { mutableStateMapOf<String, Color>() }
    val cellIdToColorGSM = remember { mutableStateMapOf<String, Color>() }
    val cellIdToColorWCDMA = remember { mutableStateMapOf<String, Color>() }
    val cellIdToColorCDMA = remember { mutableStateMapOf<String, Color>() }
    val cellIdToColorNR = remember { mutableStateMapOf<String, Color>() }
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        while (true) {
            state.messageToData2?.let { messageToData2 ->
                cellInfoDataByType.value = mapOf(
                    "CDMA" to messageToData2.cdma.cellInfoList,
                    "GSM" to messageToData2.gsm.cellInfoList,
                    "LTE" to messageToData2.lte.cellInfoList,
                    "WCDMA" to messageToData2.wcdma.cellInfoList,
                    "NR" to messageToData2.nr.cellInfoList
                )
            }
            delay(500)
        }
    }

    val chartData = when (selectedTabIndex) {
        0 -> when (selectedChartType) {
            "RSRP" -> state.rsrpDetailedData
            "RSSI" -> state.rssiDetailedData
            "RSRQ" -> state.rsrqDetailedData
            "AsuLevel" -> state.asuLevelLTEDetailed
            "Level" -> state.levelLTEDetailed
            "Earfcn" -> state.earfcnLTEDetailed
            else -> state.rsrpDetailedData
        }
        1 -> when (selectedChartType) {
            "RSSI" -> state.rssiDataGsmDetailed
            "ARFCN" -> state.arfcnGsmDetailed
            else -> state.rssiDataGsmDetailed
        }
        2 -> when (selectedChartType) {
            "RSCP" -> state.rscpDataWcdmaDetailed
            "Level" -> state.levelWcdmaDetailed
            else -> state.rscpDataWcdmaDetailed
        }
        3 -> when (selectedChartType) {
            "RSSI" -> state.rssiDataCdmaDetailed
            "Level" -> state.levelCdmaDetailed
            else -> state.rssiDataCdmaDetailed
        }
        4 -> when (selectedChartType) {
            "RSSI" -> state.rssiDataNrDetailed
            "AsuLevel" -> state.asuLevelNrDetailed
            else -> state.rssiDataNrDetailed
        }
        else -> emptyList()
    }

    val currentCellIdToColor = when (selectedTabIndex) {
        0 -> cellIdToColorLTE
        1 -> cellIdToColorGSM
        2 -> cellIdToColorWCDMA
        3 -> cellIdToColorCDMA
        4 -> cellIdToColorNR
        else -> cellIdToColorLTE
    }

    if (state.isFullscreen) {
        FullscreenChartContent(
            chartData = chartData,
            state = state,
            onFullscreenToggle = { state.isFullscreen = false },
            networkType = getNetworkTypeShortName(selectedTabIndex),
            cellIdToColor = currentCellIdToColor
           )
    } else {
        Scaffold(
            topBar = {
                DataTopBar(
                    state = state,
                    onNavigateTo = onNavigateTo,
                    selectedTabIndex = selectedTabIndex,
                    onNetworkChange = { selectedTabIndex = it },
                    selectedChartType = selectedChartType,
                    onChartTypeChange = { selectedChartType = it }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF))
            ) {
                item {
                    DetailedChartContent(
                        chartData = chartData,
                        state = state,
                        onDismiss = {},
                        networkType = getNetworkTypeShortName(selectedTabIndex),
                        onFullscreenToggle = {
                            state.isFullscreen = !state.isFullscreen
                        },
                        cellIdToColor = currentCellIdToColor
                    )
                }

                item {
                    PhoneInfoCard(state)
                }

                items(cellInfoDataByType.value[getNetworkTypeShortName(selectedTabIndex)] ?: emptyList()) { cellInfo ->
                    CellInfoCard(cellType = getNetworkTypeShortName(selectedTabIndex), cellInfo = cellInfo)
                }
            }
        }
    }
}

@Composable
fun DataTopBar(state: MainActivity.MainActivityState,  onNavigateTo: (Int) -> Unit,  selectedTabIndex: Int,  onNetworkChange: (Int) -> Unit,  selectedChartType: String, onChartTypeChange: (String) -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    var expandedNetworkMenu by remember { mutableStateOf(false) }
    var expandedChartMenu by remember { mutableStateOf(false) }
    val networkNames = listOf("LTE", "GSM", "WCDMA", "CDMA", "NR")

    val chartTypesByNetwork = mapOf(
        0 to listOf("RSRP", "RSSI", "RSRQ", "AsuLevel", "Level", "Earfcn"),
        1 to listOf("RSSI", "ARFCN"),
        2 to listOf("RSCP", "Level"),
        3 to listOf("RSSI", "Level"),
        4 to listOf("RSSI", "AsuLevel")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF8F8F8))
            .border(
                width = 2.dp,
                color = if (isDarkTheme) Color(0x4D9E9E9E) else Color(0x809E9E9E),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onNavigateTo(5) }) {
                Image(
                    painter = painterResource(id = if (isDarkTheme) R.drawable.transition_dark else R.drawable.transition_light),
                    contentDescription = "Назад",
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.data),
                color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))

            Box {
                Button(
                    onClick = { expandedNetworkMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor =  if (isDarkTheme) Color(0xFF3C3C3E) else Color(0xFFFFFFFF)),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0x809E9E9E) else Color(0x4D9E9E9E)),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .width(110.dp)
                        .height(36.dp)
                        .padding(end = 20.dp)
                ) {
                    Text(
                        text = networkNames[selectedTabIndex],
                        color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xB334204C),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }

                DropdownMenu(
                    expanded = expandedNetworkMenu,
                    onDismissRequest = { expandedNetworkMenu = false },
                    modifier = Modifier
                        .width(90.dp)
                        .background(if (isDarkTheme) Color(0xFF3C3C3E) else Color(0xFFFFFFFF))
                        .align(Alignment.CenterStart)
                ) {
                    networkNames.forEachIndexed { index, name ->
                        DropdownMenuItem(onClick = {
                            onNetworkChange(index)
                            expandedNetworkMenu = false
                        }) {
                            Text(name, color =  if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xB334204C), fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Box {
                Button(
                    onClick = { expandedChartMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor =  if (isDarkTheme) Color(0xFF3C3C3E) else Color(0xFFFFFFFF)),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0x809E9E9E) else Color(0x4D9E9E9E)),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .width(110.dp)
                        .height(36.dp)
                        .padding(end = 20.dp)
                ) {
                    Text(
                        text = selectedChartType,
                        color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xB334204C),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }

                DropdownMenu(
                    expanded = expandedChartMenu,
                    onDismissRequest = { expandedChartMenu = false },
                    modifier = Modifier
                        .width(90.dp)
                        .background(if (isDarkTheme) Color(0xFF3C3C3E) else Color(0xFFFFFFFF))
                        .align(Alignment.CenterStart)
                ) {

                    chartTypesByNetwork[selectedTabIndex]?.forEach { chartType ->
                        DropdownMenuItem(onClick = {
                            onChartTypeChange(chartType)
                            expandedChartMenu = false
                        }) {
                            Text(chartType, color =  if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xB334204C), fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellInfoCard(cellType: String, cellInfo: CellInfoData) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color(0xFF3C3C3E) else Color(0xFFF8F8F8)
    val textColor =  if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xB334204C)
    val borderColor = if (isDarkTheme) Color(0x809E9E9E) else Color(0x4D9E9E9E)
    val paddingModifier = Modifier.padding(horizontal = 15.dp)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(paddingModifier),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cell Type: $cellType", style = MaterialTheme.typography.titleMedium, color = textColor)

            for (field in CellInfoData::class.declaredMemberProperties) {
                val fieldValue = field.getter.call(cellInfo)
                if (field.name != "type" &&
                    field.name != "timestamp" &&
                    fieldValue != null &&
                    fieldValue != "N/A"
                ) {
                    when (cellType) {
                        "LTE" -> {
                            if (field.name in listOf(
                                    "mcc", "mnc", "ci", "pci", "tac", "earfcn", "bandwidth", "rsrp",
                                    "rssi", "rsrq", "rssnr", "cqi", "timingAdvance", "level",
                                    "asuLevel"
                                )
                            ) {
                                Text(
                                    text = "${field.name.capitalize()}: $fieldValue",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                        "GSM" -> {
                            if (field.name in listOf(
                                    "mcc", "mnc", "lac", "cid", "arfcn", "bsic", "rssi", "bitErrorRate",
                                    "timingAdvance", "level", "asuLevel"
                                )
                            ) {
                                Text(
                                    text = "${field.name.capitalize()}: $fieldValue",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                        "WCDMA" -> {
                            if (field.name in listOf(
                                    "mcc", "mnc", "lac", "cid", "psc", "uarfcn", "rscp", "level",
                                    "asuLevel", "ecNo"
                                )
                            ) {
                                Text(
                                    text = "${field.name.capitalize()}: $fieldValue",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                        "CDMA" -> {
                            if (field.name in listOf(
                                    "sid", "nid", "bsid", "rssi", "ecIo", "evdoDbm", "evdoEcio",
                                    "evdoSnr", "cdmaLevel", "asuLevel", "level"
                                )
                            ) {
                                Text(
                                    text = "${field.name.capitalize()}: $fieldValue",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                        "NR" -> {
                            if (field.name in listOf(
                                    "mcc", "mnc", "nci", "pci", "tac", "nrarfcn", "csiRsrp",
                                    "csiRsrq", "csiSinr", "ssRsrp", "ssRsrq", "ssSinr", "asuLevel",
                                    "csiCqiTableIndex", "ssRsrpDbm", "timingAdvanceMicros", "csiCqiReport"
                                )
                            ) {
                                Text(
                                    text = "${field.name.capitalize()}: $fieldValue",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
            Divider(Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun PhoneInfoCard(state: MainActivity.MainActivityState) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color(0xFF3C3C3E) else Color(0xFFF8F8F8)
    val textColor =  if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xB334204C)
    val borderColor = if (isDarkTheme) Color(0x809E9E9E) else Color(0x4D9E9E9E)
    val paddingModifier = Modifier.padding(horizontal = 15.dp)
    val context = LocalContext.current
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    val cardBackgroundColor = backgroundColor

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(paddingModifier),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.phone_info),
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Divider(Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(id = R.string.device_model, Build.MODEL ?: "Unknown"),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.manufacturer, Build.MANUFACTURER ?: "Unknown"),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.brand, Build.BRAND ?: "Unknown"),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.android_version, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.board, Build.BOARD ?: "Unknown"),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.bootloader, Build.BOOTLOADER ?: "Unknown"),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.device, Build.DEVICE ?: "Unknown"),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.hardware, Build.HARDWARE ?: "Unknown"),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.product, Build.PRODUCT ?: "Unknown"),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.operator, telephonyManager?.networkOperatorName ?: "Unknown"),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.network_type, getNetworkType(telephonyManager?.networkType ?: 0)),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Text(
                    text = stringResource(id = R.string.sim_operator, telephonyManager?.simOperatorName ?: "N/A"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Text(
                    text = stringResource(id = R.string.sim_state, getSimState(telephonyManager?.simState ?: 0)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
            Text(
                text = stringResource(id = R.string.phone_type, getPhoneType(telephonyManager?.phoneType ?: 0)),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Text(
                    text = stringResource(id = R.string.data_network_type, getDataNetworkType(telephonyManager?.dataNetworkType ?: 0)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }

            Text(
                text = stringResource(id = R.string.latitude, state.Latitude),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.longitude, state.Longtitude),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = stringResource(id = R.string.altitude, state.Altitude),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager =
                    context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val subscriptions = subscriptionManager.activeSubscriptionInfoList

                if (subscriptions.isNullOrEmpty()) {
                    Text(
                        text = stringResource(id = R.string.no_sim_card),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                } else {
                    for (subscription in subscriptions) {
                        Text(
                            text = stringResource(
                                id = R.string.sim_info,
                                subscription.simSlotIndex + 1
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                        Text(
                            text = stringResource(
                                id = R.string.carrier_name,
                                subscription.carrierName ?: "Unknown"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Text(
                            text = stringResource(
                                id = R.string.country_iso,
                                subscription.countryIso ?: "Unknown"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Text(
                            text = stringResource(
                                id = R.string.number,
                                subscription.number ?: "Unknown"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getNetworkType(networkType: Int): String {
    return when (networkType) {
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> "Unknown"
        TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
        TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
        TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
        TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
        TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO rev. 0"
        TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO rev. A"
        TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
        TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
        TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
        TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
        TelephonyManager.NETWORK_TYPE_IDEN -> "iDen"
        TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO rev. B"
        TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
        TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
        TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
        TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
        TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
        TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
        TelephonyManager.NETWORK_TYPE_NR -> "NR (5G)"
        else -> "Unknown ($networkType)"
    }
}
private fun getNetworkTypeShortName(tabIndex: Int): String {
    return when (tabIndex) {
        0 -> "LTE"
        1 -> "GSM"
        2 -> "WCDMA"
        3 -> "CDMA"
        4 -> "NR"
        else -> "Unknown"
    }
}

private fun getSimState(simState: Int): String {
    return when (simState) {
        TelephonyManager.SIM_STATE_UNKNOWN -> "Unknown"
        TelephonyManager.SIM_STATE_ABSENT -> "Absent"
        TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
        TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network Locked"
        TelephonyManager.SIM_STATE_READY -> "Ready"
        TelephonyManager.SIM_STATE_NOT_READY -> "Not Ready"
        TelephonyManager.SIM_STATE_PERM_DISABLED -> "Permanently Disabled"
        TelephonyManager.SIM_STATE_CARD_IO_ERROR -> "Card IO Error"
        TelephonyManager.SIM_STATE_CARD_RESTRICTED -> "Card Restricted"
        else -> "Unknown ($simState)"
    }
}

private fun getPhoneType(phoneType: Int): String {
    return when (phoneType) {
        TelephonyManager.PHONE_TYPE_NONE -> "None"
        TelephonyManager.PHONE_TYPE_GSM -> "GSM"
        TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
        TelephonyManager.PHONE_TYPE_SIP -> "SIP"
        else -> "Unknown ($phoneType)"
    }
}

@RequiresApi(Build.VERSION_CODES.N)
private fun getDataNetworkType(dataNetworkType: Int): String {
    return when (dataNetworkType) {
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> "Unknown"
        TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
        TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
        TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
        TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
        TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO rev. 0"
        TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO rev. A"
        TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
        TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
        TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
        TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
        TelephonyManager.NETWORK_TYPE_IDEN -> "iDen"
        TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO rev. B"
        TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
        TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
        TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
        TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
        TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
        TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
        TelephonyManager.NETWORK_TYPE_NR -> "NR (5G)"
        else -> "Unknown Network Type"
    }
}
data class ChartDataPoint(val timestamp: Long, val value: Float, val cellId: String)

internal fun addChartDataForDetailedChart(chartData: MutableList<ChartDataPoint>, value: String, timestamp: Long, cellId: String) {
    val chartValue = value.replace(" dBm", "").replace(" dB", "").toFloatOrNull() ?: 0f

    chartData.add(ChartDataPoint(timestamp, chartValue, cellId))

    while (chartData.size > 1500) {
        chartData.removeAt(0)
    }
}

@Composable
fun DetailedChartContent(
    chartData: List<ChartDataPoint>,
    state: MainActivity.MainActivityState,
    onDismiss: () -> Unit,
    networkType: String,
    cellIdToColor: MutableMap<String, Color>,
    onFullscreenToggle: () -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val textMeasurer = rememberTextMeasurer()

    val maxChartValue = chartData.maxOfOrNull { it.value } ?: 0f
    val minChartValue = chartData.minOfOrNull { it.value } ?: 0f

    val yAxisMaxValue = if (maxChartValue == minChartValue) {
        maxChartValue + 10
    } else {
        ceil(maxChartValue / 10) * 10
    }

    val yAxisMinValue = if (maxChartValue == minChartValue) {
        minChartValue - 10
    } else {
        floor(minChartValue / 10) * 10
    }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val pointWidth = 10.dp
    val chartWidth = chartData.size * pointWidth * scale
    val chartHeight = if (state.isFullscreen) 600.dp else 300.dp

    val predefinedColors = listOf(
        Color(0xFFF44336),
        Color(0xFF9C27B0),
        Color(0xFF673AB7),
        Color(0xFF3F51B5),
        Color(0xFF2196F3),
        Color(0xFF009688),
        Color(0xFF4CAF50),
        Color(0xFFFFEB3B),
        Color(0xFFFF9800)
    )

    fun generateUniqueColor(): Color {
        var color: Color
        do {
            color = Color(
                (0..255).random(),
                (0..255).random(),
                (0..255).random()
            )
        } while (cellIdToColor.containsValue(color))
        return color
    }

    fun getColorForCellId(cellId: String): Color {
        return cellIdToColor.getOrPut(cellId) {
            if (cellIdToColor.size < predefinedColors.size) {
                predefinedColors[cellIdToColor.size]
            } else {
                generateUniqueColor()
            }
        }
    }
    Column {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            val isDarkTheme = isSystemInDarkTheme()
            val iconColor = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xB334204C)

            if (state.isFullscreen) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = iconColor
                    )
                }
            } else {
                IconButton(onClick = {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Scroll to End",
                        tint = iconColor
                    )
                }
                IconButton(onClick = onFullscreenToggle) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = iconColor
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(Unit) {
                    if (state.isFullscreen) {
                        detectTransformGestures { centroid, pan, zoom, rotation ->
                            scale = (scale * zoom).coerceAtLeast(1f)
                            offsetX += pan.x / scale - scrollState.value.toFloat() * (zoom - 1)
                            offsetY += pan.y / scale
                        }
                    }
                }
        ) {
            Row {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(chartHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stepSize = (yAxisMaxValue - yAxisMinValue) / 5f
                        for (i in 0..5) {
                            val y = chartHeight.toPx() - i * (chartHeight.toPx() / 5)
                            drawLine(
                                color = Color.LightGray,
                                start = Offset(0f, y),
                                end = Offset(chartWidth.toPx(), y),
                                strokeWidth = 1f
                            )
                            drawText(
                                textMeasurer = textMeasurer,
                                text = String.format("%.0f", yAxisMinValue + i * stepSize),
                                topLeft = Offset(5.dp.toPx(), y - 5.dp.toPx()),
                                style = TextStyle(color = Color.Gray, fontSize = 8.sp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .width(chartWidth)
                        .height(chartHeight)
                ) {
                    Canvas(modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                    ) {
                        var previousPoint: Offset? = null
                        var previousCellId: String? = null
                        var previousColor: Color? = null

                        chartData.forEachIndexed { index, dataPoint ->
                            val x = index * pointWidth.toPx()
                            val y = chartHeight.toPx() - ((dataPoint.value - yAxisMinValue) / (yAxisMaxValue - yAxisMinValue) * chartHeight.toPx())
                            val currentPoint = Offset(x, y)

                            val cellId = dataPoint.cellId
                            val color = getColorForCellId(cellId)

                            if (previousPoint != null) {
                                if (previousCellId != cellId) {
                                    val middleX = (previousPoint!!.x + currentPoint.x) / 2
                                    drawLine(
                                        color = previousColor!!,
                                        start = previousPoint!!,
                                        end = Offset(middleX, previousPoint!!.y),
                                        strokeWidth = 2f
                                    )
                                    drawLine(
                                        color = color,
                                        start = Offset(middleX, previousPoint!!.y),
                                        end = currentPoint,
                                        strokeWidth = 2f
                                    )
                                } else {
                                    drawLine(
                                        color = color,
                                        start = previousPoint!!,
                                        end = currentPoint,
                                        strokeWidth = 2f
                                    )
                                }
                            }

                            drawCircle(
                                color = color,
                                center = currentPoint,
                                radius = 4f
                            )

                            val textOffset = if (index % 2 == 0) {
                                Offset(x, (y - 10.dp.toPx()).coerceAtLeast(10.dp.toPx()))
                            } else {
                                Offset(x, y + 10.dp.toPx())
                            }
                            if (index != 0 && index % 5 == 0) {
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = String.format("%.0f", dataPoint.value),
                                    topLeft = textOffset,
                                    style = TextStyle(color = Color.Gray, fontSize = 8.sp)
                                )
                            }

                            drawLine(
                                color = Color.LightGray,
                                start = Offset(x, 0f),
                                end = Offset(x, chartHeight.toPx()),
                                strokeWidth = 1f
                            )

                            if (index != 0 && index % 5 == 0) {
                                val date = Date(dataPoint.timestamp)
                                val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                val formattedTime = format.format(date)
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = formattedTime,
                                    topLeft = Offset(x - 15.dp.toPx(), chartHeight.toPx() - 10.dp.toPx()),
                                    style = TextStyle(color = Color.Gray, fontSize = 6.sp)
                                )
                            }

                            previousPoint = currentPoint
                            previousCellId = cellId
                            previousColor = color
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .height(40.dp)
            ) {
                Row {
                    val isDarkTheme = isSystemInDarkTheme()
                    cellIdToColor.forEach { (cellId, color) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cell ID: $cellId",
                                color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xB334204C),
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenChartContent(
    chartData: List<ChartDataPoint>,
    state: MainActivity.MainActivityState,
    onFullscreenToggle: () -> Unit,
    networkType: String,
    cellIdToColor: MutableMap<String, Color>
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val isDarkTheme = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFF5F5F5))
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    scale *= zoom
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        DetailedChartContent(
            chartData = chartData,
            state = state,
            onDismiss = onFullscreenToggle,
            networkType = networkType,
            cellIdToColor = cellIdToColor,
            onFullscreenToggle = {}
        )
    }
}

internal fun addChartData(chartData: MutableList<Pair<Long, Float>>, value: String, timestamp: Long) {
    val chartValue = value.replace(" dBm", "").replace(" dB", "").toFloatOrNull() ?: 0f

    val lastIndex = chartData.indexOfLast { it.first < timestamp }

    if (lastIndex != -1) {
        val lastTimestamp = chartData[lastIndex].first
        val timeDifference = timestamp - lastTimestamp

        if (timeDifference > MainActivity.UPDATE_INTERVAL) {
            val missedUpdatesCount = (timeDifference / MainActivity.UPDATE_INTERVAL).toInt() - 1
            for (i in 1..missedUpdatesCount) {
                val missedTimestamp = lastTimestamp + i * MainActivity.UPDATE_INTERVAL
                chartData.add(lastIndex + i, Pair(missedTimestamp, chartValue))
            }
        } else {
            chartData.add(Pair(timestamp, chartValue))
        }
    } else {
        chartData.add(Pair(timestamp, chartValue))
    }

    while (chartData.size > 6) {
        chartData.removeAt(0)
    }
}

private fun getDefaultChartType(tabIndex: Int): String {
    return when (tabIndex) {
        0 -> "RSRP"
        1 -> "RSSI"
        2 -> "RSCP"
        3 -> "RSSI"
        4 -> "RSSI"
        else -> ""
    }
}