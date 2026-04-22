package com.rbt.survey.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.rbt.survey.data.model.GpItem
import com.rbt.survey.data.model.GpStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    formId: Int,
    blockCode: String?,
    gpStatusList: List<GpItem>,
    onBack: () -> Unit,
    onMarkerClick: (Int, String?,String?) -> Unit
) {

    val gpList by viewModel.gpList.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(20.5937, 78.9629),
            4f
        )
    }

    // Load data
    LaunchedEffect(formId, blockCode) {
        viewModel.setCompletedGpList(gpStatusList)
        viewModel.loadGpLocations(formId, blockCode)
    }

    // Move camera to first GP
    LaunchedEffect(gpList) {
        if (gpList.isNotEmpty()) {
            val first = gpList.first()
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(
                    LatLng(first.lat, first.lng),
                    10f
                )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GP Points") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        val context = androidx.compose.ui.platform.LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ✅ MAP
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {

                gpList.forEach { gp ->

                    val markerBitmap = createLabeledSquareMarker(
                        text = gp.name,
                        squareColor = if (gp.isCompleted)
                            android.graphics.Color.GREEN
                        else
                            android.graphics.Color.RED,
                        squareSize = 30,
                        textSizeSp = 12f,
                        textColor = android.graphics.Color.RED,
                        context = context
                    )

                    Marker(
                        state = MarkerState(
                            position = LatLng(gp.lat, gp.lng)
                        ),
                        icon = BitmapDescriptorFactory.fromBitmap(markerBitmap),
                        onClick = {
                            if (gp.isCompleted) {
                                // 🟢 Completed → show message
                                Toast.makeText(
                                    context,
                                    "You already filled the form for this GP",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // 🔴 Not completed → navigate
                                onMarkerClick(formId, blockCode, gp.name)
                            }

                            true
                        }
                    )
                }
            }

            // ✅ LOADING ON TOP OF MAP
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

}

fun createLabeledSquareMarker(
    text: String,
    squareColor: Int,
    squareSize: Int,
    textSizeSp: Float,
    textColor: Int,
    context: android.content.Context
): android.graphics.Bitmap {

    val density = context.resources.displayMetrics

    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            textSizeSp,
            density
        )
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val textBounds = android.graphics.Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)

    val textWidth = textPaint.measureText(text)
    val textHeight = textBounds.height().toFloat()

    val horizontalPadding = 20
    val verticalPadding = 10

    val bitmapWidth = (textWidth + horizontalPadding * 2).toInt()
    val bitmapHeight = (textHeight + verticalPadding + squareSize + verticalPadding).toInt()

    val bitmap = android.graphics.Bitmap.createBitmap(
        bitmapWidth,
        bitmapHeight,
        android.graphics.Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(bitmap)

    // Draw TEXT (top)
    val textX = bitmapWidth / 2f
    val textY = textHeight + verticalPadding
    canvas.drawText(text, textX, textY, textPaint)

    // Draw RED SQUARE (below text)
    val squarePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = squareColor
    }

    val squareLeft = (bitmapWidth - squareSize) / 2f
    val squareTop = textY + verticalPadding
    val squareRight = squareLeft + squareSize
    val squareBottom = squareTop + squareSize

    canvas.drawRect(squareLeft, squareTop, squareRight, squareBottom, squarePaint)

    return bitmap
}
