/*
 * Copyright (c) 2024 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.history

import com.duckduckgo.app.history.store.HistoryDao
import com.duckduckgo.common.utils.CurrentTimeProvider
import io.reactivex.Single

interface HistoryRepository {
    fun getHistoryObservable(): Single<List<HistoryEntry>>
    fun saveToHistory(
        url: String,
        title: String?,
        query: String?,
        isSerp: Boolean,
    )
}

class RealHistoryRepository(
    private val historyDao: HistoryDao,
    private val currentTimeProvider: CurrentTimeProvider,
) : HistoryRepository {

    private var cachedHistoryEntries: List<HistoryEntry>? = null

    override fun getHistoryObservable(): Single<List<HistoryEntry>> {
        return if (cachedHistoryEntries != null) {
            Single.just(cachedHistoryEntries)
        } else {
            fetchAndCacheHistoryEntries()
        }
    }

    override fun saveToHistory(
        url: String,
        title: String?,
        query: String?,
        isSerp: Boolean,
    ) {
        historyDao.updateOrInsertVisit(url, title ?: "", query, isSerp, currentTimeProvider.currentTimeMillis())
        fetchAndCacheHistoryEntries()
    }
    private fun fetchAndCacheHistoryEntries(): Single<List<HistoryEntry>> {
        return historyDao.getHistoryEntriesWithVisits()
            .map { entries ->
                entries.map { it.toHistoryEntry() }.also {
                    cachedHistoryEntries = it
                }
            }
    }
}
