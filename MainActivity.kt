package com.auratune.player

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auratune.player.data.AuraTrack
import com.auratune.player.data.StreamProviders
import com.auratune.player.service.AuraPlaybackService
import com.auratune.player.ui.AuraTuneMaterial3Theme
import com.google.common.util.concurrent.MoreExecutors

class MainActivity : ComponentActivity() {

    private var mediaController by mutableStateOf<MediaController?>(null)

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, AuraPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
            { mediaController = controllerFuture.get() },
            MoreExecutors.directExecutor()
        )
    }

    override fun onStop() {
        mediaController?.release()
        mediaController = null
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var useSystemMonet by remember { mutableStateOf(true) }
            var useSage by remember { mutableStateOf(false) }
            var amoledMode by remember { mutableStateOf(false) }

            AuraTuneMaterial3Theme(
                useSystemMonet = useSystemMonet,
                useSagePalette = useSage,
                amoledBlack = amoledMode
            ) {
                AuraMainApp(
                    controller = mediaController,
                    useSystemMonet = useSystemMonet,
                    onToggleMonet = { useSystemMonet = !useSystemMonet },
                    useSage = useSage,
                    onToggleSage = { useSage = !useSage },
                    amoledMode = amoledMode,
                    onToggleAmoled = { amoledMode = !amoledMode }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraMainApp(
    controller: MediaController?,
    useSystemMonet: Boolean,
    onToggleMonet: () -> Unit,
    useSage: Boolean,
    onToggleSage: () -> Unit,
    amoledMode: Boolean,
    onToggleAmoled: () -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var currentTrack by remember { mutableStateOf(StreamProviders.defaultCatalog.first()) }
    var isPlaying by remember { mutableStateOf(false) }

    // 5 полос аппаратного эквалайзера: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val eqBands = remember { mutableStateListOf(3f, 1.5f, 0f, 2f, 3.5f) }
    val eqFreqLabels = listOf("60 Гц", "230 Гц", "910 Гц", "3.6 кГц", "14 кГц")

    fun playTrack(track: AuraTrack) {
        currentTrack = track
        isPlaying = true
        val item = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .build()
            )
            .build()
        controller?.setMediaItem(item)
        controller?.prepare()
        controller?.play()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AuraTune", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                        Text(
                            "Material You • Media3 ExoPlayer",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilterChip(
                        selected = useSystemMonet,
                        onClick = onToggleMonet,
                        label = { Text("Monet") },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    FilterChip(
                        selected = amoledMode,
                        onClick = onToggleAmoled,
                        label = { Text("OLED") },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column {
                // Мягкий плавающий мини-плеер в стиле ArchiveTune / SimpMusic
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Album, contentDescription = null)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                currentTrack.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                "${currentTrack.artist} • ${currentTrack.codecBadge}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                            )
                        }
                        FilledTonalIconButton(
                            onClick = {
                                if (isPlaying) {
                                    controller?.pause()
                                    isPlaying = false
                                } else {
                                    playTrack(currentTrack)
                                }
                            }
                        ) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause"
                            )
                        }
                    }
                }

                // Нижняя панель навигации Material 3 с капсульными индикаторами
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null) },
                        label = { Text("Медиатека") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Rounded.CloudSync, contentDescription = null) },
                        label = { Text("Синхрон.") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                        label = { Text("5-Полос EQ") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Rounded.BatterySaver, contentDescription = null) },
                        label = { Text("Энергия") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (currentTab) {
                0 -> LibraryTab(
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it },
                    currentTrack = currentTrack,
                    onSelectTrack = { playTrack(it) }
                )
                1 -> SyncTab()
                2 -> EqualizerTab(
                    bands = eqBands,
                    labels = eqFreqLabels,
                    onBandChange = { idx, gain ->
                        eqBands[idx] = gain
                        AuraPlaybackService.InstanceHolder.service?.setBandGain(idx.toShort(), gain)
                    }
                )
                3 -> BatteryTab(amoledMode = amoledMode, onToggleAmoled = onToggleAmoled)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTab(
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    currentTrack: AuraTrack,
    onSelectTrack: (AuraTrack) -> Unit
) {
    val filters = listOf(
        "ALL" to "Все потоки",
        "YTM" to "YouTube Music",
        "SC" to "SoundCloud",
        "TG" to "Telegram"
    )
    val tracks = StreamProviders.defaultCatalog.filter {
        selectedFilter == "ALL" || it.source == selectedFilter
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { (key, title) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { onSelectFilter(key) },
                        label = { Text(title, fontSize = 12.sp) },
                        shape = CircleShape
                    )
                }
            }
        }

        items(tracks) { track ->
            val isCurrent = track.id == currentTrack.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onSelectTrack(track) },
                shape = RoundedCornerShape(22.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                track.source,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${track.artist} • ${track.codecBadge}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Модули синхронизации", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        SyncCard(
            title = "YouTube Music (Opus 251)",
            desc = "Прямой аудиопоток без декодирования видео через NewPipe / Piped API + SponsorBlock.",
            status = "Активно"
        )
        SyncCard(
            title = "SoundCloud",
            desc = "Синхронизация лайков, плейлистов и андеграунд-миксов по OAuth / Client ID.",
            status = "Подключено"
        )
        SyncCard(
            title = "Telegram Cloud Audio",
            desc = "Индексация аудиофайлов (.flac, .mp3, .ogg) из «Избранного» (Saved Messages) и музыкальных каналов.",
            status = "Кэш + Стрим"
        )
    }
}

@Composable
fun SyncCard(title: String, desc: String, status: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EqualizerTab(
    bands: List<Float>,
    labels: List<String>,
    onBandChange: (Int, Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Аппаратный 5-полосный эквалайзер",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
        Text(
            "Напрямую управляет системным DSP чипом Android (android.media.audiofx.Equalizer) без нагрузки на CPU.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        bands.forEachIndexed { index, gain ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(labels[index], fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(
                            "${if (gain >= 0) "+" else ""}${"%.1f".format(gain)} дБ",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = gain,
                        onValueChange = { onBandChange(index, it) },
                        valueRange = -12f..12f
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryTab(amoledMode: Boolean, onToggleAmoled: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Энергоэффективность", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        SyncCard(
            title = "Пакетный буфер радиомодуля (90 сек)",
            desc = "ExoPlayer накапливает буфер в ОЗУ пачками и переводит модем телефона в глубокий сон (Doze Mode). Расход ~1.2% в час.",
            status = "Включено"
        )
        SyncCard(
            title = "Отключение видеодекодера (Audio-Only)",
            desc = "Из YouTube Music забирается только звуковая дорожка Opus 251 (160 кбит/с) без рендера видео.",
            status = "Активно"
        )
        Button(
            onClick = onToggleAmoled,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (amoledMode) "Выключить режим Pure AMOLED (#000000)" else "Включить режим Pure AMOLED (#000000)")
        }
    }
}
