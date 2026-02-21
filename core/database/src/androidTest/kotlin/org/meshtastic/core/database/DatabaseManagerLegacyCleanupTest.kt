
package org.meshtastic.core.database

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.meshtastic.core.di.CoroutineDispatchers

@RunWith(AndroidJUnit4::class)
class DatabaseManagerLegacyCleanupTest {
    @Test
    fun deletes_legacy_db_on_switch_when_flag_not_set() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("db-manager-prefs", Context.MODE_PRIVATE)

        prefs.edit().remove(DatabaseConstants.LEGACY_DB_CLEANED_KEY).apply()

        val legacyName = DatabaseConstants.LEGACY_DB_NAME
        val legacyFile = app.getDatabasePath(legacyName)
        
        app.openOrCreateDatabase(legacyName, Context.MODE_PRIVATE, null).close()
        assertTrue("Precondition: legacy DB should exist before switch", legacyFile.exists())

        val testDispatchers =
            CoroutineDispatchers(io = Dispatchers.IO, main = Dispatchers.Main, default = Dispatchers.Default)
        val manager = DatabaseManager(app, testDispatchers)

        manager.switchActiveDatabase("01:23:45:67:89:AB")

        var attempts = 0
        while (legacyFile.exists() && attempts < 20) {
            delay(100)
            attempts++
        }

        assertFalse("Legacy DB should be deleted after switch", legacyFile.exists())
    }
}

