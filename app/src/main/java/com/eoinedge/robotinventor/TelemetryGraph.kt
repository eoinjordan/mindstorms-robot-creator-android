package com.eoinedge.robotinventor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun TelemetryGraph(
    title: String,
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
    minY: Float = -2f,
    maxY: Float = 2f
) {
    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.labelSmall)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(vertical = 4.dp)
        ) {
            val width = size.width
            val height = size.height
            
            // Draw axis
            drawLine(
                color = Color.Gray,
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2),
                strokeWidth = 1f
            )

            if (data.size > 1) {
                val path = Path()
                val stepX = width / (data.size - 1)
                val rangeY = maxY - minY
                
                data.forEachIndexed { index, value ->
                    val x = index * stepX
                    val normalizedValue = (value - minY) / rangeY
                    val y = height - (normalizedValue * height)
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
