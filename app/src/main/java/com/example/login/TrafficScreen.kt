package com.example.login

import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.DatePicker
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit


@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun TrafficScreen(state: MainActivity.MainActivityState) {
    val context = LocalContext.current
    val appTrafficData = remember { mutableStateOf(emptyList<AppTrafficData>()) }
    var days by remember { mutableStateOf("1") }
    var showError by remember { mutableStateOf(false) }
    var showAppChart by remember { mutableStateOf(false) }
    var showTotalChart by remember { mutableStateOf(false) }
    var selectedAppName by remember { mutableStateOf("") }
    val totalTrafficData = remember { mutableStateOf(TotalTrafficData(0L, 0L, 0L)) }
    var isSendingTrafficData by remember { mutableStateOf(false) }

    var selectedCalendar by remember { mutableStateOf<Calendar?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var activeMode by remember { mutableStateOf("days") }

    fun onDaysChanged(newDays: String) {
        days = newDays
        activeMode = "days"
    }

    LaunchedEffect(appTrafficData.value, days, selectedCalendar, activeMode) {
        if (activeMode == "calendar" && selectedCalendar != null) {
            val daysList = listOf(
                TimeUnit.MILLISECONDS.toDays(
                    Calendar.getInstance().timeInMillis - selectedCalendar!!.timeInMillis
                ).toInt()
            )
            appTrafficData.value =
                getAppTrafficDataForDays(context, daysList).sortedByDescending { it.totalBytes }
            totalTrafficData.value = getTotalTrafficDataForDays(context, daysList)
        } else if (activeMode == "days") {
            val daysList = days.split(",").mapNotNull { it.toIntOrNull() }
            if (daysList.isNotEmpty()) {
                showError = false
                while (true) {
                    appTrafficData.value =
                        getAppTrafficData(context, daysList.sum()).sortedByDescending { it.totalBytes }
                    totalTrafficData.value = getTotalTrafficData(context, daysList.sum())
                    delay(5000)
                }
            } else {
                showError = true
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Total Traffic:", fontWeight = FontWeight.SemiBold)
                    Text(text = "${(totalTrafficData.value.totalBytes / 1024)} Kb")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Mobile:", fontWeight = FontWeight.SemiBold)
                    Text(text = "${(totalTrafficData.value.mobileBytes / 1024)} Kb")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Wi-Fi:", fontWeight = FontWeight.SemiBold)
                    Text(text = "${(totalTrafficData.value.wifiBytes / 1024)} Kb")
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = days,
                    onValueChange = { onDaysChanged(it) },
                    label = { Text("Days") },
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Choose Date"
                    )
                }
            }
        }

        item {
            if (showError) {
                Text(
                    text = "Invalid number of days",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item {
            if (!PermissionUtils.hasUsageStatsPermission(context)) {
                Button(onClick = { PermissionUtils.requestUsageStatsPermission(context) }) {
                    Text("Grant Usage Stats Permission")
                }
            }
        }

        item {
            Button(onClick = {
                isSendingTrafficData = true
                MainActivity.networkManager.authenticateForTraffic(state.Email, state.Password) { authResponse ->
                    if (authResponse != null) {
                        val top5Apps = appTrafficData.value.take(10)
                        MainActivity.networkManager.sendTrafficDataToServer(authResponse.jwt, top5Apps) { success ->
                            if (success) {
                                Log.d(MainActivity.TAG, "Traffic data sent successfully!")
                            } else {
                                Log.e(MainActivity.TAG, "Failed to send traffic data")
                            }
                        }
                    } else {
                        Log.e(MainActivity.TAG, "Authentication for traffic data failed")
                    }
                    isSendingTrafficData = false
                }
            }) {
                Text("Send Data to Server")
            }
        }

        item {
            Button(
                onClick = { showTotalChart = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Show Total Hourly Traffic")
            }
        }

        items(appTrafficData.value, key = { it.appName }) { appData ->
            TrafficItem(appData) { appName ->
                selectedAppName = appName
                showAppChart = true
            }
        }
    }

    if (showAppChart) {
        HourlyTrafficChart(
            appName = selectedAppName,
            onClose = { showAppChart = false },
            context = context,
            selectedDate = selectedCalendar
        )
    }

    if (showTotalChart) {
        TotalHourlyTrafficChart(
            onClose = { showTotalChart = false },
            context = context,
            selectedDate = if (activeMode == "calendar") selectedCalendar else null
        )
    }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                activeMode = "calendar"
                showDatePicker = false

            },
            year,
            month,
            day
        )

        datePickerDialog.setOnCancelListener {
            showDatePicker = false
            activeMode = "days"
        }

        datePickerDialog.setOnDismissListener {
            showDatePicker = false
        }

        datePickerDialog.show()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun TrafficItem(appData: AppTrafficData, onShowChart: (String) -> Unit) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<Drawable?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(appData) {
        coroutineScope.launch(Dispatchers.IO) {
            icon = try {
                context.packageManager.getApplicationIcon(appData.packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("TrafficItem", "Error loading icon for ${appData.packageName}", e)
                null
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            setImageDrawable(icon)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
            Column {
                Text(text = appData.appName, fontWeight = FontWeight.SemiBold)
                Text(text = "Total: ${(appData.totalBytes / 1024).toString()} Kb")
                Text(text = "Mobile: ${(appData.mobileBytes / 1024).toString()} Kb")
                Text(text = "Wi-Fi: ${(appData.wifiBytes / 1024).toString()} Kb")
                Text(text = "Downlink: ${(appData.rxBytes / 1024).toString()} Kb")
                Text(text = "Uplink: ${(appData.txBytes / 1024).toString()} Kb")
            }
        }
        Button(onClick = { onShowChart(appData.appName) }) {
            Text("Show Chart")
        }
    }
}