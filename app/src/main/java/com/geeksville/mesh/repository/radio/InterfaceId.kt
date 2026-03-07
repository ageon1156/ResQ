

package com.geeksville.mesh.repository.radio

enum class InterfaceId(val id: Char) {
    BLUETOOTH('x'),
    NOP('n'),
    SERIAL('s'),
    TCP('t'),
    ;

    companion object {
        fun forIdChar(id: Char): InterfaceId? {
            return entries.firstOrNull { it.id == id }
        }
    }
}
