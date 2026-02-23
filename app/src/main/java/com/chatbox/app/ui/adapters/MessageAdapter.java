package com.chatbox.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chatbox.app.R;
import com.chatbox.app.data.entity.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MessageAdapter - RecyclerView adapter for message list
 * 
 * This adapter displays chat messages in a conversation format.
 * Different view types are used for user and assistant messages.
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    /**
     * View type for user messages
     */
    private static final int VIEW_TYPE_USER = 1;
    
    /**
     * View type for assistant messages
     */
    private static final int VIEW_TYPE_ASSISTANT = 2;
    
    /**
     * List of messages
     */
    private List<Message> messages;
    
    /**
     * Click listener
     */
    private OnMessageClickListener listener;
    
    /**
     * Date format for timestamps
     */
    private final SimpleDateFormat dateFormat;
    
    /**
     * Constructor
     * 
     * @param listener Click listener
     * @param messages Initial message list
     */
    public MessageAdapter(OnMessageClickListener listener, List<Message> messages) {
        this.listener = listener;
        this.messages = messages;
        this.dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    /**
     * Update the message list
     * 
     * @param newMessages New message list
     */
    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }
    
    /**
     * Set the click listener
     * 
     * @param listener Click listener
     */
    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.listener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.isUser()) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_ASSISTANT;
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_assistant, parent, false);
            return new AssistantMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AssistantMessageViewHolder) {
            ((AssistantMessageViewHolder) holder).bind(message);
        }
    }
    
    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }
    
    // =========================================================================
    // ViewHolders
    // =========================================================================
    
    /**
     * ViewHolder for user messages
     */
    class UserMessageViewHolder extends RecyclerView.ViewHolder {
        
        private final TextView textContent;
        private final TextView textTime;
        
        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textContent = itemView.findViewById(R.id.text_message_content);
            textTime = itemView.findViewById(R.id.text_message_time);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageClick(messages.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageLongClick(messages.get(position), itemView);
                    return true;
                }
                return false;
            });
        }
        
        void bind(Message message) {
            textContent.setText(message.getContent());
            textTime.setText(formatTime(message.getTimestamp()));
        }
    }
    
    /**
     * ViewHolder for assistant messages
     */
    class AssistantMessageViewHolder extends RecyclerView.ViewHolder {
        
        private final TextView textContent;
        private final TextView textTime;
        private final View progressGenerating;
        
        AssistantMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textContent = itemView.findViewById(R.id.text_message_content);
            textTime = itemView.findViewById(R.id.text_message_time);
            progressGenerating = itemView.findViewById(R.id.progress_generating);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageClick(messages.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageLongClick(messages.get(position), itemView);
                    return true;
                }
                return false;
            });
        }
        
        void bind(Message message) {
            textContent.setText(message.getContent());
            textTime.setText(formatTime(message.getTimestamp()));
            
            // Show generating indicator
            if (message.isGenerating()) {
                progressGenerating.setVisibility(View.VISIBLE);
            } else {
                progressGenerating.setVisibility(View.GONE);
            }
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
    
    // =========================================================================
    // Interface
    // =========================================================================
    
    /**
     * Interface for message click events
     */
    public interface OnMessageClickListener {
        /**
         * Called when a message is clicked
         * 
         * @param message The clicked message
         */
        void onMessageClick(Message message);
        
        /**
         * Called when a message is long-clicked
         * 
         * @param message The clicked message
         * @param anchor The anchor view for popup menu
         */
        void onMessageLongClick(Message message, View anchor);
    }
}
