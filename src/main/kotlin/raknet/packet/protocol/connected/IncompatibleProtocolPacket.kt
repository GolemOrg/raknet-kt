package raknet.packet.protocol.connected

import io.netty.buffer.ByteBuf
import raknet.packet.DataPacket
import raknet.packet.PacketType

class IncompatibleProtocolPacket(
    var protocol: Int,
    var serverGuid: Long,
): DataPacket(PacketType.INCOMPATIBLE_PROTOCOL_VERSION.id()) {

    override fun encodeOrder(): Array<Any> {
        return arrayOf(protocol, serverGuid)
    }

    companion object {
        fun from(buffer: ByteBuf): IncompatibleProtocolPacket {
            return IncompatibleProtocolPacket(
                buffer.readInt(),
                buffer.readLong()
            )
        }
    }

    override fun toString(): String {
        return "IncompatibleProtocolPacket(protocol=$protocol, serverGuid=$serverGuid)"
    }
}