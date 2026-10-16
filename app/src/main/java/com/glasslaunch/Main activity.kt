package com.glasslaunch

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

private val Ink = Color(0xFF07172C)
private val Ice = Color(0xFFEAF7FF)
private val GlassStroke = Color.White.copy(alpha = .34f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        setContent { MaterialTheme { GlassHome() } }
    }
}

data class LaunchApp(val label: String, val packageName: String, val icon: ImageBitmap)

@Composable
private fun GlassHome() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    var drawer by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedPage by remember { mutableIntStateOf(0) }
    val apps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { ri ->
                val ai = ri.activityInfo ?: return@mapNotNull null
                if (ai.packageName == context.packageName) return@mapNotNull null
                try { LaunchApp(ri.loadLabel(pm).toString(), ai.packageName, drawableToBitmap(ri.loadIcon(pm))) } catch (_: Exception) { null }
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(30_000) } }
    val time = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(now).replaceFirstChar { it.uppercase() } }
    val filtered = apps.filter { it.label.contains(query, true) }

    Box(Modifier.fillMaxSize().background(Ink)) {
        LiquidBackdrop()
        if (!drawer) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(time, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light, letterSpacing = (-1.8).sp)
                        Text(date, color = Color.White.copy(.84f), fontSize = 13.sp)
                    }
                    GlassIconButton(Icons.Default.Settings, "Configurações") {
                        context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    GlassWidget(Modifier.weight(1f).height(116.dp), "CLIMA", "☀", "24°", "Parcialmente nublado")
                    GlassWidget(Modifier.weight(1f).height(116.dp), "CALENDÁRIO", "${now.date}", "Hoje", date.substringBefore(","))
                }
                Spacer(Modifier.height(22.dp))
                GlassSearch(value = query, onValueChange = { query = it }, onClick = { drawer = true })
                Spacer(Modifier.height(22.dp))
                val favoriteNames = listOf("Telefone", "Mensagens", "Câmera", "Fotos", "Configurações", "Play Store", "Chrome", "YouTube")
                val favorites = favoriteNames.mapNotNull { name -> apps.firstOrNull { it.label.equals(name, true) || it.label.contains(name, true) } }.distinctBy { it.packageName }.take(8)
                if (favorites.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        favorites.chunked(4).forEach { rowApps ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                rowApps.forEach { app -> AppIcon(app, Modifier.weight(1f)) { launchApp(context, app) } }
                                repeat((4 - rowApps.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(Icons.Default.CameraAlt to "Câmera", Icons.Default.CalendarMonth to "Calendário", Icons.Default.FolderOpen to "Arquivos", Icons.Default.Settings to "Ajustes").forEach { (icon, label) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { if (label == "Ajustes") context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }) {
                                GlassIconTile(icon, Modifier.size(58.dp)); Spacer(Modifier.height(7.dp)); Text(label, color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { i -> Box(Modifier.size(if (i == selectedPage) 7.dp else 5.dp).clip(CircleShape).background(if (i == selectedPage) Color.White else Color.White.copy(.42f))) }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(Color.White.copy(.24f), Color.White.copy(.09f), Color.White.copy(.16f)))).border(1.dp, GlassStroke, RoundedCornerShape(30.dp)).padding(horizontal = 13.dp, vertical = 13.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    val dockApps = favorites.take(4)
                    if (dockApps.isNotEmpty()) dockApps.forEach { AppIcon(it, Modifier.weight(1f), showLabel = false) { launchApp(context, it) } }
                    else listOf(Icons.Default.WbSunny, Icons.Default.CalendarMonth, Icons.Default.CameraAlt, Icons.Default.FolderOpen).forEach { GlassIconButton(it, "Abrir aplicativos") { drawer = true } }
                }
                Spacer(Modifier.height(8.dp))
                Text("Deslize para cima para ver os apps", color = Color.White.copy(.58f), fontSize = 10.sp, modifier = Modifier.clickable { drawer = true })
                Spacer(Modifier.height(6.dp))
            }
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onLongPress = { drawer = true }) })
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Biblioteca", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
                    GlassIconButton(Icons.Default.Close, "Fechar biblioteca") { drawer = false; query = "" }
                }
                Spacer(Modifier.height(18.dp))
                GlassSearch(query, { query = it }, onClick = {})
                Spacer(Modifier.height(16.dp))
                Text("APLICATIVOS", color = Color.White.copy(.66f), fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(filtered, key = { it.packageName }) { app -> AppIcon(app, Modifier.fillMaxWidth()) { launchApp(context, app) } }
                }
            }
        }
    }
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): ImageBitmap {
    val w = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(192)
    val h = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(192)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
}

private fun launchApp(context: android.content.Context, app: LaunchApp) {
    try { context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) } } catch (_: Exception) { }
}

@Composable
private fun LiquidBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF0B2446), Color(0xFF17618A), Color(0xFF0B1837), Color(0xFF050B1B)), start = Offset(0f, 0f), end = Offset(size.width, size.height)))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF78D7FF).copy(.46f), Color(0xFF5AB8FF).copy(.14f), Color.Transparent), center = Offset(size.width * .18f, size.height * .24f), radius = size.width * .78f), radius = size.width * .78f, center = Offset(size.width * .18f, size.height * .24f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF9EACFF).copy(.32f), Color(0xFF5369D6).copy(.10f), Color.Transparent), center = Offset(size.width * .88f, size.height * .60f), radius = size.width * .8f), radius = size.width * .8f, center = Offset(size.width * .88f, size.height * .60f))
        val wave = Path().apply {
            moveTo(0f, size.height * .54f)
            cubicTo(size.width * .28f, size.height * .38f, size.width * .46f, size.height * .70f, size.width, size.height * .47f)
            lineTo(size.width, size.height * .72f)
            cubicTo(size.width * .56f, size.height * .90f, size.width * .24f, size.height * .64f, 0f, size.height * .82f)
            close()
        }
        drawPath(wave, Brush.linearGradient(listOf(Color(0xFFB3EDFF).copy(.13f), Color(0xFF4C8EFF).copy(.04f), Color(0xFFB8C8FF).copy(.10f))))
        drawPath(wave, Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(.22f), Color.Transparent)), style = Stroke(width = 1.2.dp.toPx()))
        // Fine specular streaks suggest glass without an expensive full-screen blur.
        repeat(7) { i ->
            val x = size.width * (i / 7f)
            drawLine(Color.White.copy(alpha = .025f), Offset(x, 0f), Offset(x + size.width * .18f, size.height), strokeWidth = 1.dp.toPx())
        }
    }
}

@Composable
private fun GlassWidget(modifier: Modifier, title: String, symbol: String, value: String, subtitle: String) {
    Column(modifier.clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Color.White.copy(.25f), Color.White.copy(.09f), Color.White.copy(.16f)))).border(1.dp, GlassStroke, RoundedCornerShape(26.dp)).padding(14.dp)) {
        Text(title, color = Color.White.copy(.7f), fontSize = 9.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, color = Color.White, fontSize = if (symbol.length > 2) 23.sp else 26.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.width(8.dp))
            Text(value, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = Color.White.copy(.78f), fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun GlassSearch(value: String, onValueChange: (String) -> Unit, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color.White.copy(.25f), Color.White.copy(.10f)))).border(1.dp, GlassStroke, CircleShape).clickable { onClick() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Search, null, tint = Color.White.copy(.9f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Buscar", color = Color.White.copy(.72f), fontSize = 14.sp)
            BasicTextField(value, onValueChange, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp), cursorBrush = Brush.verticalGradient(listOf(Color.White, Color.White)), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AppIcon(app: LaunchApp, modifier: Modifier = Modifier, showLabel: Boolean = true, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) .88f else 1f, animationSpec = tween(180, easing = FastOutSlowInEasing), label = "iconScale")
    Column(modifier.scale(scale).clickable { pressed = true; onClick(); }.padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(Color.White.copy(.28f), Color.White.copy(.06f)))).border(1.dp, Color.White.copy(.24f), RoundedCornerShape(17.dp)).padding(2.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)))
            Canvas(Modifier.fillMaxSize()) { drawLine(Color.White.copy(.34f), Offset(size.width * .18f, size.height * .04f), Offset(size.width * .72f, size.height * .04f), strokeWidth = 1.dp.toPx()) }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(app.label, color = Color.White.copy(.96f), fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(190); pressed = false } }
}

@Composable
private fun GlassIconTile(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color.White.copy(.30f), Color.White.copy(.10f)))).border(1.dp, GlassStroke, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun GlassIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(43.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color.White.copy(.25f), Color.White.copy(.08f)))).border(1.dp, GlassStroke, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(21.dp))
    }
}
