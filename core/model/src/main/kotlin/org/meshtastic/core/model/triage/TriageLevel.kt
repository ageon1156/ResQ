package org.meshtastic.core.model.triage

enum class TriageLevel(
    val priority: Int,
    val displayLabel: String,
    val symbol: String,
) {
    BLACK(4, "Deceased / Expectant", "⚫"),
    RED(3, "Immediate", "🔴"),
    YELLOW(2, "Delayed", "🟡"),
    GREEN(1, "Minor", "🟢"),
    ;

    fun mergeWith(other: TriageLevel): TriageLevel =
        if (other.priority > this.priority) other else this

    companion object {
        fun fromString(value: String): TriageLevel =
            entries.firstOrNull { it.name == value } ?: RED
    }
}
