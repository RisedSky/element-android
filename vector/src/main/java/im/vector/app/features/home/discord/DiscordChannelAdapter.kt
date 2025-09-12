/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.discord

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.databinding.ItemDiscordChannelBinding

data class DiscordChannel(
    val id: String,
    val name: String,
    val type: ChannelType,
    val isSelected: Boolean = false,
    val unreadCount: Int = 0,
    val hasUnread: Boolean = false,
    val isCategory: Boolean = false,
    val isCollapsed: Boolean = false
)

enum class ChannelType {
    TEXT,
    VOICE,
    CATEGORY
}

class DiscordChannelAdapter(
    private val onChannelClick: (DiscordChannel) -> Unit
) : ListAdapter<DiscordChannel, DiscordChannelAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemDiscordChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChannelViewHolder(private val binding: ItemDiscordChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(channel: DiscordChannel) {
            binding.discordChannelContainer.setOnClickListener { onChannelClick(channel) }
            
            // Set selected state
            binding.discordChannelContainer.isSelected = channel.isSelected
            
            // Set channel name
            binding.discordChannelName.text = channel.name
            
            // Set channel icon based on type
            when (channel.type) {
                ChannelType.TEXT -> {
                    binding.discordChannelIcon.setImageResource(im.vector.app.R.drawable.ic_hashtag)
                    binding.discordChannelIcon.visibility = android.view.View.VISIBLE
                }
                ChannelType.VOICE -> {
                    binding.discordChannelIcon.setImageResource(im.vector.app.R.drawable.ic_voice_broadcast)
                    binding.discordChannelIcon.visibility = android.view.View.VISIBLE
                }
                ChannelType.CATEGORY -> {
                    binding.discordChannelIcon.setImageResource(im.vector.app.R.drawable.ic_arrow_drop_down)
                    binding.discordChannelIcon.visibility = android.view.View.VISIBLE
                    // Make category text bold and gray
                    binding.discordChannelName.setTextColor(binding.root.context.getColor(im.vector.app.R.color.discord_text_muted))
                    binding.discordChannelName.textSize = 12f
                    binding.discordChannelName.typeface = android.graphics.Typeface.DEFAULT_BOLD
                    binding.discordChannelName.text = channel.name.uppercase()
                }
            }
            
            // Show unread indicator
            if (channel.hasUnread && !channel.isCategory) {
                binding.discordChannelUnread.visibility = android.view.View.VISIBLE
                if (channel.unreadCount > 0) {
                    binding.discordChannelUnread.text = channel.unreadCount.toString()
                } else {
                    binding.discordChannelUnread.text = ""
                }
            } else {
                binding.discordChannelUnread.visibility = android.view.View.GONE
            }
            
            // Set proper padding for categories vs channels
            if (channel.isCategory) {
                binding.discordChannelContainer.setPaddingRelative(16, 8, 16, 4)
            } else {
                binding.discordChannelContainer.setPaddingRelative(32, 4, 16, 4)
            }
        }
    }

    private class ChannelDiffCallback : DiffUtil.ItemCallback<DiscordChannel>() {
        override fun areItemsTheSame(oldItem: DiscordChannel, newItem: DiscordChannel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DiscordChannel, newItem: DiscordChannel): Boolean {
            return oldItem == newItem
        }
    }
}