

package org.meshtastic.feature.node.list

import kotlinx.coroutines.flow.map
import org.meshtastic.core.database.model.NodeSortOption
import org.meshtastic.core.datastore.UiPreferencesDataSource
import javax.inject.Inject

class NodeFilterPreferences @Inject constructor(private val uiPreferencesDataSource: UiPreferencesDataSource) {
    val includeUnknown = uiPreferencesDataSource.includeUnknown
    val excludeInfrastructure = uiPreferencesDataSource.excludeInfrastructure
    val onlyOnline = uiPreferencesDataSource.onlyOnline
    val onlyDirect = uiPreferencesDataSource.onlyDirect
    val showIgnored = uiPreferencesDataSource.showIgnored

    val nodeSortOption =
        uiPreferencesDataSource.nodeSort.map { NodeSortOption.entries.getOrElse(it) { NodeSortOption.VIA_FAVORITE } }

    fun setNodeSort(option: NodeSortOption) {
        uiPreferencesDataSource.setNodeSort(option.ordinal)
    }

    fun toggleIncludeUnknown() {
        uiPreferencesDataSource.setIncludeUnknown(!includeUnknown.value)
    }

    fun toggleExcludeInfrastructure() {
        uiPreferencesDataSource.setExcludeInfrastructure(!excludeInfrastructure.value)
    }

    fun toggleOnlyOnline() {
        uiPreferencesDataSource.setOnlyOnline(!onlyOnline.value)
    }

    fun toggleOnlyDirect() {
        uiPreferencesDataSource.setOnlyDirect(!onlyDirect.value)
    }

    fun toggleShowIgnored() {
        uiPreferencesDataSource.setShowIgnored(!showIgnored.value)
    }
}

