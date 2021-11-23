// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

import kotlinx.coroutines.future.await
import software.amazon.awssdk.crt.io.ClientBootstrap
import software.amazon.awssdk.crt.io.EventLoopGroup
import software.amazon.awssdk.crt.io.SocketOptions
import software.amazon.awssdk.eventstreamrpc.EventStreamRPCConnection
import software.amazon.awssdk.eventstreamrpc.EventStreamRPCConnection.LifecycleHandler
import software.amazon.awssdk.eventstreamrpc.EventStreamRPCConnectionConfig
import software.amazon.awssdk.eventstreamrpc.GreengrassConnectMessageSupplier

// Port number is not used in domain sockets.
// It is ignored but the field needs to be set when creating socket connection
private const val DEFAULT_PORT = 8033

private val DEFAULT_SOCKET_OPTIONS = SocketOptions().apply {
    connectTimeoutMs = 3000
    domain = SocketOptions.SocketDomain.LOCAL
    type = SocketOptions.SocketType.STREAM
}

private val DEFAULT_LIFECYCLE_HANDLER = object : LifecycleHandler {
    //only called on successful connection.
    // That is full on Connect -> ConnectAck(ConnectionAccepted=true)
    override fun onConnect() {
    }

    override fun onDisconnect(errorCode: Int) {
    }

    //This on error is for any errors that is connection level, including problems during connect()
    override fun onError(t: Throwable): Boolean {
        return true //hints at handler to disconnect due to this error
    }
}

// removed dependency on kernel, as it is only being used to pull ipcServerSocketPath
suspend fun connectEventStreamIPC(port: Int = DEFAULT_PORT, socketOptions: SocketOptions = DEFAULT_SOCKET_OPTIONS, lifecycleHandler: LifecycleHandler = DEFAULT_LIFECYCLE_HANDLER): EventStreamRPCConnection {
    val ipcServerSocketPath = System.getenv("AWS_GG_NUCLEUS_DOMAIN_SOCKET_FILEPATH_FOR_COMPONENT")
    val authToken = System.getenv("SVCUID")

    EventLoopGroup(1).use { elGroup ->
        ClientBootstrap(elGroup, null).use { clientBootstrap ->
            val connectMessageAmender = GreengrassConnectMessageSupplier.connectMessageSupplier(authToken)
            val config = EventStreamRPCConnectionConfig(clientBootstrap, elGroup, socketOptions, null, ipcServerSocketPath, port, connectMessageAmender)
            val connection = EventStreamRPCConnection(config)

            //this is a bit cumbersome but does not prevent a convenience wrapper from exposing a sync
            //connect() or a connect() that returns a CompletableFuture that errors
            //this could be wrapped by utility methods to provide a more
            connection.connect(lifecycleHandler).await()
            return connection
        }
    }
}
