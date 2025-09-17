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
import im.vector.app.R
import im.vector.app.databinding.DialogDiscordAddServerBinding

class DiscordAddServerDialog(
    context: Context,
    private val onJoinServer: (String) -> Unit,
    private val onCreateServer: () -> Unit
) : Dialog(context, R.style.Theme_Vector_BottomSheet) {

    private lateinit var binding: DialogDiscordAddServerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogDiscordAddServerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.discordAddServerClose.setOnClickListener {
            dismiss()
        }
        
        binding.discordJoinServerOption.setOnClickListener {
            showJoinServerInput()
        }
        
        binding.discordCreateServerOption.setOnClickListener {
            onCreateServer()
            dismiss()
        }
        
        binding.discordJoinServerButton.setOnClickListener {
            val inviteCode = binding.discordServerInviteInput.text.toString().trim()
            if (inviteCode.isNotEmpty()) {
                onJoinServer(inviteCode)
                dismiss()
            }
        }
        
        binding.discordJoinServerCancel.setOnClickListener {
            hideJoinServerInput()
        }
    }
    
    private fun showJoinServerInput() {
        binding.discordAddServerOptions.visibility = android.view.View.GONE
        binding.discordJoinServerInput.visibility = android.view.View.VISIBLE
        binding.discordJoinServerButtons.visibility = android.view.View.VISIBLE
        binding.discordServerInviteInput.requestFocus()
    }
    
    private fun hideJoinServerInput() {
        binding.discordAddServerOptions.visibility = android.view.View.VISIBLE
        binding.discordJoinServerInput.visibility = android.view.View.GONE
        binding.discordJoinServerButtons.visibility = android.view.View.GONE
        binding.discordServerInviteInput.text?.clear()
    }
}