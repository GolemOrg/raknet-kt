package raknet.packet.protocol

import io.netty.buffer.ByteBuf
import raknet.Magic
import raknet.Magic.readMagic
import raknet.packet.DataPacket
import raknet.packet.PacketType

class UnconnectedPingPacket(
    var time: Long,
    var magic: Magic,
    var clientGuid: Long
): DataPacket(PacketType.UNCONNECTED_PING.id()) {

    override fun decode(buffer: ByteBuf) {

    }

    companion object {
        fun from(buffer: ByteBuf): UnconnectedPingPacket {
            return UnconnectedPingPacket(
                buffer.readLong(),
                buffer.readMagic(),
                buffer.readLong()
            )
        }
    }

    override fun toString(): String {
        return "UnconnectedPingPacket(time=$time, clientGuid=$clientGuid)"
    }
}