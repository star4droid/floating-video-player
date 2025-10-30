package com.floatingvideoplayer.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.floatingvideoplayer.R;
import com.floatingvideoplayer.models.MediaFile;

import java.util.ArrayList;
import java.util.List;

/**
 * ArrayAdapter for displaying media files in grid view
 * Supports multi-selection mode with checkboxes and thumbnails
 */
public class MediaFileGridAdapter extends ArrayAdapter<MediaFile> {
    
    private Context context;
    private List<MediaFile> files;
    private LayoutInflater inflater;
    
    public MediaFileGridAdapter(Context context, List<MediaFile> files) {
        super(context, R.layout.media_file_grid_item, files);
        this.context = context;
        this.files = files;
        this.inflater = LayoutInflater.from(context);
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.media_file_grid_item, parent, false);
            holder = new ViewHolder();
            holder.thumbnailImageView = convertView.findViewById(R.id.iv_file_thumbnail);
            holder.nameTextView = convertView.findViewById(R.id.tv_file_name);
            holder.detailsTextView = convertView.findViewById(R.id.tv_file_details);
            holder.checkBox = convertView.findViewById(R.id.cb_file_select);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        MediaFile mediaFile = files.get(position);
        
        // Set file thumbnail/icon
        if (mediaFile.getThumbnail() != null) {
            // Display thumbnail if available
            // Note: This would require additional thumbnail loading logic
            holder.thumbnailImageView.setImageResource(mediaFile.getIconResource());
        } else {
            holder.thumbnailImageView.setImageResource(mediaFile.getIconResource());
        }
        
        // Set file name
        String displayName = mediaFile.getName();
        if (displayName.length() > 20) {
            displayName = displayName.substring(0, 17) + "...";
        }
        holder.nameTextView.setText(displayName);
        
        // Set file details
        String details = getFileDetails(mediaFile);
        holder.detailsTextView.setText(details);
        
        // Set checkbox visibility based on selection state
        boolean showCheckbox = mediaFile.isSelected();
        holder.checkBox.setVisibility(showCheckbox ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(mediaFile.isSelected());
        
        // Set background color for selection
        if (mediaFile.isSelected()) {
            convertView.setBackgroundResource(R.drawable.selected_file_background);
        } else {
            convertView.setBackgroundResource(android.R.color.transparent);
        }
        
        return convertView;
    }
    
    /**
     * Get formatted file details based on file type (short version for grid view)
     */
    private String getFileDetails(MediaFile mediaFile) {
        if (mediaFile.isDirectory()) {
            return "Folder";
        } else if (mediaFile.isVideo()) {
            return getVideoDetails(mediaFile);
        } else if (mediaFile.isAudio()) {
            return getAudioDetails(mediaFile);
        } else {
            return mediaFile.getExtension().toUpperCase();
        }
    }
    
    /**
     * Get video file details (short version)
     */
    private String getVideoDetails(MediaFile mediaFile) {
        StringBuilder details = new StringBuilder();
        
        if (mediaFile.getDuration() > 0) {
            details.append(mediaFile.getFormattedDuration());
        }
        
        if (mediaFile.getDuration() <= 0 && !mediaFile.getResolution().equals("Unknown")) {
            details.append(mediaFile.getResolution());
        }
        
        return details.length() > 0 ? details.toString() : "Video";
    }
    
    /**
     * Get audio file details (short version)
     */
    private String getAudioDetails(MediaFile mediaFile) {
        StringBuilder details = new StringBuilder();
        
        if (mediaFile.getDuration() > 0) {
            details.append(mediaFile.getFormattedDuration());
        }
        
        if (mediaFile.getDuration() <= 0 && !mediaFile.getArtist().isEmpty()) {
            details.append(mediaFile.getArtist());
        }
        
        return details.length() > 0 ? details.toString() : "Audio";
    }
    
    /**
     * Update the files list and notify adapter
     */
    public void updateFiles(List<MediaFile> newFiles) {
        this.files.clear();
        this.files.addAll(newFiles);
        notifyDataSetChanged();
    }
    
    /**
     * Get all files in the adapter
     */
    public List<MediaFile> getFiles() {
        return files;
    }
    
    /**
     * Clear all selections
     */
    public void clearSelections() {
        for (MediaFile file : files) {
            file.setSelected(false);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Get selected files
     */
    public List<MediaFile> getSelectedFiles() {
        List<MediaFile> selected = new ArrayList<>();
        for (MediaFile file : files) {
            if (file.isSelected()) {
                selected.add(file);
            }
        }
        return selected;
    }
    
    /**
     * Select or deselect all files
     */
    public void setAllSelected(boolean selected) {
        for (MediaFile file : files) {
            file.setSelected(selected);
        }
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder pattern for efficient grid item recycling
     */
    private static class ViewHolder {
        ImageView thumbnailImageView;
        TextView nameTextView;
        TextView detailsTextView;
        CheckBox checkBox;
    }
}