// BoardConverters.kt
package com.example.to_dolist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb  // <-- REQUIRED for toArgb()

data class SerializableColor(val argb: Int)

fun Color.toSerializable(): SerializableColor = SerializableColor(this.toArgb())

fun SerializableColor.toComposeColor(): Color = Color(this.argb)
