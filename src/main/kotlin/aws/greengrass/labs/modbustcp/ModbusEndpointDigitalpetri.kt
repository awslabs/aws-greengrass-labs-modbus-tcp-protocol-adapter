// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

import com.digitalpetri.modbus.ExceptionCode
import com.digitalpetri.modbus.ModbusResponseException
import com.digitalpetri.modbus.ModbusTimeoutException
import com.digitalpetri.modbus.master.ModbusTcpMaster
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig
import com.digitalpetri.modbus.requests.*
import com.digitalpetri.modbus.responses.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class ModbusEndpointDigitalpetri(configuration: ModbusConfiguration.Endpoint, private val parentCoroutineContext: CoroutineContext) : ModbusEndpoint, CoroutineScope {
    private val client = ModbusTcpMaster(configuration.toModbusTcpMasterConfig())

    private val job = Job()
    override val coroutineContext get() = parentCoroutineContext + job

    override suspend fun connect() {
        withContext(coroutineContext) {
            client.connect().await()
        }
    }

    override suspend fun disconnect() {
        job.cancel()
        client.disconnect().await()
    }

    override suspend fun readCoils(unitId: Int, address: Int, quantity: Int) = executeOrThrow {
        val request = ReadCoilsRequest(address, quantity)
        val response = client.sendRequest<ReadCoilsResponse>(request, unitId).await()
        response.use {
            response.coilStatus.readBitList(quantity)
        }
    }

    override suspend fun readDiscreteInputs(unitId: Int, address: Int, quantity: Int) = executeOrThrow {
        val request = ReadDiscreteInputsRequest(address, quantity)
        val response = client.sendRequest<ReadDiscreteInputsResponse>(request, unitId).await()
        response.use {
            response.inputStatus.readBitList(quantity)
        }
    }

    override suspend fun readHoldingRegisters(unitId: Int, address: Int, quantity: Int) = executeOrThrow {
        val request = ReadHoldingRegistersRequest(address, quantity)
        val response = client.sendRequest<ReadHoldingRegistersResponse>(request, unitId).await()
        response.use {
            response.registers.readByteArray(quantity * 2).toList()
        }
    }

    override suspend fun readInputRegisters(unitId: Int, address: Int, quantity: Int) = executeOrThrow {
        val request = ReadInputRegistersRequest(address, quantity)
        val response = client.sendRequest<ReadInputRegistersResponse>(request, unitId).await()
        response.use {
            response.registers.readByteArray(quantity * 2).toList()
        }
    }

    override suspend fun writeSingleCoil(unitId: Int, address: Int, value: Boolean) = executeOrThrow {
        val request = WriteSingleCoilRequest(address, value)
        val response = client.sendRequest<WriteSingleCoilResponse>(request, unitId).await()
        response.address to (response.value != 0x0000)
    }

    override suspend fun writeSingleRegister(unitId: Int, address: Int, value: UShort) = executeOrThrow {
        val request = WriteSingleRegisterRequest(address, value.toInt())
        val response = client.sendRequest<WriteSingleRegisterResponse>(request, unitId).await()
        response.address to response.value.toUShort()
    }

    override suspend fun writeMultipleCoils(unitId: Int, address: Int, values: List<Boolean>) = executeOrThrow {
        val request = WriteMultipleCoilsRequest(address, values.size, values.toBytes().toByteArray())
        val response = client.sendRequest<WriteMultipleCoilsResponse>(request, unitId).await()
        response.address to response.quantity
    }

    override suspend fun writeMultipleRegisters(unitId: Int, address: Int, values: List<Byte>) = executeOrThrow {
        val request = WriteMultipleRegistersRequest(address, values.size / 2, values.toByteArray())
        val response = client.sendRequest<WriteMultipleRegistersResponse>(request, unitId).await()
        response.address to response.quantity
    }

    override suspend fun maskWriteRegister(unitId: Int, address: Int, andMask: Int, orMask: Int) = executeOrThrow {
        val request = MaskWriteRegisterRequest(address, andMask, orMask)
        val response = client.sendRequest<MaskWriteRegisterResponse>(request, unitId).await()
        Triple(response.address, response.andMask, response.orMask)
    }

    override suspend fun readWriteMultipleRegisters(unitId: Int, readAddress: Int, readQuantity: Int, writeAddress: Int, values: List<Byte>) = executeOrThrow {
        val request = ReadWriteMultipleRegistersRequest(readAddress, readQuantity, writeAddress, values.size / 2, values.toByteArray())
        val response = client.sendRequest<ReadWriteMultipleRegistersResponse>(request, unitId).await()
        response.use {
            response.registers.readByteArray(readQuantity * 2).toList()
        }
    }
}

private fun ModbusConfiguration.Endpoint.toModbusTcpMasterConfig() = ModbusTcpMasterConfig.Builder(host).apply {
    port?.let { setPort(it) }
    timeout?.let { setTimeout(Duration.ofMillis((it * 1000).toLong())) }
}.build()

private suspend fun <R> ModbusEndpointDigitalpetri.executeOrThrow(exec: suspend ModbusEndpointDigitalpetri.() -> R): R = withContext(coroutineContext) {
    try {
        exec()
    } catch (ex: ModbusResponseException) {
        throw ModbusException(ex.response.exceptionCode.toModbusExceptionCode(), ex)
    } catch (ex: ModbusTimeoutException) {
        throw EndpointTimeoutException(ex)
    }
}

private fun ExceptionCode.toModbusExceptionCode() = when(this) {
    ExceptionCode.IllegalFunction -> ModbusExceptionCode.IllegalFunction
    ExceptionCode.IllegalDataAddress -> ModbusExceptionCode.IllegalDataAddress
    ExceptionCode.IllegalDataValue -> ModbusExceptionCode.IllegalDataValue
    ExceptionCode.SlaveDeviceFailure -> ModbusExceptionCode.SlaveDeviceFailure
    ExceptionCode.Acknowledge -> ModbusExceptionCode.Acknowledge
    ExceptionCode.SlaveDeviceBusy -> ModbusExceptionCode.SlaveDeviceBusy
    ExceptionCode.MemoryParityError -> ModbusExceptionCode.MemoryParityError
    ExceptionCode.GatewayPathUnavailable -> ModbusExceptionCode.GatewayPathUnavailable
    ExceptionCode.GatewayTargetDeviceFailedToResponse -> ModbusExceptionCode.GatewayTargetDeviceFailedToResponse
}
