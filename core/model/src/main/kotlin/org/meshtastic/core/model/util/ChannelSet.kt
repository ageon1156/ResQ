

package org.meshtastic.core.model.util

import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import co.touchlab.kermit.Logger
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.meshtastic.core.model.Channel
import org.meshtastic.proto.AppOnlyProtos.ChannelSet
import java.net.MalformedURLException

private const val MESHTASTIC_HOST = "meshtastic.org"
private const val CHANNEL_PATH = "/e/"
const val URL_PREFIX = "https://$MESHTASTIC_HOST$CHANNEL_PATH"
private const val BASE64FLAGS = Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING

@Throws(MalformedURLException::class)
fun Uri.toChannelSet(): ChannelSet {
    if (fragment.isNullOrBlank() || !host.equals(MESHTASTIC_HOST, true) || !path.equals(CHANNEL_PATH, true)) {
        throw MalformedURLException("Not a valid Meshtastic URL: ${toString().take(40)}")
    }

    val url = ChannelSet.parseFrom(Base64.decode(fragment.orEmpty().substringBefore('?'), BASE64FLAGS))
    val shouldAdd =
        fragment?.substringAfter('?', "")?.takeUnless { it.isBlank() }?.equals("add=true")
            ?: getBooleanQueryParameter("add", false)

    return url.toBuilder().apply { if (shouldAdd) clearLoraConfig() }.build()
}

val ChannelSet.subscribeList: List<String>
    get() = settingsList.filter { it.downlinkEnabled }.map { Channel(it, loraConfig).name }

fun ChannelSet.getChannel(index: Int): Channel? =
    if (settingsCount > index) Channel(getSettings(index), loraConfig) else null

val ChannelSet.primaryChannel: Channel?
    get() = getChannel(0)

fun ChannelSet.getChannelUrl(upperCasePrefix: Boolean = false, shouldAdd: Boolean = false): Uri {
    val channelBytes = this.toByteArray() ?: ByteArray(0) 
    val enc = Base64.encodeToString(channelBytes, BASE64FLAGS)
    val p = if (upperCasePrefix) URL_PREFIX.uppercase() else URL_PREFIX
    val query = if (shouldAdd) "?add=true" else ""
    return Uri.parse("$p$query#$enc")
}

fun ChannelSet.qrCode(shouldAdd: Boolean): Bitmap? = try {
    val multiFormatWriter = MultiFormatWriter()
    val bitMatrix =
        multiFormatWriter.encode(getChannelUrl(false, shouldAdd).toString(), BarcodeFormat.QR_CODE, 960, 960)
    val barcodeEncoder = BarcodeEncoder()
    barcodeEncoder.createBitmap(bitMatrix)
} catch (ex: Throwable) {
    Logger.e { "URL was too complex to render as barcode" }
    null
}

