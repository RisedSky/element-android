/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.discord

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.databinding.ItemDiscordMemberBinding
import im.vector.app.databinding.ItemDiscordMemberHeaderBinding

sealed class DiscordMemberItem {
    data class Header(val title: String) : DiscordMemberItem()
    data class Member(val member: DiscordMember) : DiscordMemberItem()
}

class DiscordMembersAdapter(
    private val onMemberClick: (DiscordMember) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = listOf<DiscordMemberItem>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_MEMBER = 1
    }

    fun updateMembers(newItems: List<DiscordMemberItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DiscordMemberItem.Header -> TYPE_HEADER
            is DiscordMemberItem.Member -> TYPE_MEMBER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemDiscordMemberHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
            TYPE_MEMBER -> {
                val binding = ItemDiscordMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MemberViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DiscordMemberItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is DiscordMemberItem.Member -> (holder as MemberViewHolder).bind(item.member)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(private val binding: ItemDiscordMemberHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.memberHeaderTitle.text = title
        }
    }

    inner class MemberViewHolder(private val binding: ItemDiscordMemberBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(member: DiscordMember) {
            binding.memberName.text = member.name
            binding.memberRole.text = member.role ?: ""
            binding.memberRole.visibility = if (member.role != null) android.view.View.VISIBLE else android.view.View.GONE
            
            // Set status indicator
            val statusColor = when (member.status) {
                MemberStatus.ONLINE -> im.vector.app.R.color.discord_status_online
                MemberStatus.AWAY -> im.vector.app.R.color.discord_status_away
                MemberStatus.BUSY -> im.vector.app.R.color.discord_status_busy
                MemberStatus.OFFLINE -> im.vector.app.R.color.discord_text_muted
            }
            
            binding.memberStatus.setColorFilter(binding.root.context.getColor(statusColor))
            
            // Set avatar placeholder
            binding.memberAvatar.setImageResource(im.vector.app.R.drawable.ic_person)
            
            binding.root.setOnClickListener { onMemberClick(member) }
        }
    }
}