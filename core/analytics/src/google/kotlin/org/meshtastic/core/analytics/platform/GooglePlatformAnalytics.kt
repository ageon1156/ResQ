
package org.meshtastic.core.analytics.platform

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.compose.ExperimentalTrackingApi
import com.datadog.android.compose.NavigationViewTrackingEffect
import com.datadog.android.compose.enableComposeActionTracking
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.log.Logger
import com.datadog.android.log.Logs
import com.datadog.android.log.LogsConfiguration
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumConfiguration
import com.datadog.android.rum.tracking.AcceptAllNavDestinations
import com.datadog.android.sessionreplay.ImagePrivacy
import com.datadog.android.sessionreplay.SessionReplay
import com.datadog.android.sessionreplay.SessionReplayConfiguration
import com.datadog.android.sessionreplay.TextAndInputPrivacy
import com.datadog.android.sessionreplay.compose.ComposeExtensionSupport
import com.datadog.android.trace.Trace
import com.datadog.android.trace.TraceConfiguration
import com.datadog.android.trace.opentelemetry.DatadogOpenTelemetry
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.initialize
import dagger.hilt.android.qualifiers.ApplicationContext
import io.opentelemetry.api.GlobalOpenTelemetry
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.meshtastic.core.analytics.BuildConfig
import org.meshtastic.core.analytics.DataPair
import org.meshtastic.core.prefs.analytics.AnalyticsPrefs
import javax.inject.Inject
import co.touchlab.kermit.Logger as KermitLogger

class GooglePlatformAnalytics
@Inject
constructor(
    @ApplicationContext private val context: Context,
    analyticsPrefs: AnalyticsPrefs,
) : PlatformAnalytics {

    private val sampleRate = 100f.takeIf { BuildConfig.DEBUG } ?: 10f 

    private val isInTestLab: Boolean
        get() {
            val testLabSetting = Settings.System.getString(context.contentResolver, "firebase.test.lab")
            return "true" == testLabSetting
        }

    companion object {
        private const val TAG = "GooglePlatformAnalytics"
        private const val SERVICE_NAME = "org.meshtastic"
    }

    init {
        initDatadog(context as Application, analyticsPrefs)
        initCrashlytics(context, analyticsPrefs)

        val datadogLogger =
            Logger.Builder()
                .setService(SERVICE_NAME)
                .setNetworkInfoEnabled(true)
                .setRemoteSampleRate(sampleRate)
                .setBundleWithTraceEnabled(true)
                .setBundleWithRumEnabled(true)
                .build()
        val writers = buildList {
            add(DatadogLogWriter(datadogLogger))
            add(CrashlyticsLogWriter())
            if (BuildConfig.DEBUG) {
                add(co.touchlab.kermit.LogcatWriter())
            }
        }
        KermitLogger.setLogWriters(writers)
        KermitLogger.setMinSeverity(if (BuildConfig.DEBUG) Severity.Debug else Severity.Info)

        updateAnalyticsConsent(analyticsPrefs.analyticsAllowed)

        analyticsPrefs
            .getAnalyticsAllowedChangesFlow()
            .onEach { allowed -> updateAnalyticsConsent(allowed) }
            .launchIn(ProcessLifecycleOwner.get().lifecycleScope)
    }

    private fun initDatadog(application: Application, analyticsPrefs: AnalyticsPrefs) {
        val configuration =
            Configuration.Builder(
                clientToken = BuildConfig.datadogClientToken,
                env = if (BuildConfig.DEBUG) "debug" else "release",
                variant = BuildConfig.FLAVOR,
            )
                .useSite(DatadogSite.US5)
                .setCrashReportsEnabled(true)
                .setUseDeveloperModeWhenDebuggable(true)
                .build()
        
        Datadog.initialize(application, configuration, TrackingConsent.PENDING)
        Datadog.setUserInfo(analyticsPrefs.installId)
        Datadog.setVerbosity(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.WARN)

        val rumConfiguration =
            RumConfiguration.Builder(BuildConfig.datadogApplicationId)
                .trackAnonymousUser(true)
                .trackBackgroundEvents(true)
                .trackFrustrations(true)
                .trackLongTasks()
                .trackNonFatalAnrs(true)
                .trackUserInteractions()
                .setSessionSampleRate(sampleRate)
                .enableComposeActionTracking()
                .build()
        Rum.enable(rumConfiguration)

        val logsConfig = LogsConfiguration.Builder().build()
        Logs.enable(logsConfig)

        val traceConfig = TraceConfiguration.Builder().setNetworkInfoEnabled(true).build()
        Trace.enable(traceConfig)

        GlobalOpenTelemetry.set(DatadogOpenTelemetry(serviceName = SERVICE_NAME))

        val sessionReplayConfig =
            SessionReplayConfiguration.Builder(sampleRate = sampleRate)
                .setTextAndInputPrivacy(TextAndInputPrivacy.MASK_ALL)
                .setImagePrivacy(ImagePrivacy.MASK_ALL)
                .addExtensionSupport(ComposeExtensionSupport())
                .build()
        SessionReplay.enable(sessionReplayConfig)
    }

    private fun initCrashlytics(application: Application, analyticsPrefs: AnalyticsPrefs) {
        Firebase.initialize(application)
        Firebase.crashlytics.setUserId(analyticsPrefs.installId)
    }

    fun updateAnalyticsConsent(allowed: Boolean) {
        if (!isPlatformServicesAvailable || isInTestLab) {
            return
        }

        Datadog.setTrackingConsent(if (allowed) TrackingConsent.GRANTED else TrackingConsent.NOT_GRANTED)
        Firebase.crashlytics.isCrashlyticsCollectionEnabled = allowed
        Firebase.analytics.setAnalyticsCollectionEnabled(allowed)

        if (allowed) {
            Firebase.crashlytics.sendUnsentReports()
        }
    }

    override fun setDeviceAttributes(firmwareVersion: String, model: String) {
        
    }

    @Composable
    override fun AddNavigationTrackingEffect(navController: NavHostController) {
        
    }

    private val isGooglePlayAvailable: Boolean
        get() =
            GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context).let {
                it != ConnectionResult.SERVICE_MISSING && it != ConnectionResult.SERVICE_INVALID
            }

    private val isDatadogAvailable: Boolean
        get() = Datadog.isInitialized()

    override val isPlatformServicesAvailable: Boolean
        get() = false 

    private class CrashlyticsLogWriter : LogWriter() {
        companion object {
            private const val KEY_PRIORITY = "priority"
            private const val KEY_TAG = "tag"
            private const val KEY_MESSAGE = "message"
        }

        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            if (!Firebase.crashlytics.isCrashlyticsCollectionEnabled) return

            Firebase.crashlytics.setCustomKeys {
                key(KEY_PRIORITY, severity.ordinal)
                key(KEY_TAG, tag)
                key(KEY_MESSAGE, message)
            }

            if (throwable == null) {
                Firebase.crashlytics.recordException(Exception(message))
            } else {
                Firebase.crashlytics.recordException(throwable)
            }
        }
    }

    private class DatadogLogWriter(private val datadogLogger: Logger) : LogWriter() {
        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            val datadogPriority =
                when (severity) {
                    Severity.Verbose -> android.util.Log.VERBOSE
                    Severity.Debug -> android.util.Log.DEBUG
                    Severity.Info -> android.util.Log.INFO
                    Severity.Warn -> android.util.Log.WARN
                    Severity.Error -> android.util.Log.ERROR
                    Severity.Assert -> android.util.Log.ASSERT
                }
            datadogLogger.log(datadogPriority, message, throwable, mapOf("tag" to tag))
        }
    }

    private fun String.extractSemanticVersion(): String {
        val regex = "^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?".toRegex()
        val matchResult = regex.find(this)
        return matchResult?.groupValues?.drop(1)?.filter { it.isNotEmpty() }?.joinToString(".") ?: this
    }

    override fun track(event: String, vararg properties: DataPair) {
        
    }
}

