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

package com.duckduckgo.app.browser.certificates

import com.duckduckgo.di.scopes.AppScope
import dagger.SingleInstanceIn
import javax.inject.Inject

interface TrustedSitesRepository {

    fun add(domain: String)

    fun contains(domain: String): Boolean
}

@SingleInstanceIn(AppScope::class)
class RealTrustedSitesRepository @Inject constructor() : TrustedSitesRepository {

    private val trustedSites: MutableList<String> = mutableListOf()
    override fun add(domain: String) {
        trustedSites.add(domain)
    }

    override fun contains(domain: String): Boolean {
        return trustedSites.contains(domain)
    }
}