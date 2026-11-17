package com.subnook.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subnook.app.data.IconType
import com.subnook.app.data.PresetCatalog
import java.io.File
import java.util.UUID

object IconFiles {
    fun dir(context: Context) = File(context.filesDir, "icons").apply { mkdirs() }
    fun file(context: Context, name: String) = File(dir(context), name)

    @SuppressLint("DiscouragedApi")
    fun presetRes(context: Context, key: String): Int =
        context.resources.getIdentifier("preset_$key", "drawable", context.packageName)

    /** 把相册图片裁成正方形、缩放到 256px 后存入私有目录，返回文件名 */
    fun importImage(context: Context, uri: Uri): String {
        val src: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ ->
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
        }
        val side = minOf(src.width, src.height)
        val cropped = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(cropped, 256, 256, true)
        val name = "custom_${UUID.randomUUID()}.png"
        file(context, name).outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return name
    }
}

@Composable
fun SubIcon(
    iconType: IconType,
    iconValue: String,
    color: Int,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(size * 0.26f)
    val bg = Color(color)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), shape),
        contentAlignment = Alignment.Center,
    ) {
        when (iconType) {
            IconType.PRESET -> {
                val res = remember(iconValue) { IconFiles.presetRes(context, iconValue) }
                if (res != 0) {
                    Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    val mat = PresetCatalog.all.firstOrNull { it.key == iconValue }?.material
                    MaterialFill(mat ?: "", bg, size, name)
                }
            }
            IconType.CUSTOM -> {
                val bmp = remember(iconValue) {
                    runCatching { BitmapFactory.decodeFile(IconFiles.file(context, iconValue).path)?.asImageBitmap() }.getOrNull()
                }
                if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else LetterFill(name, bg, size)
            }
            IconType.MATERIAL -> MaterialFill(iconValue, bg, size, name)
            IconType.TEXT -> LetterFill(iconValue.ifBlank { name }, bg, size)
        }
    }
}

@Composable
private fun MaterialFill(iconName: String, bg: Color, size: Dp, fallback: String) {
    val vec = DesignIcons.byName(iconName)
    if (vec == null) {
        LetterFill(fallback, bg, size); return
    }
    Box(Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
        Icon(vec, null, tint = onColor(bg), modifier = Modifier.padding(size * 0.2f).fillMaxSize())
    }
}

@Composable
private fun LetterFill(text: String, bg: Color, size: Dp) {
    val letter = text.trim().take(if (text.trim().firstOrNull()?.let { it.code > 0x2E80 } == true) 1 else 2)
        .uppercase().ifBlank { "?" }
    Box(Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
        Text(
            letter,
            color = onColor(bg),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * if (letter.length > 1) 0.36f else 0.46f).sp,
        )
    }
}

fun onColor(bg: Color): Color {
    val l = 0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue
    return if (l > 0.65) Color(0xFF1C1B1F) else Color.White
}
