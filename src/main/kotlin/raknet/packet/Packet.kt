package raknet.packet

import io.netty.buffer.ByteBuf

/**
 * This is the base layer for any RakNet Packet.
 * This can be one of three things in RakNet:
 * 1. An ACK
 * 2. A NAK
 * 3. A datagram
 *
 *
 * We use this interface as a way to have a uniform way to encode & decode any data
 */
interface Packet {

    fun encode(): ByteBuf

    fun decode(buffer: ByteBuf)

}