

package org.meshtastic.core.model.util

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChannelSetTest {

    @Test
    fun matchPython() {
        val url = Uri.parse("https://meshtastic.org/e/#CgMSAQESBggBQANIAQ")
        val cs = url.toChannelSet()
        Assert.assertEquals("LongFast", cs.primaryChannel!!.name)
        Assert.assertEquals(url, cs.getChannelUrl(false))
    }

    @Test
    fun parseCaseInsensitive() {
        var url = Uri.parse("HTTPS://MESHTASTIC.ORG/E/#CgMSAQESBggBQANIAQ")
        Assert.assertEquals("LongFast", url.toChannelSet().primaryChannel!!.name)

        url = Uri.parse("HTTPS://mEsHtAsTiC.OrG/e/#CgMSAQESBggBQANIAQ")
        Assert.assertEquals("LongFast", url.toChannelSet().primaryChannel!!.name)
    }

    @Test
    fun handleAddInFragment() {
        val url = Uri.parse("https://meshtastic.org/e/#CgMSAQESBggBQANIAQ?add=true")
        val cs = url.toChannelSet()
        Assert.assertEquals("Custom", cs.primaryChannel!!.name)
        Assert.assertFalse(cs.hasLoraConfig())
    }

    @Test
    fun handleAddInQueryParams() {
        val url = Uri.parse("https://meshtastic.org/e/?add=true#CgMSAQESBggBQANIAQ")
        val cs = url.toChannelSet()
        Assert.assertEquals("Custom", cs.primaryChannel!!.name)
        Assert.assertFalse(cs.hasLoraConfig())
    }
}

