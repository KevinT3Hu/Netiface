package com.netiface.nfsclient

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageVideoViewerScreen(
    mediaFiles: List<NfsFileInfo>,
    initialIndex: Int,
    nfsClient: NfsClient,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaFiles.size }
    )
    
    var showControls by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            if (showControls) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = mediaFiles.getOrNull(pagerState.currentPage)?.name ?: "",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${pagerState.currentPage + 1} / ${mediaFiles.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.7f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        }
                    )
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val mediaFile = mediaFiles[page]
                val fileType = FileTypeUtil.getMediaFileType(mediaFile.name)
                
                when (fileType) {
                    MediaFileType.IMAGE -> {
                        ImageViewer(
                            filePath = mediaFile.path,
                            nfsClient = nfsClient
                        )
                    }
                    MediaFileType.VIDEO -> {
                        VideoPlayer(
                            filePath = mediaFile.path,
                            nfsClient = nfsClient,
                            isCurrentPage = pagerState.currentPage == page
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Unsupported file type",
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // Navigation indicators
            if (showControls && mediaFiles.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(mediaFiles.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .background(
                                    if (index == pagerState.currentPage) Color.White else Color.Gray,
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageViewer(
    filePath: String,
    nfsClient: NfsClient
) {
    val context = LocalContext.current
    
    // Create ImageLoader with custom NFS fetcher for streaming
    val imageLoader = remember(nfsClient) {
        context.imageLoader.newBuilder()
            .components {
                add(NfsFetcher.Factory(nfsClient))
            }
            .build()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(NfsImageData(filePath))
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            onError = { error ->
                android.util.Log.e("ImageViewer", "Error loading image: ${error.result.throwable}")
            }
        )
    }
}

@Composable
fun VideoPlayer(
    filePath: String,
    nfsClient: NfsClient,
    isCurrentPage: Boolean
) {
    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            
            // Create custom data source factory for NFS streaming
            val dataSourceFactory = NfsDataSource.Factory(nfsClient)
            
            // Create media item with NFS URI
            val mediaItem = MediaItem.fromUri("${NfsDataSource.SCHEME}://$filePath")
            
            // Create progressive media source for streaming
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            
            setMediaSource(mediaSource)
            prepare()
        }
    }
    
    // Control playback based on whether this page is visible
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.pause()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
