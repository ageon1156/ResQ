
package com.geeksville.mesh.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.core.model.DataPacket
import org.meshtastic.proto.user

class MeshCommandSenderTest {

    private lateinit var commandSender: MeshCommandSender
    private lateinit var nodeManager: MeshNodeManager

    @Before
    fun setUp() {
        nodeManager = MeshNodeManager()
        commandSender = MeshCommandSender(null, nodeManager, null, null)
    }

    @Test
    fun `generatePacketId produces unique non-zero IDs`() {
        val ids = mutableSetOf<Int>()
        repeat(1000) {
            val id = commandSender.generatePacketId()
            assertNotEquals(0, id)
            ids.add(id)
        }
        assertEquals(1000, ids.size)
    }

    @Test
    fun `resolveNodeNum handles broadcast ID`() {
        assertEquals(DataPacket.NODENUM_BROADCAST, commandSender.resolveNodeNum(DataPacket.ID_BROADCAST))
    }

    @Test
    fun `resolveNodeNum handles hex ID with exclamation mark`() {
        assertEquals(123, commandSender.resolveNodeNum("!0000007b"))
    }

    @Test
    fun `resolveNodeNum handles custom node ID from database`() {
        val nodeNum = 456
        val userId = "custom_id"
        val entity = NodeEntity(num = nodeNum, user = user { id = userId })
        nodeManager.nodeDBbyNodeNum[nodeNum] = entity
        nodeManager.nodeDBbyID[userId] = entity

        assertEquals(nodeNum, commandSender.resolveNodeNum(userId))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `resolveNodeNum throws for unknown ID`() {
        commandSender.resolveNodeNum("unknown")
    }
}

