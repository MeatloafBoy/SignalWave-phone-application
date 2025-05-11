package com.example.signalwave

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.signalwave.model.Message
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(context: Context, resource: Int, messages: List<Message>, private val senderNames: Map<String, String>) :
    ArrayAdapter<Message>(context, resource, messages) {

    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        val message = getItem(position)

        // Determine the layout based on the sender (you'll need two layouts: one for your messages and one for others)
        val layoutResource = if (message?.senderId == currentUserUid) {
            R.layout.item_message_sent // Create this layout
        } else {
            R.layout.item_message_received
        }

        if (itemView == null || itemView.tag != layoutResource) {
            itemView = LayoutInflater.from(context).inflate(layoutResource, parent, false)
            itemView.tag = layoutResource // Tag the view with the layout resource used
        }

        val messageTextView: TextView = itemView!!.findViewById(R.id.text_message_body) // Create these IDs in your layouts
        val timeTextView: TextView = itemView.findViewById(R.id.text_message_time) // Create these IDs in your layouts
        val senderTextView: TextView? = itemView.findViewById(R.id.text_message_sender) // Optional, might not be needed in item_message_sent

        messageTextView.text = message?.text
        // Format the timestamp as needed
        timeTextView.text = message?.timestamp?.let { android.text.format.DateFormat.format("h:mm a", it) }

        // Only set sender for received messages
        if (message?.senderId != currentUserUid) {

            val senderName = senderNames[message?.senderId]

            if (senderName != null) {
                senderTextView?.text = senderName
                senderTextView?.visibility = View.VISIBLE // Make sure the TextView is visible
            } else {

                senderTextView?.text = message?.senderId // Fallback to ID
                senderTextView?.visibility = View.GONE // Hide if no name is available
            }
        } else {
            // For sent messages, hide the sender TextView if it exists
            senderTextView?.visibility = View.GONE
        }

        return itemView
    }
}