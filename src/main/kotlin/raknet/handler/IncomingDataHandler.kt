package raknet.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.DatagramPacket
import raknet.Magic
import raknet.Server
import raknet.connection.Connection
import raknet.enums.Flag
import raknet.packet.*
import raknet.packet.protocol.*
import java.net.InetSocketAddress
import kotlin.experimental.and

/**
 * TODO: It may be worth it to split the incoming data handler into UnconnectedHandshakeHandler & ConnectedHandler
 */
class IncomingDataHandler(private val server: Server): SimpleChannelInboundHandler<DatagramPacket>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: DatagramPacket) {
        val sender = msg.sender()
        val buffer = msg.content()
        // Capture buffer data and transform it into a packet if possible
        // We could use a packet factory here, but I'm not sure if that's the best avenue to take at the moment
        val id = buffer.readUnsignedByte()
        val type: PacketType? = PacketType.find(id)
        if(!server.hasConnection(sender)) {
            if(type == null) return
            val response = handleUnconnected(ctx, type, buffer, sender) ?: return
            ctx.sendPacket(sender, response)
        } else {
            val connection = server.getConnection(sender)!!
            when(type) {
                PacketType.ACK -> connection.handleAck(Acknowledge.from(buffer))
                PacketType.NACK -> connection.handleNAck(NAcknowledge.from(buffer))
                else -> {
                    if(!isDatagram(id)) {
                        connection.log("Received non-datagram packet of type $id from $sender")
                        return
                    }
                    val datagram = Datagram.from(id, buffer)
                    for (frame in datagram.frames) {
                        val body = frame.body
                        val frameType = body.readUnsignedByte()
                        val packet: DataPacket = when(PacketType.find(frameType)) {
                            PacketType.CONNECTION_REQUEST -> ConnectionRequest.from(body)
                            PacketType.NEW_INCOMING_CONNECTION -> NewIncomingConnection.from(body)
                            PacketType.DISCONNECTION_NOTIFICATION -> DisconnectionNotification()
                            PacketType.CONNECTED_PING -> ConnectedPing.from(body)
                            else -> {
                                connection.log("Received unknown packet of type $frameType from $sender")
                                continue
                            }
                        }
                        connection.handle(packet)
                    }
                }
            }
        }
    }

    private fun isDatagram(id: Short): Boolean = id and Flag.DATAGRAM.id() != 0.toShort()

    private fun handleUnconnected(ctx: ChannelHandlerContext, type: PacketType, buffer: ByteBuf, sender: InetSocketAddress): DataPacket? {
        return when(type) {
            PacketType.UNCONNECTED_PING -> {
                val ping = UnconnectedPing.from(buffer)
                UnconnectedPong(
                    pingId = ping.time,
                    magic = Magic,
                    guid = server.guid.mostSignificantBits,
                    serverName = server.identifier.toString(),
                )
            }
            PacketType.OPEN_CONNECTION_REQUEST_1 -> {
                val request = OpenConnectionRequest1.from(buffer)
                OpenConnectionReply1(
                    magic = Magic,
                    serverGuid = server.guid.mostSignificantBits,
                    useSecurity = false,
                    mtuSize = (request.mtuSize + 28).toShort(),
                )
            }
            PacketType.OPEN_CONNECTION_REQUEST_2 -> {
                val request = OpenConnectionRequest2.from(buffer)
                server.addConnection(Connection(
                    context = ctx,
                    server = server,
                    address = sender,
                    mtuSize = request.mtuSize,
                    guid = request.clientGuid
                ))
                OpenConnectionReply2(
                    magic = Magic,
                    serverGuid = server.guid.mostSignificantBits,
                    mtuSize = request.mtuSize,
                    clientAddress = request.serverAddress,
                    encryptionEnabled = false,
                )
            }
            else -> null
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        cause?.printStackTrace()
    }
}

fun ChannelHandlerContext.sendPacket(address: InetSocketAddress, packet: DataPacket): ChannelFuture = writeAndFlush(DatagramPacket(packet.prepare(), address))