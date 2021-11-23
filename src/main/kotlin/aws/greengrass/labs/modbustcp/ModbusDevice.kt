// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.aws.greengrass.GreengrassCoreIPC
import software.amazon.awssdk.aws.greengrass.SubscribeToTopicResponseHandler
import software.amazon.awssdk.aws.greengrass.model.*
import software.amazon.awssdk.eventstreamrpc.StreamResponseHandler
import java.util.*
import kotlin.coroutines.CoroutineContext

private fun topicForDeviceRequest(name: String) = "modbus/request/$name"
private fun topicForDeviceResponse(name: String) = "modbus/response/$name"

class ModbusDevice(configuration: ModbusConfiguration.Device, private val endpoint: ModbusEndpoint, private val ipcClient: GreengrassCoreIPC, private val parentCoroutineContext: CoroutineContext) : CoroutineScope {
    val name = configuration.name
    val unitId = configuration.unitId

    private val job = Job()
    override val coroutineContext get() = parentCoroutineContext + job

    private var commandRequestHandler: SubscribeToTopicResponseHandler? = null

    private val commandHandler = object : StreamResponseHandler<SubscriptionResponseMessage> {
        override fun onStreamEvent(responseMessage: SubscriptionResponseMessage) {
            launch {
                val response: ModbusResponse = try {
                    val request = ModbusRequest.fromJson(String(responseMessage.binaryMessage.message))
                    try {
                        endpoint.request(request, unitId)
                    } catch (ex: Exception) {
                        ModbusResponse.BadRequest(request.id)
                    }
                } catch (ex: Exception) {
                    ModbusResponse.BadRequest()
                }
                val publishRequest = PublishToTopicRequest().apply {
                    topic = topicForDeviceResponse(name)
                    publishMessage = PublishMessage().apply {
                        binaryMessage = BinaryMessage().apply {
                            message = response.toJson().toByteArray()
                        }
                    }
                }
                ipcClient.publishToTopic(publishRequest, Optional.empty()).response.await()
            }
        }

        override fun onStreamError(throwable: Throwable): Boolean {
            return true
        }

        override fun onStreamClosed() {
        }
    }

    suspend fun connect() {
        withContext(coroutineContext) {
            val request = SubscribeToTopicRequest().apply {
                topic = topicForDeviceRequest(name)
            }
            val handler = ipcClient.subscribeToTopic(request, Optional.of(commandHandler))
            handler.response.await()

            commandRequestHandler = handler
        }
    }

    suspend fun disconnect() {
        job.cancel()
        commandRequestHandler?.closeStream()?.await()
    }
}
