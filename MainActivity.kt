package com.example.novavideo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private val AppBg = Color(0xFF0F0F0F)
private val CardBg = Color(0xFF202020)
private val Accent = Color(0xFFFF3030)
private val TextMuted = Color(0xFFB8B8B8)

data class VideoSource(val title: String, val url: String, val source: String = "رابط مباشر")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(15, 15, 15)
        window.navigationBarColor = android.graphics.Color.rgb(15, 15, 15)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent,
                background = AppBg,
                surface = CardBg,
                onBackground = Color.White,
                onSurface = Color.White
            )) { NovaVideoApp() }
        }
    }
}

@Composable
private fun NovaVideoApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { VideoDatabase(context.applicationContext) }
    val videos = remember { mutableStateListOf<VideoSource>() }
    LaunchedEffect(database) { videos.clear(); videos.addAll(database.allVideos()) }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var sourceName by remember { mutableStateOf("رابط مباشر") }
    var searchQuery by remember { mutableStateOf("") }
    var current by remember { mutableStateOf<VideoSource?>(null) }
    var message by remember { mutableStateOf("أضف رابط MP4 أو HLS (M3U8) مباشر تملك حق تشغيله") }
    var showAddForm by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF181818)).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("▶", color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text("Nova Video", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("بدون إعلانات مضافة", color = TextMuted, fontSize = 11.sp)
        }

        if (current != null) {
            VideoPlayer(source = current!!, onError = { message = it })
            Text(current!!.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            Text(current!!.source, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Button(onClick = { current = null }, modifier = Modifier.padding(16.dp)) { Text("العودة إلى المكتبة") }
            Text(message, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showAddForm = !showAddForm }) { Text(if (showAddForm) "إخفاء الإضافة" else "إضافة فيديو") }
                Text("المكتبة · ${videos.size} فيديو", color = TextMuted, modifier = Modifier.align(Alignment.CenterVertically))
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("ابحث في مكتبتك") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
            if (showAddForm) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("اسم الفيديو") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("رابط الفيديو المباشر") }, placeholder = { Text("https://example.com/video.mp4") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sourceName, onValueChange = { sourceName = it }, label = { Text("اسم المصدر (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            val cleaned = url.trim()
                            val uri = runCatching { Uri.parse(cleaned) }.getOrNull()
                            if ((uri?.scheme == "https" || uri?.scheme == "http") && !uri.host.isNullOrBlank()) {
                                val item = VideoSource(title.trim().ifBlank { "فيديو ${videos.size + 1}" }, cleaned, sourceName.trim().ifBlank { "رابط مباشر" })
                                if (database.addVideo(item)) {
                                    videos.add(0, item)
                                    message = "تم حفظ الفيديو في قاعدة البيانات المحلية"
                                } else {
                                    message = "هذا الرابط موجود مسبقاً في المكتبة"
                                }
                                title = ""
                                url = ""
                                sourceName = "رابط مباشر"
                                message = "تمت إضافة الرابط إلى المكتبة"
                            } else {
                                message = "أدخل رابط HTTP أو HTTPS صالحاً لفيديو مباشر، وليس رابط صفحة YouTube"
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("إضافة إلى المكتبة") }
                        Text(message, color = TextMuted, fontSize = 12.sp)
                    }
                }
            }

            val filteredVideos = videos.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.url.contains(searchQuery, ignoreCase = true) ||
                    it.source.contains(searchQuery, ignoreCase = true)
            }
            if (filteredVideos.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("▶", color = Accent, fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(if (videos.isEmpty()) "مكتبتك فارغة" else "لا توجد نتائج", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(if (videos.isEmpty()) "أضف رابط فيديو مباشر للبدء" else "جرّب عبارة بحث أخرى", color = TextMuted, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp)) {
                    items(filteredVideos, key = { it.url }) { video ->
                        VideoRow(video = video, onClick = { current = video }, onDelete = { database.deleteVideo(video.url); videos.remove(video) })
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoRow(video: VideoSource, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 112.dp, height = 72.dp).background(Color(0xFF333333), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("▶", color = Accent, fontSize = 32.sp)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(video.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(video.url, color = TextMuted, fontSize = 10.sp, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text("اضغط للتشغيل", color = Accent, fontSize = 12.sp)
            }
            Text("×", color = TextMuted, fontSize = 24.sp, modifier = Modifier.clickable { onDelete() }.padding(6.dp))
        }
    }
}

@Composable
private fun VideoPlayer(source: VideoSource, onError: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(source.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(source.url))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    onError("تعذر تشغيل الفيديو: ${error.errorCodeName}. تحقق من الرابط وتوافق الصيغة.")
                }
            })
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
        update = { it.player = player },
        modifier = Modifier.fillMaxWidth().height(230.dp)
    )
}
