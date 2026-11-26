package com.rsounds.app

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.rsounds.core.lyrics.LyricsDocument
import com.rsounds.core.lyrics.LyricsTimeline
import com.rsounds.provider.local.LocalAudioTrack
import com.rsounds.provider.local.MediaStoreAudioRepository
import com.rsounds.provider.lrclib.LrclibLookupResult
import com.rsounds.provider.lrclib.LrclibRepository
import com.rsounds.provider.lrclib.LrclibTrackQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

@UnstableApi
class MainActivity : ComponentActivity() {
    private var mediaController by mutableStateOf<MediaController?>(null)
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                RsoundsHomeScreen(controller = mediaController)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(
            this,
            ComponentName(this, com.rsounds.core.player.RsoundsPlaybackService::class.java)
        )
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            { mediaController = runCatching { future.get() }.getOrNull() },
            Executor { command -> runOnUiThread(command) }
        )
    }

    override fun onStop() {
        val future = controllerFuture
        controllerFuture = null
        mediaController = null
        if (future != null) MediaController.releaseFuture(future)
        super.onStop()
    }
}

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
private fun RsoundsHomeScreen(controller: MediaController?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mediaPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    var hasPermission by remember {
        mutableStateOf(context.checkSelfPermission(mediaPermission) == PackageManager.PERMISSION_GRANTED)
    }
    var tracks by remember { mutableStateOf(emptyList<LocalAudioTrack>()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember(controller) { mutableStateOf(controller?.isPlaying == true) }
    var currentTitle by remember(controller) {
        mutableStateOf(controller?.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty())
    }
    var lyricsDocument by remember { mutableStateOf<LyricsDocument?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }
    var lyricsMessage by remember { mutableStateOf<String?>(null) }
    var showLyrics by remember { mutableStateOf(false) }
    var playbackPositionMs by remember { mutableStateOf(0L) }

    val localRepository = remember(context) {
        MediaStoreAudioRepository(context.contentResolver)
    }
    val lyricsRepository = remember { LrclibRepository() }
    val coroutineScope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    val currentTrack = tracks.firstOrNull {
        it.mediaStoreId.toString() == controller?.currentMediaItem?.mediaId
    }

    LaunchedEffect(hasPermission, localRepository) {
        if (hasPermission) {
            isLoading = true
            loadError = null
            val result = withContext(Dispatchers.IO) {
                runCatching { localRepository.getTracks() }
            }
            tracks = result.getOrElse {
                loadError = it.message ?: "Could not read the audio library."
                emptyList()
            }
            isLoading = false
        } else {
            tracks = emptyList()
        }
    }

    LaunchedEffect(controller, isPlaying, showLyrics, lyricsDocument) {
        if (controller != null && showLyrics && lyricsDocument != null) {
            while (true) {
                playbackPositionMs = controller.currentPosition.coerceAtLeast(0L)
                delay(if (isPlaying) 100L else 250L)
            }
        } else {
            playbackPositionMs = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
        }
    }

    DisposableEffect(controller, showLyrics) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentTitle = mediaItem?.mediaMetadata?.title?.toString().orEmpty()
                lyricsDocument = null
                lyricsMessage = if (showLyrics) "Track changed. Load lyrics for the new song." else null
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    fun loadLyrics(track: LocalAudioTrack) {
        showLyrics = true
        lyricsLoading = true
        lyricsDocument = null
        lyricsMessage = "Searching LRCLIB…"
        coroutineScope.launch {
            val result = lyricsRepository.lookup(
                LrclibTrackQuery(
                    trackName = track.title,
                    artistName = track.artist,
                    albumName = track.album,
                    durationMs = track.durationMs
                )
            )
            lyricsLoading = false
            when (result) {
                is LrclibLookupResult.Found -> {
                    lyricsDocument = result.document
                    lyricsMessage = if (result.signatureMatch.isStrong) {
                        "Matched " + result.record.trackName.orEmpty() + " · " +
                            result.record.artistName.orEmpty()
                    } else {
                        "Possible metadata mismatch. Check the title, artist and version before trusting this lyric."
                    }
                }
                LrclibLookupResult.Instrumental -> lyricsMessage = "This recording is marked instrumental."
                LrclibLookupResult.NoLyricsAvailable -> lyricsMessage = "The record was found, but no usable lyrics were supplied."
                LrclibLookupResult.NotFound -> lyricsMessage = "No lyrics match was found for this recording."
                is LrclibLookupResult.RateLimited -> lyricsMessage =
                    "LRCLIB rate limit. Retry after about " + (result.retryAfterMs / 1_000L) + " seconds."
                is LrclibLookupResult.TemporarilyUnavailable -> lyricsMessage =
                    "LRCLIB is temporarily busy. Retry after about " + (result.retryAfterMs / 1_000L) + " seconds."
                is LrclibLookupResult.HttpFailure -> lyricsMessage =
                    "LRCLIB returned HTTP " + result.statusCode + "."
                is LrclibLookupResult.InvalidResponse -> lyricsMessage = result.reason
                is LrclibLookupResult.NetworkFailure -> lyricsMessage = result.reason
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("RSounds", fontWeight = FontWeight.Bold)
                        Text(
                            "Music, in every word.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (controller?.currentMediaItem != null) {
                Surface(tonalElevation = 6.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                currentTitle.ifBlank { "Now playing" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                currentTrack?.artist ?: "Local library",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        if (currentTrack != null) {
                            Button(onClick = { loadLyrics(currentTrack) }) { Text("Lyrics") }
                        }
                        Button(onClick = {
                            if (isPlaying) controller.pause() else controller.play()
                        }) {
                            Text(if (isPlaying) "Pause" else "Play")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Your library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (hasPermission) tracks.size.toString() + " tracks on this device"
                        else "Your music stays on your device. Grant audio access to scan it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showLyrics) {
                item {
                    LyricsPanel(
                        document = lyricsDocument,
                        isLoading = lyricsLoading,
                        message = lyricsMessage,
                        playbackPositionMs = playbackPositionMs,
                        onClose = { showLyrics = false }
                    )
                }
            }

            if (!hasPermission) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Connect your music library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "RSounds only reads audio shared with Android MediaStore. It does not move, upload, or modify your files.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { permissionLauncher.launch(mediaPermission) }) {
                                Text("Allow audio access")
                            }
                        }
                    }
                }
            } else if (isLoading) {
                item { Text("Scanning audio library…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (loadError != null) {
                item { Text("Library scan failed: " + loadError, color = MaterialTheme.colorScheme.error) }
            } else if (tracks.isEmpty()) {
                item {
                    Text(
                        "No audio tracks found. Add music to your device and scan again.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(tracks, key = { it.mediaStoreId }) { track ->
                    LocalTrackRow(track = track) {
                        if (controller != null) {
                            lyricsDocument = null
                            lyricsMessage = null
                            showLyrics = false
                            playTrack(controller, tracks, track)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsPanel(
    document: LyricsDocument?,
    isLoading: Boolean,
    message: String?,
    playbackPositionMs: Long,
    onClose: () -> Unit
) {
    val cursor = remember(document, playbackPositionMs) {
        document?.let { LyricsTimeline.locate(it, playbackPositionMs) }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(cursor?.lineIndex, document?.lines?.size) {
        val index = cursor?.lineIndex
        if (index != null && index >= 0) listState.animateScrollToItem(index)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lyrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        document?.syncLevel?.name?.replace('_', ' ') ?: "LRCLIB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onClose) { Text("Close") }
            }

            if (isLoading) Text("Searching synchronized lyrics…", style = MaterialTheme.typography.bodyMedium)
            if (message != null) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (document == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.secondary
                )
            }
            if (document != null) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = document.lines,
                        key = { index, line -> index.toString() + "-" + (line.startMs?.toString() ?: "plain") }
                    ) { index, line ->
                        val activeLine = cursor?.lineIndex == index
                        val activeWordColor = MaterialTheme.colorScheme.primary
                        val annotatedLine = buildAnnotatedString {
                            if (line.words.isEmpty()) {
                                append(line.text)
                            } else {
                                line.words.forEachIndexed { wordIndex, word ->
                                    if (activeLine && cursor?.wordIndex == wordIndex) {
                                        withStyle(
                                            SpanStyle(
                                                color = activeWordColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) { append(word.text) }
                                    } else {
                                        append(word.text)
                                    }
                                }
                            }
                        }
                        Text(
                            text = annotatedLine,
                            style = if (activeLine) MaterialTheme.typography.titleMedium
                            else MaterialTheme.typography.bodyLarge,
                            color = if (activeLine) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalTrackRow(track: LocalAudioTrack, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Text("♪", modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp), style = MaterialTheme.typography.titleLarge)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(track.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                track.artist + " · " + track.album,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(formatDuration(track.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun playTrack(
    controller: MediaController,
    tracks: List<LocalAudioTrack>,
    selectedTrack: LocalAudioTrack
) {
    val startIndex = tracks.indexOfFirst { it.mediaStoreId == selectedTrack.mediaStoreId }
    if (startIndex < 0) return

    val mediaItems = tracks.map { track ->
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .build()
        MediaItem.Builder()
            .setMediaId(track.mediaStoreId.toString())
            .setUri(track.contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    controller.setMediaItems(mediaItems, startIndex, 0L)
    controller.prepare()
    controller.play()
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return minutes.toString() + ":" + seconds.toString().padStart(2, '0')
}
