/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.discord

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import im.vector.app.R
import im.vector.app.databinding.DialogDiscordMembersBinding

data class DiscordMember(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val status: MemberStatus = MemberStatus.OFFLINE,
    val role: String? = null
)

enum class MemberStatus {
    ONLINE,
    AWAY,
    BUSY,
    OFFLINE
}

class DiscordMembersDialog(
    context: Context,
    private val onMemberClick: (DiscordMember) -> Unit
) : Dialog(context, R.style.Theme_Vector_BottomSheet) {

    private lateinit var binding: DialogDiscordMembersBinding
    private lateinit var membersAdapter: DiscordMembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogDiscordMembersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupMockData()
        
        binding.discordMembersClose.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        membersAdapter = DiscordMembersAdapter { member ->
            onMemberClick(member)
        }
        
        binding.discordMembersList.layoutManager = LinearLayoutManager(context)
        binding.discordMembersList.adapter = membersAdapter
    }

    private fun setupMockData() {
        // Add some mock members data
        val mockMembers = listOf(
            DiscordMember("1", "John Doe", null, MemberStatus.ONLINE, "Admin"),
            DiscordMember("2", "Jane Smith", null, MemberStatus.AWAY, "Moderator"),
            DiscordMember("3", "Bob Wilson", null, MemberStatus.ONLINE, null),
            DiscordMember("4", "Alice Brown", null, MemberStatus.BUSY, null),
            DiscordMember("5", "Charlie Davis", null, MemberStatus.OFFLINE, null),
            DiscordMember("6", "Eve Miller", null, MemberStatus.ONLINE, null)
        )
        
        // Group by status
        val onlineMembers = mockMembers.filter { it.status == MemberStatus.ONLINE }
        val awayMembers = mockMembers.filter { it.status == MemberStatus.AWAY }
        val busyMembers = mockMembers.filter { it.status == MemberStatus.BUSY }
        val offlineMembers = mockMembers.filter { it.status == MemberStatus.OFFLINE }
        
        val groupedMembers = mutableListOf<DiscordMemberItem>()
        
        if (onlineMembers.isNotEmpty()) {
            groupedMembers.add(DiscordMemberItem.Header("Online — ${onlineMembers.size}"))
            onlineMembers.forEach { groupedMembers.add(DiscordMemberItem.Member(it)) }
        }
        
        if (awayMembers.isNotEmpty()) {
            groupedMembers.add(DiscordMemberItem.Header("Away — ${awayMembers.size}"))
            awayMembers.forEach { groupedMembers.add(DiscordMemberItem.Member(it)) }
        }
        
        if (busyMembers.isNotEmpty()) {
            groupedMembers.add(DiscordMemberItem.Header("Do Not Disturb — ${busyMembers.size}"))
            busyMembers.forEach { groupedMembers.add(DiscordMemberItem.Member(it)) }
        }
        
        if (offlineMembers.isNotEmpty()) {
            groupedMembers.add(DiscordMemberItem.Header("Offline — ${offlineMembers.size}"))
            offlineMembers.forEach { groupedMembers.add(DiscordMemberItem.Member(it)) }
        }
        
        membersAdapter.updateMembers(groupedMembers)
    }
}