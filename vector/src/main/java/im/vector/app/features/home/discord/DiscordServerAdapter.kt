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
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.databinding.ItemDiscordServerBinding
import im.vector.app.features.avatars.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

data class DiscordServer(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val isSelected: Boolean = false,
    val unreadCount: Int = 0,
    val hasUnread: Boolean = false
)

class DiscordServerAdapter(
    private val avatarRenderer: AvatarRenderer,
    private val onServerClick: (DiscordServer) -> Unit
) : ListAdapter<DiscordServer, DiscordServerAdapter.ServerViewHolder>(ServerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val binding = ItemDiscordServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ServerViewHolder(private val binding: ItemDiscordServerBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(server: DiscordServer) {
            binding.discordServerIcon.setOnClickListener { onServerClick(server) }
            
            // Set selected state
            binding.discordServerIcon.isSelected = server.isSelected
            
            // Load avatar
            val matrixItem = MatrixItem.SpaceItem(server.id, server.name, server.avatar)
            avatarRenderer.render(matrixItem, binding.discordServerIcon)
            
            // Show unread indicator
            if (server.hasUnread) {
                binding.discordServerUnread.visibility = android.view.View.VISIBLE
                if (server.unreadCount > 0) {
                    binding.discordServerUnread.setTextOrHide(server.unreadCount.toString())
                } else {
                    binding.discordServerUnread.text = ""
                }
            } else {
                binding.discordServerUnread.visibility = android.view.View.GONE
            }
        }
    }

    private class ServerDiffCallback : DiffUtil.ItemCallback<DiscordServer>() {
        override fun areItemsTheSame(oldItem: DiscordServer, newItem: DiscordServer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DiscordServer, newItem: DiscordServer): Boolean {
            return oldItem == newItem
        }
    }
}