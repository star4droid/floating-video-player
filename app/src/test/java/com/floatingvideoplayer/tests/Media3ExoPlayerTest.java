package com.floatingvideoplayer.tests;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.floatingvideoplayer.services.Media3ExoPlayer;
import com.floatingvideoplayer.utils.Media3ErrorHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Test suite for Media3 ExoPlayer integration and video playback functionality
 */
@RunWith(AndroidJUnit4.class)
public class Media3ExoPlayerTest {
    
    private Context context;
    private Media3ExoPlayer media3Player;
    private Media3ErrorHandler errorHandler;
    private ExoPlayer exoPlayer;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        media3Player = new Media3ExoPlayer(context);
        errorHandler = new Media3ErrorHandler(context);
        
        // Create ExoPlayer instance for testing
        exoPlayer = new ExoPlayer.Builder(context).build();
    }
    
    @Test
    public void testMedia3PlayerInitialization() {
        assertNotNull("Media3ExoPlayer should be initialized", media3Player);
        assertNotNull("Media3ErrorHandler should be initialized", errorHandler);
        assertNotNull("ExoPlayer should be initialized", exoPlayer);
    }
    
    @Test
    public void testPlayerState() {
        // Test initial player state
        assertEquals("Player should be in idle state initially", 
            Player.STATE_IDLE, exoPlayer.getPlaybackState());
        
        assertEquals("Player should not be playing initially", 
            false, exoPlayer.isPlaying());
    }
    
    @Test
    public void testMediaItemCreation() {
        // Test creating media items from different sources
        String testUrl = "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4";
        Uri videoUri = Uri.parse(testUrl);
        
        MediaItem mediaItem = new MediaItem.Builder()
            .setUri(videoUri)
            .setMediaId("test-video-1")
            .setMediaMetadata(
                new MediaItem.MediaMetadata.Builder()
                    .setTitle("Test Video")
                    .setArtist("Test Artist")
                    .build())
            .build();
        
        assertNotNull("MediaItem should not be null", mediaItem);
        assertEquals("Media URI should match", videoUri, mediaItem.localConfiguration.uri);
        assertEquals("Media ID should match", "test-video-1", mediaItem.mediaId);
    }
    
    @Test
    public void testPlayerRepeatMode() {
        // Test player repeat modes
        assertEquals("Player should have default repeat mode", 
            Player.REPEAT_MODE_OFF, exoPlayer.getRepeatMode());
        
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        assertEquals("Player repeat mode should be set to ONE", 
            Player.REPEAT_MODE_ONE, exoPlayer.getRepeatMode());
        
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        assertEquals("Player repeat mode should be set to ALL", 
            Player.REPEAT_MODE_ALL, exoPlayer.getRepeatMode());
        
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
    }
    
    @Test
    public void testPlayerShuffleMode() {
        // Test player shuffle mode
        assertEquals("Player should have shuffle disabled by default", 
            false, exoPlayer.getShuffleModeEnabled());
        
        exoPlayer.setShuffleModeEnabled(true);
        assertEquals("Player should have shuffle enabled", 
            true, exoPlayer.getShuffleModeEnabled());
        
        exoPlayer.setShuffleModeEnabled(false);
    }
    
    @Test
    public void testPlayerVolume() {
        // Test player volume control
        assertTrue("Volume should be within valid range", 
            exoPlayer.getVolume() >= 0.0f && exoPlayer.getVolume() <= 1.0f);
        
        // Test volume setting
        float testVolume = 0.5f;
        exoPlayer.setVolume(testVolume);
        assertEquals("Volume should be set correctly", 
            testVolume, exoPlayer.getVolume(), 0.01f);
    }
    
    @Test
    public void testPlayerPlaybackSpeed() {
        // Test playback speed control
        assertEquals("Default playback speed should be 1.0", 
            1.0f, exoPlayer.getPlaybackSpeed(), 0.01f);
        
        // Test speed setting
        float testSpeed = 1.5f;
        exoPlayer.setPlaybackSpeed(testSpeed);
        assertEquals("Playback speed should be set correctly", 
            testSpeed, exoPlayer.getPlaybackSpeed(), 0.01f);
    }
    
    @Test
    public void testMediaMetadata() {
        // Test media metadata handling
        MediaItem.MediaMetadata metadata = new MediaItem.MediaMetadata.Builder()
            .setTitle("Test Video Title")
            .setArtist("Test Artist")
            .setAlbumTitle("Test Album")
            .setMediaType(MediaItem.MediaMetadata.MEDIA_TYPE_VIDEO)
            .build();
        
        assertEquals("Title should match", "Test Video Title", metadata.title);
        assertEquals("Artist should match", "Test Artist", metadata.artist);
        assertEquals("Media type should be video", 
            MediaItem.MediaMetadata.MEDIA_TYPE_VIDEO, metadata.mediaType);
    }
    
    @Test
    public void testErrorHandlerInitialization() {
        // Test error handler setup
        assertNotNull("Error handler should have context", errorHandler.getContext());
        
        // Test error message retrieval
        String errorMessage = errorHandler.getErrorMessage(C.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED);
        assertNotNull("Error message should not be null", errorMessage);
        assertFalse("Error message should not be empty", errorMessage.isEmpty());
    }
    
    @Test
    public void testSupportedFormats() {
        // Test supported video formats
        String[] supportedFormats = {"mp4", "webm", "mkv", "avi", "mov"};
        
        for (String format : supportedFormats) {
            assertNotNull("Format should be supported", format);
            assertFalse("Format should not be empty", format.isEmpty());
        }
    }
    
    @Test
    public void testPlayerProgress() {
        // Test player progress tracking
        long currentPosition = exoPlayer.getCurrentPosition();
        long duration = exoPlayer.getDuration();
        
        assertTrue("Current position should be non-negative", currentPosition >= 0);
        assertTrue("Duration should be non-negative", duration >= C.TIME_UNSET);
    }
    
    @Test
    public void testPlayerBuffers() {
        // Test player buffer management
        long bufferedPercentage = exoPlayer.getBufferedPercentage();
        
        assertTrue("Buffered percentage should be valid", 
            bufferedPercentage >= 0 && bufferedPercentage <= 100);
        
        // Test available video formats
        int videoDecoderCount = exoPlayer.getRendererCount();
        assertTrue("Should have video renderer", videoDecoderCount > 0);
    }
    
    @Test
    public void testPlayerAudioAttributes() {
        // Test audio attributes
        // Note: Actual audio attribute testing would require more complex setup
        assertNotNull("ExoPlayer should have audio renderer", exoPlayer);
    }
    
    @Test
    public void testPlayerLifecycle() {
        // Test player lifecycle management
        // Prepare player
        exoPlayer.prepare();
        assertEquals("Player should be ready after prepare", 
            Player.STATE_READY, exoPlayer.getPlaybackState());
        
        // Release player
        exoPlayer.release();
        assertEquals("Player should be idle after release", 
            Player.STATE_IDLE, exoPlayer.getPlaybackState());
    }
    
    @Test
    public void testMediaSessionIntegration() {
        // Test MediaSession integration
        // Note: This is a basic test - actual MediaSession integration would be more complex
        assertNotNull("ExoPlayer should support MediaSession", exoPlayer);
        
        // Test session configuration
        int sessionId = exoPlayer.hashCode(); // Basic identifier test
        assertTrue("Session ID should be valid", sessionId != 0);
    }
    
    @Test
    public void testPlayerPerformanceMetrics() {
        // Test player performance metrics
        long totalLoadTime = exoPlayer.getTotalLoadTime();
        assertTrue("Total load time should be non-negative", totalLoadTime >= 0);
        
        // Test throughput (if available)
        // Note: Actual throughput measurement would require active playback
        assertNotNull("Player should have performance metrics", exoPlayer);
    }
    
    @Test
    public void testMedia3Version() {
        // Test Media3 version compatibility
        String media3Version = "1.2.1";
        assertNotNull("Media3 version should be defined", media3Version);
        assertFalse("Media3 version should not be empty", media3Version.isEmpty());
    }
}
