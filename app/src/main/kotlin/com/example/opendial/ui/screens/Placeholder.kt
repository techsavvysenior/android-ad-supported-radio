package com.example.opendial.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape

/** A flat skeleton block in the surface color, used while lists load and behind error messages. */
@Composable
fun Modifier.placeholder(shape: Shape): Modifier =
    this.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
