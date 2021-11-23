// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

const val MODBUS_FUNCTION_DISCRIMINATOR = "function"

const val MODBUS_EXCEPTION_CODE = "ExceptionCode"
const val MODBUS_TIMEOUT = "Timeout"
const val MODBUS_BAD_REQUEST = "BadRequest"

const val MODBUS_FUNCTION_READ_COILS = "ReadCoils"
const val MODBUS_FUNCTION_READ_DISCRETE_INPUTS = "ReadDiscreteInputs"
const val MODBUS_FUNCTION_READ_HOLDING_REGISTERS = "ReadHoldingRegisters"
const val MODBUS_FUNCTION_READ_INPUT_REGISTERS = "ReadInputRegisters"
const val MODBUS_FUNCTION_WRITE_SINGLE_COIL = "WriteSingleCoil"
const val MODBUS_FUNCTION_WRITE_SINGLE_REGISTER = "WriteSingleRegister"
const val MODBUS_FUNCTION_WRITE_MULTIPLE_COILS = "WriteMultipleCoils"
const val MODBUS_FUNCTION_WRITE_MULTIPLE_REGISTERS = "WriteMultipleRegisters"
const val MODBUS_FUNCTION_MASK_WRITE_REGISTER = "MaskWriteRegister"
const val MODBUS_FUNCTION_READ_WRITE_MULTIPLE_REGISTERS = "ReadWriteMultipleRegisters"

@Serializable
@JsonClassDiscriminator(MODBUS_FUNCTION_DISCRIMINATOR)
sealed class ModbusRequest {
    abstract val id: String?
    abstract val unitId: Int?

    @Serializable
    @SerialName(MODBUS_FUNCTION_READ_COILS)
    data class ReadCoils(override val id: String? = null, override val unitId: Int? = null, val address: Int, val quantity: Int) : ModbusRequest()

    @Serializable
    @SerialName(MODBUS_FUNCTION_READ_DISCRETE_INPUTS)
    data class ReadDiscreteInputs(override val id: String? = null, override val unitId: Int? = null, val address: Int, val quantity: Int) : ModbusRequest()

    @Serializable
    @SerialName(MODBUS_FUNCTION_READ_HOLDING_REGISTERS)
    data class ReadHoldingRegisters(override val id: String? = null, override val unitId: Int? = null, val address: Int, val quantity: Int) : ModbusRequest()

    @Serializable
    @SerialName(MODBUS_FUNCTION_READ_INPUT_REGISTERS)
    data class ReadInputRegisters(override val id: String? = null, override val unitId: Int? = null, val address: Int, val quantity: Int) : ModbusRequest()

    @Serializable
    @SerialName(MODBUS_FUNCTION_WRITE_SINGLE_COIL)
    data class WriteSingleCoil(override val id: String? = null, override val unitId: Int? = null, val address: Int, val value: Boolean) : ModbusRequest()

    @Serializable
    @SerialName(MODBUS_FUNCTION_WRITE_SINGLE_REGISTER)
    data class WriteSingleRegister(override val id: String? = null, override val unitId: Int? = null, val address: Int, val value: UShort) : ModbusRequest()

    @Serializable
    @SerialName(MODBUS_FUNCTION_WRITE_MULTIPLE_COILS)
    data class WriteMultipleCoils(override val id: String? = null, override val unitId: Int? = null, val address: Int, val bits: List<Boolean>) : ModbusRequest()

    @Serializable
    @SerialName(MODBUS_FUNCTION_WRITE_MULTIPLE_REGISTERS)
    data class WriteMultipleRegisters(override val id: String? = null, override val unitId: Int? = null, val address: Int, val bytes: List<Byte>) : ModbusRequest()

    @Serializable
    @SerialName(MODBUS_FUNCTION_MASK_WRITE_REGISTER)
    data class MaskWriteRegister(override val id: String? = null, override val unitId: Int? = null, val address: Int, val andMask: Int, val orMask: Int) : ModbusRequest()

    @Serializable
    @SerialName(MODBUS_FUNCTION_READ_WRITE_MULTIPLE_REGISTERS)
    data class ReadWriteMultipleRegisters(override val id: String? = null, override val unitId: Int? = null, val readAddress: Int, val readQuantity: Int, val writeAddress: Int, val bytes: List<Byte>) : ModbusRequest()
}

@Serializable
sealed class ModbusResponse {
    abstract val id: String?

    @Serializable
    @SerialName(MODBUS_EXCEPTION_CODE)
    data class ExceptionCode(override val id: String? = null, val code: ModbusExceptionCode): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_TIMEOUT)
    data class Timeout(override val id: String? = null): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_BAD_REQUEST)
    data class BadRequest(override val id: String? = null): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_FUNCTION_READ_COILS)
    data class ReadCoils(override val id: String? = null, val bits: List<Boolean>): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_FUNCTION_READ_DISCRETE_INPUTS)
    data class ReadDiscreteInputs(override val id: String? = null, val bits: List<Boolean>): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_FUNCTION_READ_HOLDING_REGISTERS)
    data class ReadHoldingRegisters(override val id: String? = null, val bytes: List<Byte>): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_FUNCTION_READ_INPUT_REGISTERS)
    data class ReadInputRegisters(override val id: String? = null, val bytes: List<Byte>): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_FUNCTION_WRITE_SINGLE_COIL)
    data class WriteSingleCoil(override val id: String? = null, val address: Int, val value: Boolean): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_FUNCTION_WRITE_SINGLE_REGISTER)
    data class WriteSingleRegister(override val id: String? = null, val address: Int, val value: UShort): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_FUNCTION_WRITE_MULTIPLE_COILS)
    data class WriteMultipleCoils(override val id: String? = null, val address: Int, val quantity: Int): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_FUNCTION_WRITE_MULTIPLE_REGISTERS)
    data class WriteMultipleRegisters(override val id: String? = null, val address: Int, val quantity: Int): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_FUNCTION_MASK_WRITE_REGISTER)
    data class MaskWriteRegister(override val id: String? = null, val address: Int, val andMask: Int, val orMask: Int): ModbusResponse()

    @Serializable
    @SerialName(MODBUS_FUNCTION_READ_WRITE_MULTIPLE_REGISTERS)
    data class ReadWriteMultipleRegisters(override val id: String? = null, val bytes: List<Byte>): ModbusResponse()
}

suspend fun ModbusEndpoint.request(request: ModbusRequest, unitId: Int): ModbusResponse {
    try {
        return when(request) {
            is ModbusRequest.ReadCoils -> {
                val coils = readCoils(request.unitId ?: unitId, request.address, request.quantity)
                ModbusResponse.ReadCoils(request.id, coils)
            }
            is ModbusRequest.ReadDiscreteInputs -> {
                val discreteInputs = readDiscreteInputs(request.unitId ?: unitId, request.address, request.quantity)
                ModbusResponse.ReadDiscreteInputs(request.id, discreteInputs)
            }
            is ModbusRequest.ReadHoldingRegisters -> {
                val holdingRegisters = readHoldingRegisters(request.unitId ?: unitId, request.address, request.quantity)
                ModbusResponse.ReadHoldingRegisters(request.id, holdingRegisters)
            }
            is ModbusRequest.ReadInputRegisters -> {
                val inputRegisters = readInputRegisters(request.unitId ?: unitId, request.address, request.quantity)
                ModbusResponse.ReadInputRegisters(request.id, inputRegisters)
            }
            is ModbusRequest.WriteSingleCoil -> {
                val (address, value) = writeSingleCoil(request.unitId ?: unitId, request.address, request.value)
                ModbusResponse.WriteSingleCoil(request.id, address, value)
            }
            is ModbusRequest.WriteSingleRegister -> {
                val (address, value) = writeSingleRegister(request.unitId ?: unitId, request.address, request.value)
                ModbusResponse.WriteSingleRegister(request.id, address, value)
            }
            is ModbusRequest.WriteMultipleCoils -> {
                val (address, quantity) = writeMultipleCoils(request.unitId ?: unitId, request.address, request.bits)
                ModbusResponse.WriteMultipleCoils(request.id, address, quantity)
            }
            is ModbusRequest.WriteMultipleRegisters -> {
                val (address, quantity) = writeMultipleRegisters(request.unitId ?: unitId, request.address, request.bytes)
                ModbusResponse.WriteMultipleRegisters(request.id, address, quantity)
            }
            is ModbusRequest.MaskWriteRegister -> {
                val (address, andMask, orMask) = maskWriteRegister(request.unitId ?: unitId, request.address, request.andMask, request.orMask)
                ModbusResponse.MaskWriteRegister(request.id, address, andMask, orMask)
            }
            is ModbusRequest.ReadWriteMultipleRegisters -> {
                val registers = readWriteMultipleRegisters(request.unitId ?: unitId, request.readAddress, request.readQuantity, request.writeAddress, request.bytes)
                ModbusResponse.ReadWriteMultipleRegisters(request.id, registers)
            }
        }
    } catch (ex: ModbusException) {
        return ModbusResponse.ExceptionCode(request.id, ex.code)
    } catch (ex: EndpointTimeoutException) {
        return ModbusResponse.Timeout(request.id)
    }
}
