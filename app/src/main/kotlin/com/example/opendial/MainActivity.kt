package com.example.opendial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.opendial.ui.OpenDialRoot
import com.example.opendial.ui.OpenDialViewModel
import com.example.opendial.ui.theme.OpenDialTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: OpenDialViewModel by viewModels { OpenDialViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val graph = (application as OpenDialApp).graph

        if (savedInstanceState == null) {
            // AppsFlyer is initialized on every cold start but only *started* through the gate.
            if (graph.appsFlyerConfigured) {
                graph.analytics.initAppsFlyer(BuildConfig.APPSFLYER_DEV_KEY) {
                    graph.scope.launch { graph.run(graph.gate.onAppsFlyerSessionReady()) }
                }
            }
            // Consent from last session can start ads at once; the form below may update it.
            if (graph.consent.canRequestAds) graph.run(graph.gate.onConsentResolved(true))
            graph.consent.gather(this) { canRequestAds, _ ->
                graph.run(graph.gate.onConsentResolved(canRequestAds))
                viewModel.onConsentUpdated()
            }
        }

        setContent {
            OpenDialTheme {
                OpenDialRoot(viewModel = viewModel, activity = this)
            }
        }
    }
}
