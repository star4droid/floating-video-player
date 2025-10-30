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
 * ArrayAdapter for displaying media files in list view
 * Supports multi-selection mode with checkboxes
 */
public class MediaFileAdapter extends ArrayAdapter<MediaFile> {
    
    private Context context;
    private List<MediaFile> files;
    private LayoutInflater inflater;
    
    public MediaFileAdapter(Context context, List<MediaFile> files) {
        super(context, R.layout.media_file_list_item, files);
        this.context = context;
        this.files = files;
        this.inflater = LayoutInflater.from(context);
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.media_file_list_item, parent, false);
            holder = new ViewHolder();
            holder.iconImageView = convertView.findViewById(R.id.iv_file_icon);
            holder.nameTextView = convertView.findViewById(R.id.tv_file_name);
            holder.detailsTextView = convertView.findViewById(R.id.tv_file_details);
            holder.sizeTextView = convertView.findViewById(R.id.tv_file_size);
            holder.dateTextView = convertView.findViewById(R.id.tv_file_date);
            holder.checkBox = convertView.findViewById(R.id.cb_file_select);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        MediaFile mediaFile = files.get(position);
        
        // Set file icon
        holder.iconImageView.setImageResource(mediaFile.getIconResource());
        
        // Set file name
        holder.nameTextView.setText(mediaFile.getDisplayName());
        
        // Set file details
        String details = getFileDetails(mediaFile);
        holder.detailsTextView.setText(details);
        
        // Set file size
        holder.sizeTextView.setText(mediaFile.getFormattedSize());
        
        // Set file date
        holder.dateTextView.setText(mediaFile.getFormattedDate());
        
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
     * Get formatted file details based on file type
     */
    private String getFileDetails(MediaFile mediaFile) {
        if (mediaFile.isDirectory()) {
            return "Folder";
        } else if (mediaFile.isVideo()) {
            return getVideoDetails(mediaFile);
        } else if (mediaFile.isAudio()) {
            return getAudioDetails(mediaFile);
        } else {
            return mediaFile.getExtension().toUpperCase() + " File";
        }
    }
    
    /**
     * Get video file details
     */
    private String getVideoDetails(MediaFile mediaFile) {
        StringBuilder details = new StringBuilder();
        
        if (!mediaFile.getResolution().equals("Unknown")) {
            details.append(mediaFile.getResolution());
        }
        
        if (mediaFile.getDuration() > 0) {
            if (details.length() > 0) details.append(", ");
            details.append(mediaFile.getFormattedDuration());
        }
        
        if (mediaFile.getBitrate() > 0) {
            if (details.length() > 0) details.append(", ");
            details.append(mediaFile.getBitrate()).append("k");
        }
        
        return details.length() > 0 ? details.toString() : "Video";
    }
    
    /**
     * Get audio file details
     */
    private String getAudioDetails(MediaFile mediaFile) {
        StringBuilder details = new StringBuilder();
        
        if (!mediaFile.getArtist().isEmpty()) {
            details.append(mediaFile.getArtist());
        }
        
        if (!mediaFile.getTitle().isEmpty()) {
            if (details.length() > 0) details.append(" - ");
            details.append(mediaFile.getTitle());
        } else if (details.length() == 0) {
            details.append("Audio");
        }
        
        if (mediaFile.getDuration() > 0) {
            if (details.length() > 0) details.append(" • ");
            details.append(mediaFile.getFormattedDuration());
        }
        
        return details.toString();
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
     * ViewHolder pattern for efficient list item recycling
     */
    private static class ViewHolder {
        ImageView iconImageView;
        TextView nameTextView;
        TextView detailsTextView;
        TextView sizeTextView;
        TextView dateTextView;
        CheckBox checkBox;
    }
}