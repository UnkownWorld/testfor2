package com.chatbox.app.ui.adapters;

import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chatbox.app.R;
import com.chatbox.app.data.entity.Session;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SessionAdapter - RecyclerView adapter for session list
 * 
 * This adapter displays chat sessions in a list format.
 * Each item shows:
 * - Session name
 * - Last message preview
 * - Timestamp
 * - Star status
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {
    
    /**
     * List of sessions to display
     */
    private List<Session> sessions;
    
    /**
     * Click listener for session items
     */
    private OnSessionClickListener listener;
    
    /**
     * Date format for timestamps
     */
    private final SimpleDateFormat dateFormat;
    
    /**
     * Constructor
     * 
     * @param listener Click listener
     * @param sessions Initial session list
     */
    public SessionAdapter(OnSessionClickListener listener, List<Session> sessions) {
        this.listener = listener;
        this.sessions = sessions;
        this.dateFormat = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
    }
    
    /**
     * Update the session list
     * 
     * @param newSessions New session list
     */
    public void updateSessions(List<Session> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }
    
    /**
     * Set the click listener
     * 
     * @param listener Click listener
     */
    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session session = sessions.get(position);
        holder.bind(session);
    }
    
    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }
    
    /**
     * Get session at position
     * 
     * @param position Position
     * @return Session at position
     */
    public Session getSession(int position) {
        if (sessions != null && position >= 0 && position < sessions.size()) {
            return sessions.get(position);
        }
        return null;
    }
    
    // =========================================================================
    // ViewHolder
    // =========================================================================
    
    /**
     * ViewHolder for session items
     */
    class SessionViewHolder extends RecyclerView.ViewHolder {
        
        private final TextView textName;
        private final TextView textPreview;
        private final TextView textTime;
        private final ImageView imageStar;
        private final ImageButton buttonMore;
        
        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_session_name);
            textPreview = itemView.findViewById(R.id.text_session_preview);
            textTime = itemView.findViewById(R.id.text_session_time);
            imageStar = itemView.findViewById(R.id.image_star);
            buttonMore = itemView.findViewById(R.id.button_more);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSessionClick(sessions.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSessionLongClick(sessions.get(position), buttonMore);
                    return true;
                }
                return false;
            });
            
            buttonMore.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSessionLongClick(sessions.get(position), buttonMore);
                }
            });
            
            imageStar.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStarClick(sessions.get(position));
                }
            });
        }
        
        /**
         * Bind session data to views
         * 
         * @param session The session to display
         */
        void bind(Session session) {
            // Set name
            textName.setText(session.getDisplayTitle());
            
            // Set preview (last message or empty)
            String preview = session.getLastMessageSummary();
            if (preview.isEmpty()) {
                textPreview.setText(R.string.no_messages_yet);
                textPreview.setAlpha(0.5f);
            } else {
                textPreview.setText(preview);
                textPreview.setAlpha(1.0f);
            }
            
            // Set time
            textTime.setText(formatTime(session.getUpdatedAt()));
            
            // Set star icon
            if (session.isStarred()) {
                imageStar.setImageResource(R.drawable.ic_star_filled);
                imageStar.setAlpha(1.0f);
            } else {
                imageStar.setImageResource(R.drawable.ic_star_outline);
                imageStar.setAlpha(0.3f);
            }
        }
        
        /**
         * Format timestamp for display
         * 
         * @param timestamp Timestamp in milliseconds
         * @return Formatted string
         */
        private String formatTime(long timestamp) {
            Date date = new Date(timestamp);
            return dateFormat.format(date);
        }
    }
    
    // =========================================================================
    // Interfaces
    // =========================================================================
    
    /**
     * Interface for session click events
     */
    public interface OnSessionClickListener {
        /**
         * Called when a session is clicked
         * 
         * @param session The clicked session
         */
        void onSessionClick(Session session);
        
        /**
         * Called when a session is long-clicked
         * 
         * @param session The clicked session
         * @param anchor The anchor view for popup menu
         */
        void onSessionLongClick(Session session, View anchor);
        
        /**
         * Called when the star button is clicked
         * 
         * @param session The session
         */
        void onStarClick(Session session);
    }
    
    // =========================================================================
    // Item Decoration
    // =========================================================================
    
    /**
     * Item decoration for spacing between session items
     */
    public static class SessionItemDecoration extends RecyclerView.ItemDecoration {
        
        private final int margin;
        
        public SessionItemDecoration(int margin) {
            this.margin = margin;
        }
        
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, 
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.left = margin;
            outRect.right = margin;
            outRect.top = margin / 2;
            outRect.bottom = margin / 2;
        }
    }
}
