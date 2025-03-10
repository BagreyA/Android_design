package com.example.login

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.util.Locale

@Composable
fun TopPanel(onNavigateTo: (Int, String?, String?) -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()

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
            IconButton(onClick = { onNavigateTo(5, null, null) }) {
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
                text = stringResource(R.string.settings_title),
                color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun SettingsScreen(
    state: MainActivity.MainActivityState,
    onNavigateTo: (Int, String?, String?) -> Unit,
) {
    var isLanguageMenuVisible by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf("ru") }

    val isDarkTheme = isSystemInDarkTheme()

    var isDarkMode by remember { mutableStateOf(isDarkTheme) }

    val context = LocalContext.current
    val rootManager = remember { RootManager(context) }
    var isRootModeEnabled by remember { mutableStateOf(rootManager.isRootModeEnabled()) }


    TopPanel(onNavigateTo = onNavigateTo)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFFFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SettingItemWithArrow(
                label = stringResource(R.string.interface_language),
                onNavigateTo = { isLanguageMenuVisible = !isLanguageMenuVisible },
                iconRes = if (isDarkMode) R.drawable.language_d else R.drawable.language
            )
            if (isLanguageMenuVisible) {
                LanguageDropdownMenu(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { selectedLanguage ->
                        currentLanguage = selectedLanguage
                        setLocale(context, selectedLanguage)
                        (context as? Activity)?.recreate()
                        isLanguageMenuVisible = false
                    }
                )
            }

            SettingItemWithArrow(
                label = stringResource(R.string.sending_collected_data),
                onNavigateTo = { onNavigateTo(7, state.Uuid, state.JwtToken) },
                iconRes = if (isDarkMode) R.drawable.s_data_d else R.drawable.s_data
            )

            // TODO заменить иконку, переместить текст в strings

            SettingItemWithSwitch(
                label = "Root Mode",
                isChecked = isRootModeEnabled,
                onCheckedChange = { enabled ->
                    isRootModeEnabled = enabled
                    rootManager.setRootModeEnabled(enabled)
                    if (enabled) {
                        rootManager.requestRootAccess()
                    }
                },
                iconRes = if (isDarkMode) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun LanguageDropdownMenu(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    val languages = listOf(
        LanguageItem("ru", "Русский"),
        LanguageItem("en", "English")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(-1f)
    ) {

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = (-15).dp)
        ) {
            Column(
                modifier = Modifier
                    .size(100.dp)
                    .background(if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF8F8F8))
                    .border(
                        1.dp,
                        if (isDarkTheme) Color(0x4D9E9E9E) else Color(0x809E9E9E),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                languages.forEach { language ->
                    LanguageOption(
                        language = language.code,
                        label = language.name,
                        isSelected = currentLanguage == language.code,
                        onSelect = { onLanguageSelected(language.code) }
                    )
                }
            }
        }
    }
}

data class LanguageItem(val code: String, val name: String)

@Composable
fun LanguageOption(
    language: String,
    label: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onSelect(language) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
        )
    }
}

fun setLocale(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)

    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

@Composable
fun SettingItemWithArrow(label: String, onNavigateTo: () -> Unit, iconRes: Int) {
    val isDarkTheme = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF8F8F8))
            .border(
                width = 1.5.dp,
                color = if (isDarkTheme) Color(0x4D9E9E9E) else Color(0x809E9E9E),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
            .clickable { onNavigateTo() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        Icon(
            painter = painterResource(id = R.drawable.arrow),
            contentDescription = "Перейти",
            tint = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
            modifier = Modifier.size(24.dp)
        )
    }
}


@Composable
fun SettingItemWithSwitch(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconRes: Int
) {
    val isDarkTheme = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF8F8F8))
            .border(
                width = 1.5.dp,
                color = if (isDarkTheme) Color(0x4D9E9E9E) else Color(0x809E9E9E),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = if (isDarkTheme) Color(0xCCFFFFFF) else Color(0xFF34204C),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isDarkTheme) Color.White else Color(0xFF34204C),
                checkedTrackColor = if (isDarkTheme) Color(0xFF888888) else Color(0xFF8EC3F8),
                uncheckedThumbColor = if (isDarkTheme) Color.Gray else Color.LightGray,
                uncheckedTrackColor = if (isDarkTheme) Color(0xFF444444) else Color.Gray,
            )
        )
    }
}