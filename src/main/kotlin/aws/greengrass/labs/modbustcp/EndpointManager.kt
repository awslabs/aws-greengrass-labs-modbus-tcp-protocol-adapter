// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class EndpointManager(private val parentCoroutineContext: CoroutineContext) : CoroutineScope {
    private val endpoints = mutableListOf<ModbusEndpoint>()

    private val job = Job()
    override val coroutineContext get() = parentCoroutineContext + job

    fun addEndpoint(configuration: ModbusConfiguration.Endpoint): ModbusEndpoint {
        val endpoint = ModbusEndpointDigitalpetri(configuration, coroutineContext)
        endpoints.add(endpoint)
        return endpoint
    }

    suspend fun connect() {
        withContext(coroutineContext) {
            endpoints.forEach { it.connect() }
        }
    }

    suspend fun disconnect() {
        job.cancel()
        endpoints.forEach { it.disconnect() }
    }
}
