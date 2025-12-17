package com.zscaler.sdk.demoapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.zscaler.sdk.demoapp.R

@Composable
fun ZDKTestAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val lightColorScheme = lightColorScheme(
        primary = colorResource(R.color.light_primary),
        onPrimary = colorResource(R.color.light_on_primary),
        secondary = colorResource(R.color.light_secondary),
        onSecondary = colorResource(R.color.light_on_secondary),
        background = colorResource(R.color.light_background),
        surface = colorResource(R.color.light_surface),
        onSurface = colorResource(R.color.light_on_surface),
        error = colorResource(R.color.light_error)
    )

    val darkColorScheme = darkColorScheme(
        primary = colorResource(R.color.dark_primary),
        onPrimary = colorResource(R.color.dark_on_primary),
        secondary = colorResource(R.color.dark_secondary),
        onSecondary = colorResource(R.color.dark_on_secondary),
        background = colorResource(R.color.dark_background),
        surface = colorResource(R.color.dark_surface),
        onSurface = colorResource(R.color.dark_on_surface),
        error = colorResource(R.color.dark_error)
    )

    val colorScheme = if (darkTheme) darkColorScheme else lightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
