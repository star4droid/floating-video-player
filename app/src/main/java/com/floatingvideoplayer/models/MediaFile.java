package com.floatingvideoplayer.models;

import android.net.Uri;
import android.os.Parcelable;
import android.os.Parcel;

import java.util.Date;

/**
 * Model class representing a media file with metadata
 */
public class MediaFile implements Parcelable {
    
    public enum MediaType {
        VIDEO,
        AUDIO,
        UNKNOWN
    }
    
    public enum SortBy {
        NAME,
        SIZE,
        DATE_ADDED,
        DURATION,
        TYPE
    }
    
    // Basic file properties
    private String name;
    private String path;
    private String extension;
    private long size;
    private long dateAdded;
    private long dateModified;
    private MediaType type;
    
    // Media-specific properties
    private long duration; // Duration in milliseconds
    private int width; // For videos
    private int height; // For videos
    private int bitrate; // Bitrate in kbps
    private String artist; // For audio files
    private String album; // For audio files
    private String title; // For audio files
    
    // File system properties
    private boolean isDirectory;
    private boolean isHidden;
    private boolean isFromMediaStore;
    
    // Thumbnail and content URI
    private Uri contentUri;
    private byte[] thumbnail;
    
    // Selected state for multi-selection
    private boolean isSelected;
    
    public MediaFile() {
        this.type = MediaType.UNKNOWN;
        this.size = 0;
        this.dateAdded = System.currentTimeMillis();
        this.dateModified = System.currentTimeMillis();
        this.isSelected = false;
        this.isDirectory = false;
        this.isHidden = false;
        this.isFromMediaStore = false;
    }
    
    public MediaFile(String name, String path, MediaType type) {
        this();
        this.name = name;
        this.path = path;
        this.type = type;
        this.extension = getExtensionFromPath(path);
    }
    
    // Getters and Setters
    public String getName() {
        return name != null ? name : "";
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPath() {
        return path != null ? path : "";
    }
    
    public void setPath(String path) {
        this.path = path;
        if (extension == null || extension.isEmpty()) {
            this.extension = getExtensionFromPath(path);
        }
    }
    
    public String getExtension() {
        return extension != null ? extension : "";
    }
    
    public void setExtension(String extension) {
        this.extension = extension != null ? extension.toLowerCase() : "";
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public long getDateAdded() {
        return dateAdded;
    }
    
    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }
    
    public long getDateModified() {
        return dateModified;
    }
    
    public void setDateModified(long dateModified) {
        this.dateModified = dateModified;
    }
    
    public MediaType getType() {
        return type;
    }
    
    public void setType(MediaType type) {
        this.type = type != null ? type : MediaType.UNKNOWN;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public int getBitrate() {
        return bitrate;
    }
    
    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }
    
    public String getArtist() {
        return artist != null ? artist : "";
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getAlbum() {
        return album != null ? album : "";
    }
    
    public void setAlbum(String album) {
        this.album = album;
    }
    
    public String getTitle() {
        return title != null ? title : "";
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public boolean isDirectory() {
        return isDirectory;
    }
    
    public void setIsDirectory(boolean directory) {
        this.isDirectory = directory;
    }
    
    public boolean isHidden() {
        return isHidden;
    }
    
    public void setIsHidden(boolean hidden) {
        this.isHidden = hidden;
    }
    
    public boolean isFromMediaStore() {
        return isFromMediaStore;
    }
    
    public void setIsFromMediaStore(boolean fromMediaStore) {
        this.isFromMediaStore = fromMediaStore;
    }
    
    public Uri getContentUri() {
        return contentUri;
    }
    
    public void setContentUri(Uri contentUri) {
        this.contentUri = contentUri;
    }
    
    public byte[] getThumbnail() {
        return thumbnail;
    }
    
    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
    
    // Utility methods
    
    /**
     * Get formatted file size (e.g., "1.2 MB", "500 KB")
     */
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Get formatted duration (e.g., "1:23:45", "5:30")
     */
    public String getFormattedDuration() {
        if (duration <= 0) return "Unknown";
        
        long seconds = duration / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, remainingSeconds);
        } else {
            return String.format("%d:%02d", minutes, remainingSeconds);
        }
    }
    
    /**
     * Get formatted date
     */
    public String getFormattedDate() {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", 
            java.util.Locale.getDefault());
        return dateFormat.format(new java.util.Date(dateModified));
    }
    
    /**
     * Get resolution string for videos (e.g., "1920x1080")
     */
    public String getResolution() {
        if (width > 0 && height > 0) {
            return width + "x" + height;
        }
        return "Unknown";
    }
    
    /**
     * Check if file is a video file
     */
    public boolean isVideo() {
        return type == MediaType.VIDEO;
    }
    
    /**
     * Check if file is an audio file
     */
    public boolean isAudio() {
        return type == MediaType.AUDIO;
    }
    
    /**
     * Get display name (for UI)
     */
    public String getDisplayName() {
        String displayName = name;
        if (isDirectory()) {
            displayName = "📁 " + displayName;
        } else if (isVideo()) {
            displayName = "🎬 " + displayName;
        } else if (isAudio()) {
            displayName = "🎵 " + displayName;
        } else {
            displayName = "📄 " + displayName;
        }
        return displayName;
    }
    
    /**
     * Get icon resource based on file type
     */
    public int getIconResource() {
        if (isDirectory()) {
            return R.drawable.ic_folder;
        } else if (isVideo()) {
            return R.drawable.ic_video_file;
        } else if (isAudio()) {
            return R.drawable.ic_audio_file;
        } else {
            return R.drawable.ic_file;
        }
    }
    
    /**
     * Get file extension from path
     */
    private String getExtensionFromPath(String path) {
        if (path == null || !path.contains(".")) {
            return "";
        }
        return path.substring(path.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Toggle selection state
     */
    public void toggleSelection() {
        this.isSelected = !this.isSelected;
    }
    
    // Parcelable implementation
    protected MediaFile(Parcel in) {
        name = in.readString();
        path = in.readString();
        extension = in.readString();
        size = in.readLong();
        dateAdded = in.readLong();
        dateModified = in.readLong();
        type = MediaType.valueOf(in.readString());
        duration = in.readLong();
        width = in.readInt();
        height = in.readInt();
        bitrate = in.readInt();
        artist = in.readString();
        album = in.readString();
        title = in.readString();
        isDirectory = in.readByte() != 0;
        isHidden = in.readByte() != 0;
        isFromMediaStore = in.readByte() != 0;
        isSelected = in.readByte() != 0;
        contentUri = in.readParcelable(Uri.class.getClassLoader());
        thumbnail = in.createByteArray();
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(path);
        dest.writeString(extension);
        dest.writeLong(size);
        dest.writeLong(dateAdded);
        dest.writeLong(dateModified);
        dest.writeString(type.name());
        dest.writeLong(duration);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeInt(bitrate);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeString(title);
        dest.writeByte((byte) (isDirectory ? 1 : 0));
        dest.writeByte((byte) (isHidden ? 1 : 0));
        dest.writeByte((byte) (isFromMediaStore ? 1 : 0));
        dest.writeByte((byte) (isSelected ? 1 : 0));
        dest.writeParcelable(contentUri, flags);
        dest.writeByteArray(thumbnail);
    }
    
    public static final Creator<MediaFile> CREATOR = new Creator<MediaFile>() {
        @Override
        public MediaFile createFromParcel(Parcel in) {
            return new MediaFile(in);
        }
        
        @Override
        public MediaFile[] newArray(int size) {
            return new MediaFile[size];
        }
    };
    
    @Override
    public String toString() {
        return "MediaFile{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", type=" + type +
                ", size=" + getFormattedSize() +
                ", duration=" + getFormattedDuration() +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        MediaFile mediaFile = (MediaFile) obj;
        return path != null ? path.equals(mediaFile.path) : mediaFile.path == null;
    }
    
    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }
}