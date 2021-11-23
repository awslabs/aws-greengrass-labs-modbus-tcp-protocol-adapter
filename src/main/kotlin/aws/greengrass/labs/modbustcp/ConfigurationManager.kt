// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import software.amazon.awssdk.aws.greengrass.GreengrassCoreIPC
import software.amazon.awssdk.aws.greengrass.model.GetConfigurationRequest
import java.util.*
import kotlin.coroutines.CoroutineContext

class ConfigurationManager(private val ipcClient: GreengrassCoreIPC, private val parentCoroutineContext: CoroutineContext) : CoroutineScope {
    var configuration: ModbusConfiguration? = null

    private val job = Job()
    override val coroutineContext get() = parentCoroutineContext + job

    private suspend fun loadModbusConfiguration(): ModbusConfiguration? {
        val getConfigurationRequest = GetConfigurationRequest().apply {
            keyPath = listOf("Modbus")
        }
        val getConfigurationResponse = ipcClient.getConfiguration(getConfigurationRequest, Optional.empty()).response.await()
        return loadConfiguration(getConfigurationResponse.value)
    }

    suspend fun connect() {
        withContext(coroutineContext) {
            configuration = loadModbusConfiguration()
        }
    }

    suspend fun disconnect() {
        job.cancel()
    }
}

private fun loadConfiguration(config: Map<String, Any>): ModbusConfiguration? {
    val endpoints = config["Endpoints"] as? List<*> ?: return null
    return ModbusConfiguration(loadEndpoints(endpoints))
}

private fun loadEndpoints(endpoints: List<*>) = endpoints.mapNotNull {
    (it as? Map<*, *>)?.let { endpoint -> loadEndpoint(endpoint) }
}

private fun loadEndpoint(endpoint: Map<*, *>): ModbusConfiguration.Endpoint? {
    val host = endpoint["Host"] as? String ?: return null
    val port = (endpoint["Port"] as? Number)?.toInt()
    val timeout = (endpoint["Timeout"] as? Number)?.toDouble()

    val devices = endpoint["Devices"] as? List<*> ?: return null
    return ModbusConfiguration.Endpoint(host, port, timeout, loadDevices(devices))
}

private fun loadDevices(devices: List<*>) = devices.mapNotNull {
    (it as? Map<*, *>)?.let { device -> loadDevice(device) }
}

private fun loadDevice(device: Map<*, *>): ModbusConfiguration.Device? {
    val name = device["Name"] as? String ?: return null
    val unitId = (device["UnitId"] as? Number)?.toInt() ?: 0
    return ModbusConfiguration.Device(name, unitId)
}
