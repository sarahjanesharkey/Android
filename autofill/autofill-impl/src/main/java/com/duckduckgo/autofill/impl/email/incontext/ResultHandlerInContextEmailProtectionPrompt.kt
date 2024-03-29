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

package com.duckduckgo.autofill.impl.email.incontext

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import com.duckduckgo.app.di.AppCoroutineScope
import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.autofill.api.AutofillEventListener
import com.duckduckgo.autofill.api.AutofillFragmentResultsPlugin
import com.duckduckgo.autofill.api.AutofillUrlRequest
import com.duckduckgo.autofill.api.EmailProtectionInContextSignUpDialog
import com.duckduckgo.autofill.api.EmailProtectionInContextSignUpDialog.EmailProtectionInContextSignUpResult
import com.duckduckgo.autofill.api.EmailProtectionInContextSignUpDialog.EmailProtectionInContextSignUpResult.*
import com.duckduckgo.autofill.impl.email.incontext.store.EmailProtectionInContextDataStore
import com.duckduckgo.autofill.impl.jsbridge.AutofillMessagePoster
import com.duckduckgo.common.utils.DispatcherProvider
import com.duckduckgo.di.scopes.FragmentScope
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@ContributesMultibinding(FragmentScope::class)
class ResultHandlerInContextEmailProtectionPrompt @Inject constructor(
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val dataStore: EmailProtectionInContextDataStore,
    private val appBuildConfig: AppBuildConfig,
    private val messagePoster: AutofillMessagePoster,
) : AutofillFragmentResultsPlugin {
    override fun processResult(
        result: Bundle,
        context: Context,
        tabId: String,
        fragment: Fragment,
        autofillCallback: AutofillEventListener,
    ) {
        Timber.d("${this::class.java.simpleName}: processing result")

        val userSelection = result.safeGetParcelable<EmailProtectionInContextSignUpResult>(EmailProtectionInContextSignUpDialog.KEY_RESULT) ?: return
        val autofillUrlRequest = result.safeGetParcelable<AutofillUrlRequest>(EmailProtectionInContextSignUpDialog.KEY_URL) ?: return

        appCoroutineScope.launch(dispatchers.io()) {
            when (userSelection) {
                SignUp -> signUpSelected(autofillCallback, autofillUrlRequest)
                Cancel -> cancelled(autofillUrlRequest)
                DoNotShowAgain -> doNotAskAgain(autofillUrlRequest)
            }
        }
    }

    private suspend fun signUpSelected(
        autofillCallback: AutofillEventListener,
        autofillUrlRequest: AutofillUrlRequest,
    ) {
        withContext(dispatchers.main()) {
            autofillCallback.onSelectedToSignUpForInContextEmailProtection(autofillUrlRequest)
        }
    }

    private suspend fun doNotAskAgain(autofillUrlRequest: AutofillUrlRequest) {
        Timber.i("User selected to not show sign up for email protection again")
        dataStore.onUserChoseNeverAskAgain()
        notifyEndOfFlow(autofillUrlRequest)
    }

    private suspend fun cancelled(autofillUrlRequest: AutofillUrlRequest) {
        Timber.i("User cancelled sign up for email protection")
        notifyEndOfFlow(autofillUrlRequest)
    }

    private fun notifyEndOfFlow(autofillUrlRequest: AutofillUrlRequest) {
        val message = """
            {
                "success": {
                    "isSignedIn": false
                }
            }
        """.trimIndent()
        messagePoster.postMessage(message, autofillUrlRequest.requestId)
    }

    override fun resultKey(tabId: String): String {
        return EmailProtectionInContextSignUpDialog.resultKey(tabId)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("NewApi")
    private inline fun <reified T : Parcelable> Bundle.safeGetParcelable(key: String) =
        if (appBuildConfig.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(key, T::class.java)
        } else {
            getParcelable(key)
        }
}
