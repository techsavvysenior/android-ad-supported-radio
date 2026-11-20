package com.example.opendial.consent

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * Google's User Messaging Platform: asks for consent where the law requires it (EEA, UK,
 * Switzerland, and US states with their own rules) and writes the IAB TCF strings that the ads
 * SDK, the AppLovin adapter and AppsFlyer read. Runs on every launch, before anything else
 * that serves ads or measures installs.
 */
class ConsentManager(context: Context) {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    /** True when consent from a previous session already allows ad requests. */
    val canRequestAds: Boolean get() = info.canRequestAds()

    /** Whether the settings screen must offer a way to change the choice (required when it applies). */
    val privacyOptionsRequired: Boolean
        get() = info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gather(activity: Activity, onDone: (canRequestAds: Boolean, error: FormError?) -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    onDone(info.canRequestAds(), formError)
                }
            },
            { requestError -> onDone(info.canRequestAds(), requestError) },
        )
    }

    fun showPrivacyOptions(activity: Activity, onDismissed: (FormError?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDismissed(it) }
    }
}
