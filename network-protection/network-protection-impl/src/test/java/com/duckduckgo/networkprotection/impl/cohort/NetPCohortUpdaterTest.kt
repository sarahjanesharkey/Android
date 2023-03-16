/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.networkprotection.impl.cohort

import com.duckduckgo.app.CoroutineTestRule
import com.duckduckgo.mobile.android.vpn.VpnFeaturesRegistry
import com.duckduckgo.networkprotection.impl.NetPVpnFeature
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.threeten.bp.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class NetPCohortUpdaterTest {
    @get:Rule
    var coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var vpnFeaturesRegistry: VpnFeaturesRegistry

    @Mock
    private lateinit var cohortStore: NetpCohortStore
    private lateinit var testee: NetPCohortUpdater

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testee = NetPCohortUpdater(vpnFeaturesRegistry, cohortStore)
    }

    @Test
    fun whenNetPIsNotRegisteredThenDoNothingWithCohort() {
        whenever(vpnFeaturesRegistry.isFeatureRegistered(NetPVpnFeature.NETP_VPN)).thenReturn(false)

        testee.onVpnStarted(coroutineRule.testScope)

        verifyNoInteractions(cohortStore)
    }

    @Test
    fun whenNetPIsRegisteredAndCohortNotSetThenUpdateCohort() {
        whenever(vpnFeaturesRegistry.isFeatureRegistered(NetPVpnFeature.NETP_VPN)).thenReturn(true)
        whenever(cohortStore.cohortLocalDate).thenReturn(null)

        testee.onVpnStarted(coroutineRule.testScope)

        verify(cohortStore).cohortLocalDate = any()
    }

    @Test
    fun whenNetPIsRegisteredAndCohortetThenDoNothingWithCohort() {
        whenever(vpnFeaturesRegistry.isFeatureRegistered(NetPVpnFeature.NETP_VPN)).thenReturn(true)
        whenever(cohortStore.cohortLocalDate).thenReturn(LocalDate.of(2023, 1, 1))

        testee.onVpnStarted(coroutineRule.testScope)

        verify(cohortStore).cohortLocalDate
        verifyNoMoreInteractions(cohortStore)
    }
}