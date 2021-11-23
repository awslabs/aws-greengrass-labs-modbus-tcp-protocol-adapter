// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import software.amazon.awssdk.aws.greengrass.GreengrassCoreIPC
import kotlin.coroutines.CoroutineContext

class DeviceManager(private val ipcClient: GreengrassCoreIPC, private val parentCoroutineContext: CoroutineContext) : CoroutineScope {
    private val devices = mutableListOf<ModbusDevice>()

    private val job = Job()
    override val coroutineContext get() = parentCoroutineContext + job

    fun addDevice(configuration: ModbusConfiguration.Device, endpoint: ModbusEndpoint): ModbusDevice {
        val device = ModbusDevice(configuration, endpoint, ipcClient, coroutineContext)
        devices.add(device)
        return device
    }

    suspend fun connect() {
        withContext(coroutineContext) {
            devices.forEach { it.connect() }
        }
    }

    suspend fun disconnect() {
        job.cancel()
        devices.forEach { it.disconnect() }
    }
}
