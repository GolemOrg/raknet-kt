package raknet

import io.netty.util.ResourceLeakDetector
import raknet.connection.Connection
import raknet.handler.NetworkHandler
import java.net.InetSocketAddress
import java.util.*
import kotlin.collections.HashMap

class Server(
    private val port: Int = 19132,
    private val maxConnections: Int = 250,
    private val name: String = "RakNet Server",
    private var verbose: Boolean = false,
) {

    init {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID)
    }

    val guid: UUID = UUID.randomUUID()
    val identifier: Identifier = Identifier(arrayListOf(
        "Test Server",
        475,
        "1.18.0",
        0,
        100,
        guid.mostSignificantBits,
        "raknet-kt",
        "Creative"
    ))

    private val handler = NetworkHandler(this)
    private val connections: HashMap<InetSocketAddress, Connection> = HashMap()
    var running: Boolean = true

    fun start() {
        log("Starting server with name '$name'...")
        handler.start()
        log("Server successfully started")
        while(running) {

        }
        log("Shutting down server...")
        shutdown()
    }

    fun stop() {
        running = false
    }

    fun shutdown() {
        log("Shutting down server...")
        // Close all connections
        handler.shutdown()
        log("Server successfully shut down")
    }



    /**
     * Logs a message to the console if verbose is enabled
     */
    fun log(message: String): Unit = if (verbose) println(message) else Unit

    fun getPort(): Int = port

    fun getMaxConnections(): Int = maxConnections

    fun getName(): String = name

    fun addConnection(connection: Connection) {
        connections[connection.address] = connection
    }

    fun getConnection(address: InetSocketAddress): Connection? = connections[address]

    fun getConnections(): List<Connection> = connections.values.toList()

    fun closeConnection(address: InetSocketAddress) {
        connections.remove(address)
    }

}