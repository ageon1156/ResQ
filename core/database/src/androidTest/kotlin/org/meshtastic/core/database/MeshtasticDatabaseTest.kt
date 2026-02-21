

package org.meshtastic.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MeshtasticDatabaseTest {

    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MeshtasticDatabase::class.java)

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        
        helper.createDatabase(TEST_DB, 3).apply { close() }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MeshtasticDatabase::class.java,
            TEST_DB,
        )
            .build()
            .apply { openHelper.writableDatabase.close() }
    }
}

