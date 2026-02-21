
package org.meshtastic.core.database

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.meshtastic.core.di.CoroutineDispatchers
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseManager @Inject constructor(private val app: Application, private val dispatchers: CoroutineDispatchers) {
    val prefs: SharedPreferences = app.getSharedPreferences("db-manager-prefs", Context.MODE_PRIVATE)
    private val managerScope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private val mutex = Mutex()

    private val _cacheLimit = MutableStateFlow(getCacheLimit())
    val cacheLimit: StateFlow<Int> = _cacheLimit

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == DatabaseConstants.CACHE_LIMIT_KEY) {
                _cacheLimit.value = getCacheLimit()
            }
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private val _currentDb = MutableStateFlow<MeshtasticDatabase?>(null)
    val currentDb: StateFlow<MeshtasticDatabase> =
        _currentDb.filterNotNull().stateIn(managerScope, SharingStarted.Eagerly, buildRoomDb(app, defaultDbName()))

    private val _currentAddress = MutableStateFlow<String?>(null)
    val currentAddress: StateFlow<String?> = _currentAddress

    private val dbCache = mutableMapOf<String, MeshtasticDatabase>() 

    suspend fun init(address: String?) {
        switchActiveDatabase(address)
    }

    suspend fun switchActiveDatabase(address: String?) = mutex.withLock {
        val dbName = buildDbName(address)

        val previousDbName = _currentDb.value?.openHelper?.databaseName

        if (_currentAddress.value == address && _currentDb.value != null) {
            markLastUsed(dbName)
            return@withLock
        }

        val db =
            dbCache[dbName]
                ?: withContext(dispatchers.io) { buildRoomDb(app, dbName) }.also { dbCache[dbName] = it }

        _currentDb.value = db
        _currentAddress.value = address
        markLastUsed(dbName)

        previousDbName?.let { markLastUsed(it) }

        managerScope.launch(dispatchers.io) { enforceCacheLimit(activeDbName = dbName) }

        managerScope.launch(dispatchers.io) { cleanupLegacyDbIfNeeded(activeDbName = dbName) }

        Logger.i { "Switched active DB to ${anonymizeDbName(dbName)} for address ${anonymizeAddress(address)}" }
    }

    inline fun <T> withDb(block: (MeshtasticDatabase) -> T): T = block(currentDb.value)

    private fun markLastUsed(dbName: String) {
        prefs.edit().putLong(lastUsedKey(dbName), System.currentTimeMillis()).apply()
    }

    private fun lastUsed(dbName: String): Long {
        val k = lastUsedKey(dbName)
        val v = prefs.getLong(k, 0L)
        return if (v == 0L) getDbFile(app, dbName)?.lastModified() ?: 0L else v
    }

    private fun listExistingDbNames(): List<String> {
        val base = app.getDatabasePath(DatabaseConstants.LEGACY_DB_NAME)
        val dir = base.parentFile ?: return emptyList()
        val names = dir.listFiles()?.mapNotNull { f -> f.name } ?: emptyList()
        return names
            .filter { it.startsWith(DatabaseConstants.DB_PREFIX) }
            .filterNot { it.endsWith("-wal") || it.endsWith("-shm") }
            .distinct()
    }

    private suspend fun enforceCacheLimit(activeDbName: String) = mutex.withLock {
        val limit = getCacheLimit()
        val all = listExistingDbNames()
        
        val deviceDbs =
            all.filterNot { it == DatabaseConstants.LEGACY_DB_NAME || it == DatabaseConstants.DEFAULT_DB_NAME }
        Logger.d {
            "LRU check: limit=$limit, active=${anonymizeDbName(
                activeDbName,
            )}, deviceDbs=${deviceDbs.joinToString(", ") {
                anonymizeDbName(it)
            }}"
        }
        if (deviceDbs.size <= limit) return@withLock
        val usageSnapshot = deviceDbs.associateWith { lastUsed(it) }
        Logger.d {
            "LRU lastUsed(ms): ${usageSnapshot.entries.joinToString(", ") { (name, ts) ->
                "${anonymizeDbName(name)}=$ts"
            }}"
        }
        val victims = selectEvictionVictims(deviceDbs, activeDbName, limit, usageSnapshot)
        Logger.i { "LRU victims: ${victims.joinToString(", ") { anonymizeDbName(it) }}" }
        victims.forEach { name ->
            runCatching { dbCache.remove(name)?.close() }
                .onFailure { Logger.w(it) { "Failed to close database $name" } }
            app.deleteDatabase(name)
            prefs.edit().remove(lastUsedKey(name)).apply()
            Logger.i { "Evicted cached DB ${anonymizeDbName(name)}" }
        }
    }

    fun getCacheLimit(): Int = prefs
        .getInt(DatabaseConstants.CACHE_LIMIT_KEY, DatabaseConstants.DEFAULT_CACHE_LIMIT)
        .coerceIn(DatabaseConstants.MIN_CACHE_LIMIT, DatabaseConstants.MAX_CACHE_LIMIT)

    fun setCacheLimit(limit: Int) {
        val clamped = limit.coerceIn(DatabaseConstants.MIN_CACHE_LIMIT, DatabaseConstants.MAX_CACHE_LIMIT)
        if (clamped == getCacheLimit()) return
        prefs.edit().putInt(DatabaseConstants.CACHE_LIMIT_KEY, clamped).apply()
        _cacheLimit.value = clamped
        
        val active = _currentDb.value?.openHelper?.databaseName ?: defaultDbName()
        managerScope.launch(dispatchers.io) { enforceCacheLimit(activeDbName = active) }
    }

    private suspend fun cleanupLegacyDbIfNeeded(activeDbName: String) = mutex.withLock {
        if (prefs.getBoolean(DatabaseConstants.LEGACY_DB_CLEANED_KEY, false)) return@withLock
        val legacy = DatabaseConstants.LEGACY_DB_NAME
        if (legacy == activeDbName) {
            
            prefs.edit().putBoolean(DatabaseConstants.LEGACY_DB_CLEANED_KEY, true).apply()
            return@withLock
        }
        val legacyFile = getDbFile(app, legacy)
        if (legacyFile != null) {
            runCatching { dbCache.remove(legacy)?.close() }
                .onFailure { Logger.w(it) { "Failed to close legacy database $legacy before deletion" } }
            val deleted = app.deleteDatabase(legacy)
            if (deleted) {
                Logger.i { "Deleted legacy DB ${anonymizeDbName(legacy)}" }
            } else {
                Logger.w { "Attempted to delete legacy DB $legacy but deleteDatabase returned false" }
            }
        }
        prefs.edit().putBoolean(DatabaseConstants.LEGACY_DB_CLEANED_KEY, true).apply()
    }
}

object DatabaseConstants {
    const val DB_PREFIX: String = "meshtastic_database"
    const val LEGACY_DB_NAME: String = DB_PREFIX
    const val DEFAULT_DB_NAME: String = "${DB_PREFIX}_default"

    const val CACHE_LIMIT_KEY: String = "node_db_cache_limit"
    const val DEFAULT_CACHE_LIMIT: Int = 3
    const val MIN_CACHE_LIMIT: Int = 1
    const val MAX_CACHE_LIMIT: Int = 10

    const val LEGACY_DB_CLEANED_KEY: String = "legacy_db_cleaned"

    const val DB_NAME_HASH_LEN: Int = 10
    const val DB_NAME_SEPARATOR_LEN: Int = 1
    const val DB_NAME_SUFFIX_LEN: Int = 3

    const val ADDRESS_ANON_SHORT_LEN: Int = 4
    const val ADDRESS_ANON_EDGE_LEN: Int = 2
}

private fun defaultDbName(): String = DatabaseConstants.DEFAULT_DB_NAME

private fun normalizeAddress(addr: String?): String {
    val u = addr?.trim()?.uppercase()
    val normalized =
        when {
            u.isNullOrBlank() -> "DEFAULT"
            u == "N" || u == "NULL" -> "DEFAULT"
            else -> u.replace(":", "")
        }
    return normalized
}

private fun shortSha1(s: String): String = MessageDigest.getInstance("SHA-1")
    .digest(s.toByteArray())
    .joinToString("") { "%02x".format(it) }
    .take(DatabaseConstants.DB_NAME_HASH_LEN)

private fun buildDbName(address: String?): String = if (address.isNullOrBlank()) {
    defaultDbName()
} else {
    "${DatabaseConstants.DB_PREFIX}_${shortSha1(normalizeAddress(address))}"
}

private fun lastUsedKey(dbName: String) = "db_last_used:$dbName"

private fun anonymizeAddress(address: String?): String = when {
    address == null -> "null"
    address.length <= DatabaseConstants.ADDRESS_ANON_SHORT_LEN -> address
    else ->
        address.take(DatabaseConstants.ADDRESS_ANON_EDGE_LEN) +
            "…" +
            address.takeLast(DatabaseConstants.ADDRESS_ANON_EDGE_LEN)
}

private fun anonymizeDbName(name: String): String =
    if (name == DatabaseConstants.LEGACY_DB_NAME || name == DatabaseConstants.DEFAULT_DB_NAME) {
        name
    } else {
        name.take(
            DatabaseConstants.DB_PREFIX.length +
                DatabaseConstants.DB_NAME_SEPARATOR_LEN +
                DatabaseConstants.DB_NAME_SUFFIX_LEN,
        ) + "…"
    }

private fun buildRoomDb(app: Application, dbName: String): MeshtasticDatabase =
    Room.databaseBuilder(app.applicationContext, MeshtasticDatabase::class.java, dbName)
        .fallbackToDestructiveMigration(false)
        .build()

private fun getDbFile(app: Application, dbName: String): File? = app.getDatabasePath(dbName).takeIf { it.exists() }

internal fun selectEvictionVictims(
    dbNames: List<String>,
    activeDbName: String,
    limit: Int,
    lastUsedMsByDb: Map<String, Long>,
): List<String> {
    val deviceDbNames =
        dbNames.filterNot { it == DatabaseConstants.LEGACY_DB_NAME || it == DatabaseConstants.DEFAULT_DB_NAME }
    val victims =
        if (limit < 1 || deviceDbNames.size <= limit) {
            emptyList()
        } else {
            val candidates = deviceDbNames.filter { it != activeDbName }
            if (candidates.isEmpty()) {
                emptyList()
            } else {
                val toEvict = deviceDbNames.size - limit
                candidates.sortedBy { lastUsedMsByDb[it] ?: 0L }.take(toEvict)
            }
        }
    return victims
}

