

package com.geeksville.mesh.ui

import com.geeksville.mesh.model.getInitials
import org.junit.Assert.assertEquals
import org.junit.Test

class UIUnitTest {
    @Test
    fun initialsGood() {
        assertEquals("KH", getInitials("Kevin Hester"))
        assertEquals("KHLC", getInitials("  Kevin Hester Lesser Cat  "))
        assertEquals("", getInitials("  "))
        assertEquals("gksv", getInitials("geeksville"))
        assertEquals("geek", getInitials("geek"))
        assertEquals("gks1", getInitials("geeks1"))
    }

    @Test
    fun ignoreEmojisWhenCreatingInitials() {
        assertEquals("TG", getInitials("The \uD83D\uDC10 Goat"))
        assertEquals("TT", getInitials("The \uD83E\uDD14Thinker"))
        assertEquals("TCH", getInitials("\uD83D\uDC4F\uD83C\uDFFFThe Clapping Hands"))
        assertEquals("山羊", getInitials("山羊\uD83D\uDC10"))
    }
}

