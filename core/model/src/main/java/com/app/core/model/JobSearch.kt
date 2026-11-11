package com.app.core.model

data class JobSearch(val query: String = "", val location: String = "") {
    fun normalized(): JobSearch = copy(query = query.trim(), location = location.trim())
}

data class JobPage(val jobs: List<JobOpening>, val page: Int, val hasMore: Boolean)
