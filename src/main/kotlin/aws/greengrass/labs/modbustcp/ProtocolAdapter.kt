package aws.greengrass.labs.modbustcp

import kotlinx.coroutines.*
import software.amazon.awssdk.aws.greengrass.GreengrassCoreIPCClient
import kotlin.coroutines.CoroutineContext

class ProtocolAdapter(private val parentCoroutineContext: CoroutineContext) : CoroutineScope {
    private val job = Job()
    override val coroutineContext get() = parentCoroutineContext + job

    suspend fun <T> execute(block: suspend () -> T?): T? {
        return connectEventStreamIPC().use { connection ->
            val ipcClient = GreengrassCoreIPCClient(connection)

            val configManager = ConfigurationManager(ipcClient, coroutineContext)
            val endpointManager = EndpointManager(coroutineContext)
            val deviceManager = DeviceManager(ipcClient, coroutineContext)

            configManager.connect()
            try {
                val config = configManager.configuration ?: return@use null

                for (endpointConfig in config.endpoints) {
                    val endpoint = endpointManager.addEndpoint(endpointConfig)
                    for (deviceConfig in endpointConfig.devices) {
                        deviceManager.addDevice(deviceConfig, endpoint)
                    }
                }

                endpointManager.connect()
                try {
                    deviceManager.connect()
                    try {
                        println("Connected: $config")
                        return block()
                    } finally {
                        withContext(NonCancellable) {
                            deviceManager.disconnect()
                        }
                    }
                } finally {
                    withContext(NonCancellable) {
                        endpointManager.disconnect()
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    configManager.disconnect()
                }
            }
        }
    }
}
