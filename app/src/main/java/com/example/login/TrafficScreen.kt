package com.example.login

import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.DatePicker
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.work.WorkManager
import com.example.login.MainActivity.FileUploadScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun TrafficScreen(state: MainActivity.MainActivityState, onNavigateTo: (Int) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
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
    var sortCriteria by remember { mutableStateOf<SortCriteria>(SortCriteria.TOTAL) }
    var isLoading by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()

    var showSlider by remember { mutableStateOf(false) }
    val intervalOptions = listOf(1, 1800, 3600, 21600, 43200, 86400)
    var selectedInterval by remember { mutableStateOf(intervalOptions[0]) }

    fun onDaysChanged(newDays: String) {
        days = newDays
        activeMode = "days"
        selectedCalendar = null
    }
    val shimmerColors = if (isDarkTheme) {
        listOf(
            Color(0xFF3C3C3E),
            Color(0xFF4A4A4C),
            Color(0xFF5A5A5C)
        )
    } else {
        listOf(
            Color(0xFFE5E4E4),
            Color(0xFFF5F4F4),
            Color(0xFFE0E0E0)
        )
    }

    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1150,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 100f, translateAnim - 100f),
        end = Offset(translateAnim, translateAnim)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF8F8F8))
            .border(width = 2.dp, color = if (isDarkTheme) Color(0x4D9E9E9E) else Color(0x809E9E9E), shape = RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onNavigateTo(5) }) {
                Image(
                    painter = painterResource(id = if (isDarkTheme) R.drawable.transition_dark else R.drawable.transition_light),
                    contentDescription = "back",
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.traffic),
                color = if (isDarkTheme) Color.White else Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showSlider = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (isDarkTheme)  Color(0xFFFFFFFF) else Color(0xFF3C3C3E)
                )
            }
        }
    }

    fun formatInterval(intervalInSeconds: Int): String {
        return when (intervalInSeconds) {
            1 -> "1 секунда"
            1800 -> "30 минут"
            3600 -> "1 час"
            21600 -> "6 часов"
            43200 -> "12 часов"
            86400 -> "24 часа"
            else -> "$intervalInSeconds секунд"
        }
    }

    if (showSlider) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color.Black.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.6f))
                .clickable(
                    onClick = { showSlider = false },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 32.dp)
                    .background(if (isDarkTheme) Color(0xFF3C3C3E) else  Color(0xFFFFFFFF), shape = RoundedCornerShape(10.dp))
                    .padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Интервал\nмежду отправкой данных",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 50.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { showSlider = false },
                        modifier = Modifier
                            .size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Интервал: ${formatInterval(selectedInterval)}",
                    fontSize = 16.sp,
                    color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = intervalOptions.indexOf(selectedInterval).toFloat(),
                    onValueChange = { index ->
                        selectedInterval = intervalOptions[index.toInt()]
                    },
                    valueRange = 0f..(intervalOptions.size - 1).toFloat(),
                    steps = intervalOptions.size - 2,
                    colors = SliderDefaults.colors(
                        thumbColor = if (isDarkTheme) Color(0xCC567BFF) else Color(0xFF132C86),
                        activeTrackColor = if (isDarkTheme) Color(0xCC567BFF) else Color(0xB3132C86),
                        inactiveTrackColor = if (isDarkTheme) Color(0xB31C40C6) else Color(0xCC567BFF)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        WorkManager.getInstance(context).cancelAllWork()
                        FileUploadScheduler.scheduleFileUpload(selectedInterval.toLong(), context)
                        showSlider = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Color(0xFF3C3C3E) else  Color(0xFFFFFFFF)
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .border(1.dp, color = if (isDarkTheme) Color(0x809E9E9E) else Color(0x4D9E9E9E), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "Сохранить изменения",
                        color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
                    )
                }
            }
        }
    }

    LaunchedEffect(days, selectedCalendar, activeMode, sortCriteria) {
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                if (activeMode == "calendar" && selectedCalendar != null) {
                    val daysList = listOf(
                        TimeUnit.MILLISECONDS.toDays(
                            Calendar.getInstance().timeInMillis - selectedCalendar!!.timeInMillis
                        ).toInt()
                    )
                    appTrafficData.value = when (sortCriteria) {
                        SortCriteria.TOTAL -> getAppTrafficDataForDays(context, daysList).sortedByDescending { it.totalBytes }
                        SortCriteria.MOBILE -> getAppTrafficDataForDays(context, daysList).sortedByDescending { it.mobileBytes }
                        SortCriteria.WIFI -> getAppTrafficDataForDays(context, daysList).sortedByDescending { it.wifiBytes }
                    }
                    totalTrafficData.value = getTotalTrafficDataForDays(context, daysList)
                } else if (activeMode == "days") {
                    val daysList = days.split(",").mapNotNull { it.toIntOrNull() }
                    if (daysList.isNotEmpty()) {
                        showError = false
                        appTrafficData.value = when (sortCriteria) {
                            SortCriteria.TOTAL -> getAppTrafficData(context, daysList.sum()).sortedByDescending { it.totalBytes }
                            SortCriteria.MOBILE -> getAppTrafficData(context, daysList.sum()).sortedByDescending { it.mobileBytes }
                            SortCriteria.WIFI -> getAppTrafficData(context, daysList.sum()).sortedByDescending { it.wifiBytes }
                        }
                        totalTrafficData.value = getTotalTrafficData(context, daysList.sum())
                    } else {
                        showError = true
                    }
                }
            } catch (e: Exception) {
                Log.e(MainActivity.TAG, "Error fetching app traffic data: ${e.message}", e)
                showError = true
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color.Black else Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isDarkTheme) Color(0xFF3C3C3E) else Color.White,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.5.dp,
                                if (sortCriteria == SortCriteria.TOTAL) {
                                    if (isDarkTheme) Color(0xCC567BFF) else Color(0xFF132C86)
                                } else {
                                    if (isDarkTheme) Color(0x809E9E9E) else Color(0x4D9E9E9E)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { sortCriteria = SortCriteria.TOTAL }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.total),
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C)
                        )
                        Text(text = "${(totalTrafficData.value.totalBytes / 1024)} Kb", color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isDarkTheme) Color(0xFF3C3C3E) else Color.White,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.5.dp,
                                if (sortCriteria == SortCriteria.MOBILE) {
                                    if (isDarkTheme) Color(0xCC567BFF) else Color(0xFF132C86)
                                } else {
                                    if (isDarkTheme) Color(0x809E9E9E) else Color(0x4D9E9E9E)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { sortCriteria = SortCriteria.MOBILE }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.mobile),
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C)
                        )
                        Text(text = "${(totalTrafficData.value.mobileBytes / 1024)} Kb", color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isDarkTheme) Color(0xFF3C3C3E) else Color.White,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.5.dp,
                                if (sortCriteria == SortCriteria.WIFI) {
                                    if (isDarkTheme) Color(0xCC567BFF) else Color(0xFF132C86)
                                } else {
                                    if (isDarkTheme) Color(0x809E9E9E) else Color(0x4D9E9E9E)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { sortCriteria = SortCriteria.WIFI }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.wifi),
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C)
                        )
                        Text(text = "${(totalTrafficData.value.wifiBytes / 1024)} Kb", color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C))
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = days,
                            onValueChange = { onDaysChanged(it) },
                            label = {
                                Text(
                                    stringResource(id = R.string.days_count),
                                    color = if (isDarkTheme) Color(0xD9FFFFFF) else Color(0xFF929292)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
                                unfocusedTextColor = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
                                focusedBorderColor = if (isDarkTheme) Color(0xCC567BFF) else Color(0xFF132C86),
                                unfocusedBorderColor = if (isDarkTheme) Color(0x809E9E9E) else Color(0x4D9E9E9E)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .offset(y = 4.dp)
                            .background(
                                if (isDarkTheme) Color(0xCC567BFF) else Color(0xFF132C86),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        IconButton(
                            onClick = {
                                showDatePicker = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .align(Alignment.Center)
                                .zIndex(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select date",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            item {
                if (showError) {
                    Text(
                        text = stringResource(id = R.string.incorrect_days_count),
                        color = Color(0xCCAF1027),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item {
                if (!PermissionUtils.hasUsageStatsPermission(context)) {
                    Button(
                        onClick = { PermissionUtils.requestUsageStatsPermission(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDarkTheme) Color(0xCC567BFF) else Color(0xFF132C86)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.grant_permission),
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { showTotalChart = true },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDarkTheme) Color(0xCC567BFF) else Color(0xFF132C86)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .wrapContentWidth()
                                .wrapContentHeight()
                                .padding(2.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.total_traffic),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSendingTrafficData = true

                                    val authResponse = try {
                                        MainActivity.networkManager.authenticateForTraffic(state.Email, state.Password).await()
                                    } catch (e: Exception) {
                                        Log.e(MainActivity.TAG, "Authentication failed: ${e.message}", e)
                                        null
                                    }

                                    if (authResponse != null) {
                                        val sortedApps = appTrafficData.value.sortedByDescending { it.totalBytes }
                                        val top10Apps = sortedApps.take(10)
                                        try {
                                            MainActivity.networkManager.sendTrafficDataToServer(authResponse.jwt, top10Apps)
                                            Log.d(MainActivity.TAG, "Traffic data sent successfully!")
                                        } catch (e: Exception) {
                                            Log.e(MainActivity.TAG, "Failed to send traffic data: ${e.message}", e)
                                        }
                                    } else {
                                        Log.e(MainActivity.TAG, "Authentication for traffic data failed")
                                    }
                                    isSendingTrafficData = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDarkTheme) Color(0xCC567BFF) else Color(0xFF132C86)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .wrapContentWidth()
                                .wrapContentHeight()
                                .padding(2.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.send_data),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                items(10) {
                    ShimmerTrafficItem(brush = shimmerBrush)
                }
            } else {
                items(appTrafficData.value, key = { it.appName }) { appData ->
                    TrafficItem(appData) { appName ->
                        selectedAppName = appName
                        showAppChart = true
                    }
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

            LaunchedEffect(key1 = showDatePicker) {
                if (showDatePicker) {
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
                        selectedCalendar = null
                    }
                    datePickerDialog.show()
                }
            }
        }
    }
}

@Composable
fun ShimmerTrafficItem(brush: Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(brush, shape = RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.width(200.dp)) {
                ShimmerText(width = 120.dp, height = 16.sp, brush = brush)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerText(width = 80.dp, height = 14.sp, brush = brush)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerText(width = 80.dp, height = 14.sp, brush = brush)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerText(width = 80.dp, height = 14.sp, brush = brush)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerText(width = 80.dp, height = 14.sp, brush = brush)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerText(width = 80.dp, height = 14.sp, brush = brush)
            }
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(brush, shape = CircleShape)
        )
    }
}

@Composable
fun ShimmerText(width: Dp, height: TextUnit, brush: Brush) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height.value.dp)
            .background(brush)
    )
}

@Composable
fun TrafficItem(appData: AppTrafficData, onShowChart: (String) -> Unit) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<Drawable?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isSelected by remember { mutableStateOf(false) }
    var isHighlighted by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()

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

    LaunchedEffect(isSelected) {
        if (isSelected) {
            isHighlighted = true
            delay(2000)
            isHighlighted = false
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

            Column(modifier = Modifier.width(200.dp)) {
                val textColor = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xB334204C)
                Text(
                    text = appData.appName,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = "${stringResource(id = R.string.total)} ${(appData.totalBytes / 1024)} Kb", color = textColor)
                Text(text = "${stringResource(id = R.string.mobile)} ${(appData.mobileBytes / 1024)} Kb", color = textColor)
                Text(text = "${stringResource(id = R.string.wifi)} ${(appData.wifiBytes / 1024)} Kb", color = textColor)
                Text(text = "${stringResource(id = R.string.downlink)} ${(appData.rxBytes / 1024)} Kb", color = textColor)
                Text(text = "${stringResource(id = R.string.uplink)} ${(appData.txBytes / 1024)} Kb", color = textColor)
            }
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    if (isHighlighted) {
                        if (isDarkTheme) Color(0xCC567BFF) else Color(0xFF132C86)
                    } else {
                        if (isDarkTheme) Color(0xFF828282) else Color(0x809E9E9E)
                    },
                    shape = CircleShape
                )
                .clickable {
                    isHighlighted = !isHighlighted
                    isSelected = !isSelected
                    onShowChart(appData.appName)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = "Show graph",
                tint = if (isHighlighted) Color.White else Color.Transparent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
enum class SortCriteria {
    TOTAL, MOBILE, WIFI
}