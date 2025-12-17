# Media Viewer Feature

## Overview

The Netiface app now includes a full-featured media viewer that allows users to view images and videos stored on NFS shares with smooth swipe navigation between files.

## How to Use

### Opening the Viewer

1. **Connect to an NFS server** using the connection screen
2. **Browse to a folder** containing images or videos
3. **Tap on any image or video file** to open the viewer

The viewer will automatically:
- Detect all media files in the current folder
- Open at the selected file's position
- Allow swiping to navigate through all media files

### Viewer Controls

- **Swipe left/right**: Navigate between media files
- **Tap anywhere**: Toggle controls visibility (top bar and indicators)
- **Close button**: Exit viewer and return to file browser
- **Video controls**: Play/pause, seek, volume (for video files)

### Visual Indicators

- **Top bar**: Shows filename and position (e.g., "3 / 10")
- **Bottom dots**: Show your position in the media collection
- **File icons**: File browser shows special icons for images and videos

## Supported Formats

### Images
- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)
- BMP (.bmp)
- WebP (.webp)
- HEIC (.heic)
- HEIF (.heif)

### Videos
- MP4 (.mp4)
- MKV (.mkv)
- AVI (.avi)
- MOV (.mov)
- WMV (.wmv)
- FLV (.flv)
- WebM (.webm)
- M4V (.m4v)
- 3GP (.3gp)

## Features

### Streaming Support

Both images and videos **stream data in chunks** rather than loading entire files into memory. This means:

- ✅ Can view very large media files (100MB+)
- ✅ Minimal memory usage
- ✅ Faster initial load times
- ✅ Works well over slower network connections

**How it works:**
- **Images**: Load in 64KB chunks and display progressively
- **Videos**: Stream data on-demand as playback progresses

### Mixed Media Navigation

You can seamlessly swipe between images and videos in the same folder:

```
Folder: /photos/vacation/
  image1.jpg  ←─┐
  video1.mp4    │ Swipe between
  image2.png    │ all these files
  video2.mp4  ──┘
```

### Smart Playback

- **Auto-pause**: Videos automatically pause when you swipe away
- **Auto-play**: Videos start playing when swiped to
- **Position memory**: Videos remember playback position (within session)

## User Interface

### Full-Screen Experience

```
┌─────────────────────────────────────┐
│ ✕ vacation.mp4         2 / 5        │ ← Top bar (tap to hide)
│                                     │
│                                     │
│                                     │
│          [  Media Content  ]        │
│                                     │
│                                     │
│                                     │
│            ● ○ ○ ○ ○                │ ← Position indicators
└─────────────────────────────────────┘
```

### Controls Visibility

- **Shown by default** when opening viewer
- **Tap anywhere** on the screen to hide controls
- **Tap again** to show controls
- **Maximizes viewing area** when hidden

## Technical Details

### Architecture

The media viewer is built with:

- **Jetpack Compose**: Modern declarative UI
- **HorizontalPager**: Native Compose swipe component
- **Coil**: Image loading library with custom NFS fetcher
- **ExoPlayer**: Professional-grade video player with custom NFS data source

### Streaming Implementation

#### Image Streaming
```
NfsBufferedSource (64KB chunks)
    ↓
Coil ImageLoader
    ↓
Progressive image decoding
    ↓
Display on screen
```

#### Video Streaming
```
NfsDataSource (on-demand chunks)
    ↓
ExoPlayer MediaSource
    ↓
Video decoding and buffering
    ↓
Playback on screen
```

### Network Efficiency

- **Lazy loading**: Only loads media when page becomes visible
- **Chunk-based**: Reads small chunks instead of entire files
- **Progressive display**: Shows content while still loading
- **Bandwidth friendly**: Stops loading when not needed

## Performance

### Memory Usage

- **Images**: ~10-20MB per image (depends on resolution)
- **Videos**: ~5-10MB buffer (ExoPlayer's internal buffer)
- **Total**: ~20-30MB for typical usage

### Network Usage

- **Initial load**: Only loads visible page
- **Swipe**: Loads next page on demand
- **Background**: Pauses all network activity when viewer closed

### Tested Scenarios

✅ Large images (20+ MP, 50MB+ files)
✅ HD videos (1080p, 1GB+ files)
✅ Slow network connections (10 Mbps)
✅ Mixed media folders (100+ files)
✅ Quick swiping through multiple files

## Limitations

### Current Limitations

- No zoom/pan for images (planned for future)
- No subtitle support for videos (planned for future)
- No download to device option
- No sharing functionality
- No landscape mode optimization

### Known Issues

- Very large images (>50MP) may take time to decode
- Some exotic video codecs may not be supported
- Network interruptions may cause playback to stall

## Troubleshooting

### Images Not Loading

1. Check that the file extension is supported
2. Verify NFS connection is still active
3. Check that the file is not corrupted
4. Try refreshing the file browser

### Videos Not Playing

1. Check that the video codec is supported by Android
2. Verify sufficient network bandwidth
3. Check for any NFS connection issues
4. Try seeking to a different position in the video

### Performance Issues

1. Close and reopen the viewer to clear buffers
2. Check your NFS server performance
3. Verify network connection quality
4. Reduce video quality if possible

## Future Enhancements

Planned features for future versions:

- 🔍 Pinch-to-zoom for images
- 🔄 Image rotation support
- 📥 Download media to device
- 🔗 Share media with other apps
- 📺 Chromecast support
- 🎨 Photo editing tools
- 📊 EXIF data display
- 🎬 Video trimming
- 🌐 Cloud sync support

## Feedback

If you encounter any issues or have feature requests, please:

1. Check the troubleshooting section above
2. Review the known limitations
3. Open an issue on GitHub with:
   - Device model and Android version
   - File type and size
   - Steps to reproduce
   - Expected vs actual behavior

## Credits

Built using:
- [Coil](https://coil-kt.github.io/coil/) for image loading
- [ExoPlayer](https://exoplayer.dev/) for video playback
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for UI
- [libnfs](https://github.com/sahlberg/libnfs) for NFS protocol
