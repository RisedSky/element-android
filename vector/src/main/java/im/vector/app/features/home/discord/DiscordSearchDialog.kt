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
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.R
import im.vector.app.databinding.DialogDiscordSearchBinding

class DiscordSearchDialog(
    context: Context,
    private val onSearchQuery: (String) -> Unit,
    private val onItemClick: (SearchResult) -> Unit
) : Dialog(context, R.style.Theme_Vector_BottomSheet) {

    private lateinit var binding: DialogDiscordSearchBinding
    private lateinit var searchAdapter: DiscordSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogDiscordSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupSearchInput()
        setupRecyclerView()
        setupMockData()
    }

    private fun setupSearchInput() {
        binding.discordSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                onSearchQuery(query)
                filterResults(query)
            }
        })
        
        binding.discordSearchClose.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = DiscordSearchAdapter { result ->
            onItemClick(result)
            dismiss()
        }
        
        binding.discordSearchResults.layoutManager = LinearLayoutManager(context)
        binding.discordSearchResults.adapter = searchAdapter
    }

    private fun setupMockData() {
        // Add some mock search results
        val mockResults = listOf(
            SearchResult("1", "general", SearchType.CHANNEL, "#general"),
            SearchResult("2", "announcements", SearchType.CHANNEL, "#announcements"), 
            SearchResult("3", "John Doe", SearchType.USER, "@john.doe"),
            SearchResult("4", "Jane Smith", SearchType.USER, "@jane.smith"),
            SearchResult("5", "Welcome to the server", SearchType.MESSAGE, "Message in #general"),
            SearchResult("6", "Project discussion", SearchType.MESSAGE, "Message in #dev")
        )
        searchAdapter.updateResults(mockResults)
    }

    private fun filterResults(query: String) {
        // Simple filtering - in real app would search actual data
        val allResults = listOf(
            SearchResult("1", "general", SearchType.CHANNEL, "#general"),
            SearchResult("2", "announcements", SearchType.CHANNEL, "#announcements"), 
            SearchResult("3", "John Doe", SearchType.USER, "@john.doe"),
            SearchResult("4", "Jane Smith", SearchType.USER, "@jane.smith"),
            SearchResult("5", "Welcome to the server", SearchType.MESSAGE, "Message in #general"),
            SearchResult("6", "Project discussion", SearchType.MESSAGE, "Message in #dev")
        )
        
        val filtered = if (query.isEmpty()) {
            allResults
        } else {
            allResults.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true)
            }
        }
        
        searchAdapter.updateResults(filtered)
    }
}