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
import im.vector.app.databinding.ItemDiscordSearchResultBinding

data class SearchResult(
    val id: String,
    val name: String,
    val type: SearchType,
    val description: String
)

enum class SearchType {
    CHANNEL,
    USER,
    MESSAGE
}

class DiscordSearchAdapter(
    private val onItemClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<DiscordSearchAdapter.SearchViewHolder>() {

    private var results = listOf<SearchResult>()

    fun updateResults(newResults: List<SearchResult>) {
        results = newResults
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemDiscordSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int = results.size

    inner class SearchViewHolder(private val binding: ItemDiscordSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(result: SearchResult) {
            binding.searchResultName.text = result.name
            binding.searchResultDescription.text = result.description
            
            // Set icon based on type
            when (result.type) {
                SearchType.CHANNEL -> {
                    binding.searchResultIcon.setImageResource(im.vector.app.R.drawable.ic_hashtag)
                }
                SearchType.USER -> {
                    binding.searchResultIcon.setImageResource(im.vector.app.R.drawable.ic_person)
                }
                SearchType.MESSAGE -> {
                    binding.searchResultIcon.setImageResource(im.vector.app.R.drawable.ic_chat)
                }
            }
            
            binding.root.setOnClickListener { onItemClick(result) }
        }
    }
}