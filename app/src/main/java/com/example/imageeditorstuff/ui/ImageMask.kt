package com.example.imageeditorstuff.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.ImagePainter
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImageMask(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imgPicked = remember {
        mutableStateOf<Bitmap?>(null)
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imgPicked.value = uriToBitmap(context, uri)
        }
    }
    val inputText = remember {
        mutableStateOf("")
    }
    val picture = remember { android.graphics.Picture() }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        capturedBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured Image",
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                capturedBitmap = null
            }) {
                Text(text = "Try Again")
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .drawWithCache {
                        val width = this.size.width.toInt()
                        val height = this.size.height.toInt()
                        onDrawWithContent {
                            val pictureCanvas =
                                androidx.compose.ui.graphics.Canvas(
                                    picture.beginRecording(
                                        width,
                                        height
                                    )
                                )
                            draw(this, this.layoutDirection, pictureCanvas, this.size) {
                                this@onDrawWithContent.drawContent()
                            }
                            picture.endRecording()

                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawPicture(picture)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                var position by remember { mutableStateOf(Offset(0f, 0f)) }
                imgPicked.value?.let { uri ->
                    Image(
                        bitmap = uri.asImageBitmap(),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxSize()
                    )
                    Box(modifier = Modifier.padding(16.dp).pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            position = Offset(
                                x = position.x + dragAmount.x,
                                y = position.y + dragAmount.y
                            )
                        }
                    }) {
                        TextField(
                            value = inputText.value,
                            onValueChange = {
                                inputText.value = it
                            }
                        )
                    }
                } ?: Text("Tap the button to pick an image")
            }

            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text(text = "Pick Image")
            }
            Button(onClick = {
                val bitmapOfMask = createBitmapFromPicture(picture)
                bitmapOfMask.saveBitmapToGallery()
                capturedBitmap = createBitmapFromPicture(picture)
            }) {
                Text(text = "Save Image")
            }
        }
    }
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
}

private fun createBitmapFromPicture(picture: Picture): Bitmap {
    val bitmap = Bitmap.createBitmap(
        picture.width,
        picture.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    canvas.drawPicture(picture)
    return bitmap
}

fun Bitmap.saveBitmapToGallery() {
    val directory = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "ComposeCaptures"
    )
    if (!directory.exists()) directory.mkdirs()

    val file = File(directory, "captured_image_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        this.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

@Preview
@Composable
private fun ImgMaskPreview(modifier: Modifier = Modifier) {
    ImageMask()
}
