# Media Viewer Architecture

## Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              NetifaceApp (Composable)                │   │
│  │  - showFileBrowser: Boolean                          │   │
│  │  - showMediaViewer: Boolean                          │   │
│  │  - mediaFiles: List<NfsFileInfo>                     │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────┬─────────────────┬──────────────────┬─────────┘
               │                 │                  │
               ▼                 ▼                  ▼
    ┌─────────────────┐ ┌──────────────┐ ┌──────────────────┐
    │ ConnectionScreen│ │FileBrowserScr│ │ImageVideoViewer  │
    │                 │ │              │ │Screen            │
    └─────────────────┘ └──────┬───────┘ └───────┬──────────┘
                               │                  │
                  Clicks on    │   Opens with     │
                  media file ──┘   media list ────┘
                                   
┌──────────────────────────────────────────────────────────────┐
│          ImageVideoViewerScreen                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │          HorizontalPager (Swipe Navigation)            │  │
│  │                                                        │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────┐   │  │
│  │  │ ImageViewer  │  │ VideoPlayer  │  │ImageView.│   │  │
│  │  │   (Page 0)   │  │   (Page 1)   │  │ (Page 2) │   │  │
│  │  └──────┬───────┘  └──────┬───────┘  └─────┬────┘   │  │
│  │         │                  │                │        │  │
│  └─────────┼──────────────────┼────────────────┼────────┘  │
└────────────┼──────────────────┼────────────────┼───────────┘
             │                  │                │
             ▼                  ▼                ▼
    ┌─────────────────┐ ┌──────────────┐ ┌──────────────┐
    │   NfsFetcher    │ │NfsDataSource │ │  NfsFetcher  │
    │  (Coil Image)   │ │ (ExoPlayer)  │ │ (Coil Image) │
    └────────┬────────┘ └──────┬───────┘ └──────┬───────┘
             │                  │                │
             └──────────────────┼────────────────┘
                                │
                                ▼
                    ┌────────────────────────┐
                    │      NfsClient         │
                    │  readFile(path,        │
                    │           offset,      │
                    │           size)        │
                    └───────────┬────────────┘
                                │
                                ▼
                    ┌────────────────────────┐
                    │  Native JNI Layer      │
                    │  (nfs_wrapper.cpp)     │
                    └───────────┬────────────┘
                                │
                                ▼
                    ┌────────────────────────┐
                    │   libnfs Library       │
                    │   (NFS Protocol)       │
                    └────────────────────────┘
```

## Data Flow for Image Streaming

```
User taps image.jpg in FileBrowserScreen
    │
    ▼
MainActivity filters media files [image.jpg, video.mp4, photo.png]
    │
    ▼
Opens ImageVideoViewerScreen with initialIndex=0
    │
    ▼
HorizontalPager creates ImageViewer for page 0
    │
    ▼
ImageViewer creates Coil ImageLoader with NfsFetcher
    │
    ▼
Coil requests image data
    │
    ▼
NfsFetcher creates NfsBufferedSource("/path/image.jpg")
    │
    ▼
NfsBufferedSource.read() called
    │
    ▼
Calls NfsClient.readFile("/path/image.jpg", offset=0, size=64KB)
    │
    ▼
JNI calls nativeReadFile()
    │
    ▼
nfs_wrapper.cpp uses libnfs: nfs_read()
    │
    ▼
Returns 64KB chunk to NfsBufferedSource
    │
    ▼
Buffer fills, Coil decoder processes chunk
    │
    ▼
More data needed? Request next chunk at offset=64KB
    │
    ▼
Repeat until entire image loaded
    │
    ▼
Image displayed on screen
```

## Data Flow for Video Streaming

```
User swipes to video.mp4
    │
    ▼
HorizontalPager creates VideoPlayer for page 1
    │
    ▼
VideoPlayer creates ExoPlayer with NfsDataSource
    │
    ▼
ExoPlayer prepares MediaSource with URI "nfs:///path/video.mp4"
    │
    ▼
NfsDataSource.open() called
    │
    ▼
Gets file size from NfsFileInfo
    │
    ▼
ExoPlayer starts playback
    │
    ▼
NfsDataSource.read(buffer, offset, length) called
    │
    ▼
Calls NfsClient.readFile("/path/video.mp4", position, length)
    │
    ▼
JNI calls nativeReadFile()
    │
    ▼
nfs_wrapper.cpp uses libnfs: nfs_open(), nfs_lseek(), nfs_read()
    │
    ▼
Returns chunk to NfsDataSource
    │
    ▼
ExoPlayer decodes and buffers video
    │
    ▼
Video plays on screen
    │
    ▼
Player needs more data? Calls read() again with new position
    │
    ▼
Repeat as video plays
```

## Key Design Decisions

### 1. Streaming Architecture
- **Why chunk-based?** Large media files (100MB+) cannot fit in memory
- **Chunk size:** 64KB for images balances network calls vs memory
- **Progressive loading:** User sees content while it's still loading

### 2. Custom Data Sources
- **Coil NfsFetcher:** Implements BufferedSource for image streaming
- **ExoPlayer NfsDataSource:** Implements BaseDataSource for video streaming
- **Why custom?** Standard sources don't support NFS protocol

### 3. Navigation Pattern
- **HorizontalPager:** Native Compose component (no external dependencies)
- **Filter media files:** Only show images/videos in viewer
- **Maintain order:** Files shown in same order as file browser

### 4. Performance Optimizations
- **Lazy loading:** Only load media when page becomes visible
- **Auto-pause:** Videos pause when swiped away
- **Resource cleanup:** ExoPlayer properly released on dispose

### 5. User Experience
- **Tap to toggle controls:** Maximizes screen space for media
- **Position indicators:** User knows where they are in collection
- **Mixed media:** Seamlessly navigate between images and videos

## File Responsibilities

### ImageVideoViewerScreen.kt
- Main viewer UI with HorizontalPager
- Controls visibility management
- Page navigation

### NfsFetcher.kt
- Custom Coil fetcher
- BufferedSource implementation
- Image chunk streaming

### NfsDataSource.kt
- Custom ExoPlayer DataSource
- Video chunk streaming
- Seek support

### FileTypeUtil.kt
- Media type detection
- Extension mapping

### MainActivity.kt
- Navigation state management
- Media file filtering

### FileBrowserScreen.kt
- Media file icon display
- Click handling for media files
