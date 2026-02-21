
package org.meshtastic.feature.messaging.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.meshtastic.core.database.model.Message
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.ui.component.preview.NodePreviewParameterProvider

@RunWith(AndroidJUnit4::class)
class MessageItemTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun mqttIconIsDisplayedWhenViaMqttIsTrue() {
        val testNode = NodePreviewParameterProvider().minnieMouse
        val messageWithMqtt =
            Message(
                text = "Test message via MQTT",
                time = "10:00",
                fromLocal = false,
                status = MessageStatus.RECEIVED,
                snr = 2.5f,
                rssi = 90,
                hopsAway = 0,
                uuid = 1L,
                receivedTime = System.currentTimeMillis(),
                node = testNode,
                read = false,
                routingError = 0,
                packetId = 1234,
                emojis = listOf(),
                replyId = null,
                viaMqtt = true,
            )

        composeTestRule.setContent {
            MessageItem(
                message = messageWithMqtt,
                node = testNode,
                selected = false,
                onClick = {},
                onLongClick = {},
                onStatusClick = {},
                ourNode = testNode,
            )
        }

        composeTestRule.onNodeWithContentDescription("via MQTT").assertIsDisplayed()
    }

    @Test
    fun mqttIconIsNotDisplayedWhenViaMqttIsFalse() {
        val testNode = NodePreviewParameterProvider().minnieMouse
        val messageWithoutMqtt =
            Message(
                text = "Test message not via MQTT",
                time = "10:00",
                fromLocal = false,
                status = MessageStatus.RECEIVED,
                snr = 2.5f,
                rssi = 90,
                hopsAway = 0,
                uuid = 1L,
                receivedTime = System.currentTimeMillis(),
                node = testNode,
                read = false,
                routingError = 0,
                packetId = 1234,
                emojis = listOf(),
                replyId = null,
                viaMqtt = false,
            )

        composeTestRule.setContent {
            MessageItem(
                message = messageWithoutMqtt,
                node = testNode,
                selected = false,
                onClick = {},
                onLongClick = {},
                onStatusClick = {},
                ourNode = testNode,
            )
        }

        composeTestRule.onNodeWithContentDescription("via MQTT").assertDoesNotExist()
    }

    @Test
    fun messageItem_hasCorrectSemanticContentDescription() {
        val testNode = NodePreviewParameterProvider().minnieMouse
        val message =
            Message(
                text = "Hello World",
                time = "10:00",
                fromLocal = false,
                status = MessageStatus.RECEIVED,
                snr = 2.5f,
                rssi = 90,
                hopsAway = 0,
                uuid = 1L,
                receivedTime = System.currentTimeMillis(),
                node = testNode,
                read = false,
                routingError = 0,
                packetId = 1234,
                emojis = listOf(),
                replyId = null,
                viaMqtt = false,
            )

        composeTestRule.setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onClick = {},
                onLongClick = {},
                onStatusClick = {},
                ourNode = testNode,
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Message from ${testNode.user?.longName}: Hello World")
            .assertIsDisplayed()
    }
}

