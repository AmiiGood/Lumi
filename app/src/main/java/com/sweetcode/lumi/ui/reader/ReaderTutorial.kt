package com.sweetcode.lumi.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ReaderTutorial(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss)
    ) {
        // Zonas visuales: izquierda, centro, derecha
        Row(modifier = Modifier.fillMaxSize()) {
            ZoneIndicator(
                label = "Anterior",
                icon = Icons.Rounded.SwipeLeft,
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            )
            ZoneIndicator(
                label = "Menú",
                icon = Icons.Rounded.TouchApp,
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
            )
            ZoneIndicator(
                label = "Siguiente",
                icon = Icons.Rounded.SwipeLeft,
                flipIcon = true,
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Cómo leer",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            InstructionRow("• Toca los bordes laterales para cambiar de página")
            InstructionRow("• Desliza horizontal también funciona")
            InstructionRow("• Toca el centro para ver el menú")
            InstructionRow("• Pellizca o doble tap para hacer zoom")
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Entendido")
            }
        }
    }
}

@Composable
private fun ZoneIndicator(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    flipIcon: Boolean = false
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = if (flipIcon) Modifier
                        .size(28.dp)
                        .padding(0.dp) else Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InstructionRow(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}