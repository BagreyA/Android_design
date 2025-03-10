@file:Suppress("DEPRECATION")

package com.example.login

//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit


@Composable
fun HourlyTrafficChart(appName: String, onClose: () -> Unit, context: Context, selectedDate: Calendar? = null) {
    val hourlyTrafficData = remember { mutableStateListOf<Pair<Int, Long>>() }
    val packageName = getPackageNameForApp(context, appName)
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(packageName, selectedDate) {
        launch(Dispatchers.IO) {
            if (packageName != null) {
                val data = getHourlyTrafficData(context, packageName, selectedDate)
                withContext(Dispatchers.Main) {
                    hourlyTrafficData.addAll(data)
                }
            }
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0x809E9E9E) else Color(0x4D9E9E9E)),
            color = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.hourly_traffic_for_app, appName),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C)
                )

                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C)
                    )
                }

                if (packageName != null) {
                    HourlyTrafficChartContent(hourlyTrafficData)
                } else {
                    Text("Error: Package not found", color = Color.Red)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun TotalHourlyTrafficChart(onClose: () -> Unit, context: Context, selectedDate: Calendar? = null) {
    val hourlyTrafficData = remember { mutableStateListOf<Pair<Int, AppTrafficData>>() }
    var showChart by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(Unit, selectedDate) {
        launch(Dispatchers.IO) {
            val data = getTotalHourlyTrafficData(context, selectedDate)
            withContext(Dispatchers.Main) {
                hourlyTrafficData.addAll(data)
            }
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.total_hourly_traffic),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C)
                )

                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C)
                    )
                }

                if (showChart) {
                    TotalHourlyTrafficLineChartContent(
                        hourlyTrafficData,
                        getAppTrafficData(context, 1).sortedByDescending { it.totalBytes }
                            .take(10).map { it.appName }, selectedDate
                    )
                } else {
                    TotalHourlyTrafficChartContent(hourlyTrafficData)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showChart = !showChart },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C)
                    ),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF4D9E9E9E))
                ) {
                    Text(
                        text = stringResource(if (showChart) R.string.show_diagram else R.string.show_chart),
                        color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun TotalHourlyTrafficChartContent(hourlyTrafficData: List<Pair<Int, AppTrafficData>>, selectedDate: Calendar? = null) {
    val scrollState = rememberScrollState()
    val hourWidth = 50.dp
    val isDarkTheme = isSystemInDarkTheme()

    val filteredHourlyTrafficData = if (selectedDate != null) {
        (0..23).map { hour ->
            val appTrafficData = hourlyTrafficData.find { it.first == hour }?.second ?: AppTrafficData("", "", 0, 0, 0, 0, 0)
            Pair(hour, appTrafficData)
        }
    } else {
        hourlyTrafficData
    }

    val chartWidth = hourWidth * 12
    val maxTraffic = filteredHourlyTrafficData.maxOfOrNull { it.second.totalBytes } ?: 1L
    val maxTrafficKb = (maxTraffic / 1024).toFloat()

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.height(200.dp).width(40.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepSize = maxTrafficKb / 5
                for (i in 0..5) {
                    val y = size.height - i * (size.height / 5)
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, y),
                        end = Offset(chartWidth.toPx(), y),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.0f", i * stepSize),
                        10f,
                        y,
                        android.graphics.Paint().apply {
                            textSize = 10.sp.toPx()
                            color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .width(chartWidth)
        ) {
            Canvas(modifier = Modifier.width(chartWidth).height(200.dp)) {
                filteredHourlyTrafficData.forEachIndexed { index, (hour, appTrafficData) ->
                    val x = index * hourWidth.toPx()
                    val trafficKb = (appTrafficData.totalBytes / 1024).toFloat()
                    val barHeight = (trafficKb / maxTrafficKb * size.height).coerceAtLeast(0f)

                    drawRect(
                        color = Color.Blue,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(
                            hourWidth.toPx() - 4.dp.toPx(),
                            barHeight
                        )
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        appTrafficData.appName,
                        x + hourWidth.toPx() / 2,
                        size.height - barHeight - 15.dp.toPx(),
                        android.graphics.Paint().apply {
                            textSize = 9.sp.toPx()
                            color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = Typeface.DEFAULT_BOLD
                        }
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        "%.1f Kb".format(trafficKb),
                        x + hourWidth.toPx() / 2,
                        size.height - barHeight - 3.dp.toPx(),
                        android.graphics.Paint().apply {
                            textSize = 8.sp.toPx()
                            color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%02d:00", hour),
                        x + hourWidth.toPx() / 2,
                        size.height + 15f,
                        android.graphics.Paint().apply {
                            textSize = 10.sp.toPx()
                            color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun TotalHourlyTrafficLineChartContent(hourlyTrafficData: List<Pair<Int, AppTrafficData>>, topAppNames: List<String>, selectedDate: Calendar? = null) {
    val scrollState = rememberScrollState()
    val hourWidth = 60.dp
    val isDarkTheme = isSystemInDarkTheme()

    val filteredHourlyTrafficData = if (selectedDate != null) {
        (0..23).map { hour ->
            val appTrafficData = hourlyTrafficData.find { it.first == hour }?.second ?: AppTrafficData("", "", 0, 0, 0, 0, 0)
            Pair(hour, appTrafficData)
        }
    } else {
        hourlyTrafficData
    }.filter { (_, appTrafficData) -> appTrafficData.totalBytes > 0 }

    val totalTrafficByHour = MutableList(filteredHourlyTrafficData.size) { TotalTrafficData(0, 0, 0) }

    val currentTotalTraffic = mutableMapOf<Int, Long>()

    filteredHourlyTrafficData.forEachIndexed { index, (hour, appTrafficData) ->
        val currentHourIndex = index

        currentTotalTraffic[currentHourIndex] = (currentTotalTraffic[currentHourIndex] ?: 0) + appTrafficData.totalBytes

        totalTrafficByHour[currentHourIndex] = TotalTrafficData(
            currentTotalTraffic[currentHourIndex]!!,
            totalTrafficByHour[currentHourIndex].mobileBytes + appTrafficData.mobileBytes,
            totalTrafficByHour[currentHourIndex].wifiBytes + appTrafficData.wifiBytes
        )
    }

    val maxTraffic = maxOf(totalTrafficByHour.maxOfOrNull { it.totalBytes } ?: 1L, 1L)
    val maxTrafficKb = (maxTraffic / 1024).toFloat()

    val totalTrafficData = filteredHourlyTrafficData.mapIndexed { index, (hour, _) ->
        Pair(hour, totalTrafficByHour[index].totalBytes)
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.height(200.dp).width(40.dp)) {
            val stepSize = maxTrafficKb / 5
            for (i in 0..5) {
                val y = size.height - i * (size.height / 5)
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.0f Kb", i * stepSize),
                    10f,
                    y,
                    android.graphics.Paint().apply {
                        textSize = 8.sp.toPx()
                        color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                        textAlign = android.graphics.Paint.Align.LEFT
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.width(hourWidth * totalTrafficData.size + 150.dp).height(200.dp)) {
                val stepSize = maxTrafficKb / 5
                for (i in 0..5) {
                    val y = size.height - i * (size.height / 5)
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, y),
                        end = Offset((hourWidth * totalTrafficData.size).toPx(), y),
                        strokeWidth = 1f
                    )
                }

                drawTrafficLine(totalTrafficData, maxTrafficKb, Color.Black)

                val lineColors = listOf(
                    Color(0xFF00FFFF), // Голубой
                    Color(0xFFFF00FF), // Розовый
                    Color(0xFF4DAF4A), // Зеленый
                    Color(0xFF984EA3), // Фиолетовый
                    Color(0xFFE41A1C), // Красный
                    Color(0xFFFFA500), // Оранжевый
                    Color(0xFFA65628), // Коричневый
                    Color(0xFF377EB8), // Синий
                    Color(0xFFF781BF), // Светло-розовый
                    Color(0xFF999999)  // Серый
                )

                val blackIndex = topAppNames.indexOfFirst { it == "Total" }
                if (blackIndex != -1) {
                    val newColors = lineColors.toMutableList()
                    newColors[blackIndex] = Color.DarkGray
                    topAppNames.forEachIndexed { index, appName ->
                        val appTrafficByHour = totalTrafficByHour.mapIndexed { hourIndex, totalTraffic ->
                            val currentHour = filteredHourlyTrafficData[hourIndex].first
                            val appTraffic = filteredHourlyTrafficData.find { (dataHour, appData) ->
                                dataHour == currentHour && appData.appName == appName
                            }?.second?.totalBytes ?: 0L
                            Pair(currentHour, appTraffic)
                        }

                        drawTrafficLine(appTrafficByHour, maxTrafficKb, newColors[index])
                    }
                } else {
                    topAppNames.forEachIndexed { index, appName ->
                        val appTrafficByHour = totalTrafficByHour.mapIndexed { hourIndex, totalTraffic ->
                            val currentHour = filteredHourlyTrafficData[hourIndex].first
                            val appTraffic = filteredHourlyTrafficData.find { (dataHour, appData) ->
                                dataHour == currentHour && appData.appName == appName
                            }?.second?.totalBytes ?: 0L
                            Pair(currentHour, appTraffic)
                        }

                        drawTrafficLine(appTrafficByHour, maxTrafficKb, lineColors[index])
                    }
                }

                val legendSpacing = 15.dp
                var legendY = 10.dp

                drawRect(
                    color = Color.Black,
                    topLeft = Offset((hourWidth * totalTrafficData.size + 10.dp).toPx(), legendY.toPx()),
                    size = Size(20.dp.toPx(), 10.dp.toPx())
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "Total",
                    (hourWidth * totalTrafficData.size + 40.dp).toPx(),
                    legendY.toPx() + 8.dp.toPx(),
                    android.graphics.Paint().apply {
                        textSize = 8.sp.toPx()
                        color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                        textAlign = android.graphics.Paint.Align.LEFT
                    }
                )

                legendY += legendSpacing

                topAppNames.forEachIndexed { index, appName ->
                    if (appName != "Total") {
                        drawRect(
                            color = lineColors[index],
                            topLeft = Offset((hourWidth * totalTrafficData.size + 10.dp).toPx(), legendY.toPx()),
                            size = Size(20.dp.toPx(), 10.dp.toPx())
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            appName,
                            (hourWidth * totalTrafficData.size + 40.dp).toPx(),
                            legendY.toPx() + 8.dp.toPx(),
                            android.graphics.Paint().apply {
                                textSize = 8.sp.toPx()
                                color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                                textAlign = android.graphics.Paint.Align.LEFT
                            }
                        )
                        legendY += legendSpacing
                    }
                }

                for ((index, hour) in totalTrafficData.map { it.first }.withIndex()) {
                    val x = index * hourWidth.toPx()
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%02d:00", hour),
                        x + hourWidth.toPx() / 2,
                        size.height + 15f,
                        android.graphics.Paint().apply {
                            textSize = 10.sp.toPx()
                            color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.M)
private fun DrawScope.drawTrafficLine(
    trafficData: List<Pair<Int, Long>>,
    maxTrafficKb: Float,
    lineColor: Color,
    lineLabel: String = ""
) {
    var previousPoint: Offset? = null
    trafficData.forEachIndexed { index, (hour, traffic) ->
        val x = index * 60.dp.toPx() + 40.dp.toPx()
        val trafficKb = (traffic / 1024).toFloat()
        val y = size.height - (trafficKb / maxTrafficKb * size.height).coerceAtLeast(0f)
        val currentPoint = Offset(x, y)

        if (previousPoint != null) {
            val strokeWidth = if (lineColor == Color.Black) 4f else 2f
            drawLine(
                color = lineColor,
                start = previousPoint!!,
                end = currentPoint,
                strokeWidth = strokeWidth
            )
        }
        previousPoint = currentPoint
    }

}
@RequiresApi(Build.VERSION_CODES.M)
fun getTotalHourlyTrafficData(context: Context, selectedDate: Calendar? = null): List<Pair<Int, AppTrafficData>> {
    val result = mutableListOf<Pair<Int, AppTrafficData>>()
    val packageManager = context.packageManager
    val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as android.app.usage.NetworkStatsManager

    val calendarStart = Calendar.getInstance()

    if (selectedDate != null) {
        calendarStart.timeInMillis = selectedDate.timeInMillis
        calendarStart.set(Calendar.HOUR_OF_DAY, 0)
        calendarStart.set(Calendar.MINUTE, 0)
        calendarStart.set(Calendar.SECOND, 0)
        calendarStart.set(Calendar.MILLISECOND, 0)
    } else {
        calendarStart.add(Calendar.DAY_OF_YEAR, -1)
    }

    val startTime = calendarStart.timeInMillis

    val endTime = if (selectedDate != null) {
        calendarStart.apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    } else {
        startTime + TimeUnit.DAYS.toMillis(1)
    }

    val trafficData = mutableListOf<TrafficDataPoint>()

    for (packageInfo in packageManager.getInstalledApplications(PackageManager.GET_META_DATA)) {
        val uid = packageInfo.uid
        val appName = packageManager.getApplicationLabel(packageInfo).toString()

        for (networkType in listOf(ConnectivityManager.TYPE_MOBILE, ConnectivityManager.TYPE_WIFI)) {
            try {
                val networkStats = networkStatsManager.queryDetailsForUid(networkType, null, startTime, endTime, uid)
                while (networkStats.hasNextBucket()) {
                    val bucket = android.app.usage.NetworkStats.Bucket()
                    networkStats.getNextBucket(bucket)
                    trafficData.add(TrafficDataPoint(appName, bucket.rxBytes + bucket.txBytes, bucket.endTimeStamp))
                }
                networkStats.close()
            } catch (e: Exception) {
                Log.e(MainActivity.TAG, "Error fetching traffic data: ${e.message}", e)
                continue
            }
        }
    }

    trafficData.sortBy { it.timestamp }

    var currentHour = (calendarStart.get(Calendar.HOUR_OF_DAY) + 1) % 24
    var currentHourTraffic = mutableMapOf<String, Long>()
    var maxTrafficApp: AppTrafficData? = null
    var maxTraffic = 0L

    val hours = (0..23).toList()

    for (hour in hours.subList(currentHour, hours.size) + hours.subList(0, currentHour)) {
        val hourData = trafficData.filter { dataPoint ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = dataPoint.timestamp
            val dataHour = calendar.get(Calendar.HOUR_OF_DAY)
            dataHour == hour || (dataHour == (hour - 1 + 24) % 24 && calendar.get(Calendar.MINUTE) >= 55)
        }

        for (dataPoint in hourData) {
            currentHourTraffic[dataPoint.appName] = (currentHourTraffic[dataPoint.appName] ?: 0L) + dataPoint.bytes

            if (currentHourTraffic[dataPoint.appName]!! > maxTraffic) {
                maxTraffic = currentHourTraffic[dataPoint.appName]!!
                maxTrafficApp = AppTrafficData(
                    dataPoint.appName,
                    getPackageNameForApp(context, dataPoint.appName) ?: "",
                    maxTraffic,
                    0L,
                    0L,
                    0L,
                    0L
                )
            }
        }

        if (maxTrafficApp != null) {
            val resultHour = (hour - 1 + 24) % 24
            result.add(Pair(resultHour, maxTrafficApp))
        }

        currentHourTraffic = mutableMapOf()
        maxTrafficApp = null
        maxTraffic = 0L
    }

    return result
}

data class TrafficDataPoint(val appName: String, val bytes: Long, val timestamp: Long)

@SuppressLint("QueryPermissionsNeeded")
@RequiresApi(Build.VERSION_CODES.M)
fun getPackageNameForApp(context: Context, appName: String): String? {
    val packageManager = context.packageManager
    for (packageInfo in packageManager.getInstalledApplications(PackageManager.GET_META_DATA)) {
        val currentAppName = packageManager.getApplicationLabel(packageInfo).toString()
        if (currentAppName == appName) {
            return packageInfo.packageName
        }
    }
    return null
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun HourlyTrafficChartContent(hourlyTrafficData: List<Pair<Int, Long>>, selectedDate: Calendar? = null) {
    val scrollState = rememberScrollState()
    val hourWidth = 50.dp
    val isDarkTheme = isSystemInDarkTheme()

    val filteredData = if (selectedDate != null) {
        (0..23).map { hour ->
            val traffic = hourlyTrafficData.find { it.first == hour }?.second ?: 0L
            Pair(hour, traffic)
        }
    } else {
        hourlyTrafficData
    }.filter { it.second > 0 }

    val chartWidth = hourWidth * filteredData.size
    val maxTraffic = filteredData.maxOfOrNull { it.second } ?: 1L
    val maxTrafficKb = (maxTraffic / 1024).toFloat()

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.height(200.dp).width(40.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepSize = maxTrafficKb / 5
                for (i in 0..5) {
                    val y = size.height - i * (size.height / 5)
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.0f Kb", i * stepSize),
                        10f,
                        y,
                        android.graphics.Paint().apply {
                            textSize = 10.sp.toPx()
                            color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.width(chartWidth).height(200.dp)) {
                for (i in 0..5) {
                    val y = size.height - i * (size.height / 5)
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, y),
                        end = Offset(chartWidth.toPx(), y),
                        strokeWidth = 1f
                    )
                }

                filteredData.forEachIndexed { index, (hour, traffic) ->
                    val x = index * hourWidth.toPx()
                    val trafficKb = (traffic / 1024).toFloat()
                    val barHeight = (trafficKb / maxTrafficKb * size.height).coerceAtLeast(0f)

                    drawRect(
                        color = Color.Blue,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(
                            hourWidth.toPx() - 4.dp.toPx(),
                            barHeight
                        )
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%02d:00", hour),
                        x + hourWidth.toPx() / 2,
                        size.height + 15f,
                        android.graphics.Paint().apply {
                            textSize = 10.sp.toPx()
                            color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.1f Kb", trafficKb),
                        x + hourWidth.toPx() / 2,
                        size.height - barHeight - 5.dp.toPx(),
                        android.graphics.Paint().apply {
                            textSize = 8.sp.toPx()
                            color = if (isDarkTheme) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
fun getHourlyTrafficData(context: Context, packageName: String, selectedDate: Calendar? = null): List<Pair<Int, Long>> {
    val calendar = Calendar.getInstance()

    val currentHour = if (selectedDate != null) 0 else calendar.get(Calendar.HOUR_OF_DAY)

    if (selectedDate != null) {
        calendar.timeInMillis = selectedDate.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    } else {
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, currentHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    val startTime = calendar.timeInMillis

    val endTime = if (selectedDate != null) {
        calendar.apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } else {
        System.currentTimeMillis()
    }

    val hourlyTrafficData = MutableList(24) { 0L }
    val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as android.app.usage.NetworkStatsManager
    val packageManager = context.packageManager

    val uid = try {
        packageManager.getApplicationInfo(packageName, 0).uid
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e(MainActivity.TAG, "Error: Package $packageName not found", e)
        return emptyList()
    }

    try {
        var previousTotalBytes = 0L

        val hourRange = if (selectedDate != null) 0 until 24 else 0 until 24

        for (hour in hourRange) {
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            val currentHourEnd = calendar.timeInMillis

            var totalBytes = 0L
            totalBytes += getTrafficForHour(networkStatsManager, uid, startTime, currentHourEnd, android.net.ConnectivityManager.TYPE_MOBILE)
            totalBytes += getTrafficForHour(networkStatsManager, uid, startTime, currentHourEnd, android.net.ConnectivityManager.TYPE_WIFI)

            hourlyTrafficData[hour] = totalBytes - previousTotalBytes
            previousTotalBytes = totalBytes
        }
    } catch (e: Exception) {
        Log.e(MainActivity.TAG, "Error fetching network stats: ${e.message}", e)
    }

    return hourlyTrafficData.mapIndexed { index, traffic ->
        Pair((index + currentHour) % 24, traffic)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
internal fun getTotalTrafficData(context: Context, days: Int): TotalTrafficData {
    var mobileBytes = 0L
    var wifiBytes = 0L
    val currentTime = System.currentTimeMillis()
    val startTime = currentTime - TimeUnit.DAYS.toMillis(days.toLong())

    val networkStatsManager = context.getSystemService(Activity.NETWORK_STATS_SERVICE) as android.app.usage.NetworkStatsManager

    try {
        val mobileStats = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, startTime, currentTime)
        mobileBytes = mobileStats.rxBytes + mobileStats.txBytes

        val wifiStats = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, startTime, currentTime)
        wifiBytes = wifiStats.rxBytes + wifiStats.txBytes
    } catch (e: Exception) {
        Log.e(MainActivity.TAG, "Error fetching network stats", e)
    }

    return TotalTrafficData(mobileBytes + wifiBytes, mobileBytes, wifiBytes)
}
@RequiresApi(Build.VERSION_CODES.M)
private fun getTrafficForHour(networkStatsManager: android.app.usage.NetworkStatsManager, uid: Int, startTime: Long, endTime: Long, networkType: Int): Long {
    var totalBytes = 0L
    try {
        val networkStats = networkStatsManager.queryDetailsForUid(
            networkType,
            null,
            startTime,
            endTime,
            uid
        )
        while (networkStats.hasNextBucket()) {
            val bucket = android.app.usage.NetworkStats.Bucket()
            networkStats.getNextBucket(bucket)
            totalBytes += bucket.rxBytes + bucket.txBytes
        }
        networkStats.close()
    } catch (e: Exception) {
        Log.e(MainActivity.TAG, "Error fetching network stats: ${e.message}", e)
    }
    return totalBytes
}
@SuppressLint("QueryPermissionsNeeded")
@RequiresApi(Build.VERSION_CODES.M)
fun getAppTrafficData(context: Context, days: Int): List<AppTrafficData> {
    val appTrafficDataList = mutableListOf<AppTrafficData>()
    val packageManager = context.packageManager
    val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as android.app.usage.NetworkStatsManager

    val currentTime = System.currentTimeMillis()
    val startTime = currentTime - TimeUnit.DAYS.toMillis(days.toLong())

    for (packageInfo in packageManager.getInstalledApplications(PackageManager.GET_META_DATA)) {
        val uid = packageInfo.uid
        val appName = packageManager.getApplicationLabel(packageInfo).toString()

        var mobileBytes = 0L
        var wifiBytes = 0L
        var totalDownlinkBytes = 0L
        var totalUplinkBytes = 0L

        try {
            val mobileStats = networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                currentTime,
                uid
            )
            var bucket = android.app.usage.NetworkStats.Bucket()
            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(bucket)
                if (bucket.uid == uid) {
                    mobileBytes += bucket.rxBytes + bucket.txBytes
                    totalDownlinkBytes += bucket.rxBytes
                    totalUplinkBytes += bucket.txBytes
                }
            }
            mobileStats.close()

            val wifiStats = networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                currentTime,
                uid
            )
            bucket = android.app.usage.NetworkStats.Bucket()
            while (wifiStats.hasNextBucket()) {
                wifiStats.getNextBucket(bucket)
                if (bucket.uid == uid) {
                    wifiBytes += bucket.rxBytes + bucket.txBytes
                    totalDownlinkBytes += bucket.rxBytes
                    totalUplinkBytes += bucket.txBytes
                }
            }
            wifiStats.close()

            val totalBytes = mobileBytes + wifiBytes
            appTrafficDataList.add(
                AppTrafficData(
                    appName,
                    packageInfo.packageName,
                    totalBytes,
                    mobileBytes,
                    wifiBytes,
                    totalDownlinkBytes,
                    totalUplinkBytes
                )
            )

        } catch (e: Exception) {
            Log.e("AppTraffic", "Error getting traffic data for $appName: ${e.message}", e)
        }
    }

    return appTrafficDataList
}

@RequiresApi(Build.VERSION_CODES.M)
fun getAppTrafficDataForDays(context: Context, daysList: List<Int>): List<AppTrafficData> {
    val appTrafficDataList = mutableListOf<AppTrafficData>()
    val packageManager = context.packageManager
    val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as android.app.usage.NetworkStatsManager

    for (daysAgo in daysList) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val startTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = calendar.timeInMillis

        for (packageInfo in packageManager.getInstalledApplications(PackageManager.GET_META_DATA)) {
            val uid = packageInfo.uid
            val appName = packageManager.getApplicationLabel(packageInfo).toString()

            var mobileBytes = 0L
            var wifiBytes = 0L
            var totalDownlinkBytes = 0L
            var totalUplinkBytes = 0L

            try {
                val mobileStats = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_MOBILE,
                    null,
                    startTime,
                    endTime,
                    uid
                )
                var bucket = android.app.usage.NetworkStats.Bucket()
                while (mobileStats.hasNextBucket()) {
                    mobileStats.getNextBucket(bucket)
                    if (bucket.uid == uid) {
                        mobileBytes += bucket.rxBytes + bucket.txBytes
                        totalDownlinkBytes += bucket.rxBytes
                        totalUplinkBytes += bucket.txBytes
                    }
                }
                mobileStats.close()

                val wifiStats = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_WIFI,
                    null,
                    startTime,
                    endTime,
                    uid
                )
                bucket = android.app.usage.NetworkStats.Bucket()
                while (wifiStats.hasNextBucket()) {
                    wifiStats.getNextBucket(bucket)
                    if (bucket.uid == uid) {
                        wifiBytes += bucket.rxBytes + bucket.txBytes
                        totalDownlinkBytes += bucket.rxBytes
                        totalUplinkBytes += bucket.txBytes
                    }
                }
                wifiStats.close()

                val totalBytes = mobileBytes + wifiBytes
                appTrafficDataList.add(
                    AppTrafficData(
                        appName,
                        packageInfo.packageName,
                        totalBytes,
                        mobileBytes,
                        wifiBytes,
                        totalDownlinkBytes,
                        totalUplinkBytes
                    )
                )

            } catch (e: Exception) {
                Log.e("AppTraffic", "Error getting traffic data for $appName: ${e.message}", e)
            }
        }
    }
    return appTrafficDataList
}

@RequiresApi(Build.VERSION_CODES.M)
fun getTotalTrafficDataForDays(context: Context, daysList: List<Int>): TotalTrafficData {
    var totalMobileBytes = 0L
    var totalWifiBytes = 0L

    for (daysAgo in daysList) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val startTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = calendar.timeInMillis

        val networkStatsManager =
            context.getSystemService(Activity.NETWORK_STATS_SERVICE) as android.app.usage.NetworkStatsManager

        try {
            val mobileStats = networkStatsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime
            )
            totalMobileBytes += mobileStats.rxBytes + mobileStats.txBytes

            val wifiStats = networkStatsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                endTime
            )
            totalWifiBytes += wifiStats.rxBytes + wifiStats.txBytes
        } catch (e: Exception) {
            Log.e(MainActivity.TAG, "Error fetching network stats", e)
        }
    }

    return TotalTrafficData(
        totalMobileBytes + totalWifiBytes,
        totalMobileBytes,
        totalWifiBytes
    )
}