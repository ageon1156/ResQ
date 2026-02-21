
package org.meshtastic.feature.settings.radio.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meshtastic.core.strings.getString
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.cancel
import org.meshtastic.core.strings.save
import org.meshtastic.proto.ClientOnlyProtos.DeviceProfile
import org.meshtastic.proto.deviceProfile
import org.meshtastic.proto.position

@RunWith(AndroidJUnit4::class)
class EditDeviceProfileDialogTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun getString(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val title = "Export configuration"
    private val deviceProfile = deviceProfile {
        longName = "Long name"
        shortName = "Short name"
        channelUrl = "https://meshtastic.org/e/#CgMSAQESBggBQANIAQ"
        fixedPosition = position {
            latitudeI = 327766650
            longitudeI = -967969890
            altitude = 138
        }
    }

    private fun testEditDeviceProfileDialog(onDismiss: () -> Unit = {}, onConfirm: (DeviceProfile) -> Unit = {}) =
        composeTestRule.setContent {
            EditDeviceProfileDialog(
                title = title,
                deviceProfile = deviceProfile,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }

    @Test
    fun testEditDeviceProfileDialog_showsDialogTitle() {
        composeTestRule.apply {
            testEditDeviceProfileDialog()

            onNodeWithText(title).assertIsDisplayed()
        }
    }

    @Test
    fun testEditDeviceProfileDialog_showsCancelAndSaveButtons() {
        composeTestRule.apply {
            testEditDeviceProfileDialog()

            onNodeWithText(getString(Res.string.cancel)).assertIsDisplayed()
            onNodeWithText(getString(Res.string.save)).assertIsDisplayed()
        }
    }

    @Test
    fun testEditDeviceProfileDialog_clickCancelButton() {
        var onDismissClicked = false
        composeTestRule.apply {
            testEditDeviceProfileDialog(onDismiss = { onDismissClicked = true })

            onNodeWithText(getString(Res.string.cancel)).performClick()
        }

        Assert.assertTrue(onDismissClicked)
    }

    @Test
    fun testEditDeviceProfileDialog_addChannels() {
        var actualDeviceProfile: DeviceProfile? = null
        composeTestRule.apply {
            testEditDeviceProfileDialog(onConfirm = { actualDeviceProfile = it })

            onNodeWithText(getString(Res.string.save)).performClick()
        }

        Assert.assertEquals(deviceProfile, actualDeviceProfile)
    }
}

