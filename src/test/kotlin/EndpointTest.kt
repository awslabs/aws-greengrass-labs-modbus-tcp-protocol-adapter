// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import aws.greengrass.labs.modbustcp.*
import aws.greengrass.labs.modbustcp.ModbusRequest
import aws.greengrass.labs.modbustcp.ModbusResponse
import com.digitalpetri.modbus.ExceptionCode
import com.digitalpetri.modbus.requests.*
import com.digitalpetri.modbus.responses.*
import com.digitalpetri.modbus.slave.ModbusTcpSlave
import com.digitalpetri.modbus.slave.ModbusTcpSlaveConfig
import com.digitalpetri.modbus.slave.ServiceRequestHandler
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TEST_HOST = "localhost"
private const val TEST_PORT = 5020

private const val TEST_UNIT_ID = 0

@ExtendWith(MockKExtension::class)
class EndpointTest {
    private lateinit var server: ModbusTcpSlave

    @BeforeTest
    fun beforeTest() {
        runBlocking {
            server = ModbusTcpSlave(ModbusTcpSlaveConfig.Builder().build())
            server.bind(TEST_HOST, TEST_PORT).await()
        }
    }

    @AfterTest
    fun afterTest() {
        server.shutdown()
    }

    private fun test(request: ModbusRequest, expected: ModbusResponse, timeout: Double? = null) {
        runBlocking {
            val manager = EndpointManager(coroutineContext)
            val configuration = ModbusConfiguration.Endpoint(TEST_HOST, TEST_PORT, timeout, devices = listOf())
            val endpoint = manager.addEndpoint(configuration)
            manager.connect()
            try {
                val response = endpoint.request(request, TEST_UNIT_ID)

                assertEquals(expected, response)
            } finally {
                manager.disconnect()
            }
        }
    }

    @Test
    @DisplayName("ReadCoils: Valid request -> Success")
    fun testReadCoils_success_when_valid_request() {
        val expected = ModbusResponse.ReadCoils(bits = listOf(true, false, true, false, true, false, true, false, true, false, true, false))
        val readCoils = ModbusRequest.ReadCoils(address = 0, quantity = expected.bits.size)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadCoils(service: ServiceRequestHandler.ServiceRequest<ReadCoilsRequest, ReadCoilsResponse>) {
                val request = ModbusRequest.ReadCoils(address = service.request.address, quantity = service.request.quantity)
                assertEquals(readCoils, request)

                val response = ReadCoilsResponse(expected.bits.toBytes().toByteBuf())
                service.sendResponse(response)
            }
        })

        test(readCoils, expected)
    }

    @Test
    @DisplayName("ReadCoils: Unsupported -> IllegalFunction")
    fun testReadCoils_IllegalFunction_when_unsupported() {
        val readCoils = ModbusRequest.ReadCoils(address = 0, quantity = 0)
        val expected = ModbusResponse.ExceptionCode(code = ModbusExceptionCode.IllegalFunction)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadCoils(service: ServiceRequestHandler.ServiceRequest<ReadCoilsRequest, ReadCoilsResponse>) {
                val request = ModbusRequest.ReadCoils(address = service.request.address, quantity = service.request.quantity)
                assertEquals(readCoils, request)

                service.sendException(ExceptionCode.IllegalFunction)
            }
        })

        test(readCoils, expected)
    }

    @Test
    @DisplayName("ReadDiscreteInputs: Valid request -> Success")
    fun testReadDiscreteInputs_success_when_valid_request() {
        val expected = ModbusResponse.ReadDiscreteInputs(bits = listOf(false, true, false, true, false, true, false, true))
        val readDiscreteInputs = ModbusRequest.ReadDiscreteInputs(address = 0, quantity = expected.bits.size)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadDiscreteInputs(service: ServiceRequestHandler.ServiceRequest<ReadDiscreteInputsRequest, ReadDiscreteInputsResponse>) {
                val request = ModbusRequest.ReadDiscreteInputs(address = service.request.address, quantity = service.request.quantity)
                assertEquals(readDiscreteInputs, request)

                val response = ReadDiscreteInputsResponse(expected.bits.toBytes().toByteBuf())
                service.sendResponse(response)
            }
        })

        test(readDiscreteInputs, expected)
    }

    @Test
    @DisplayName("ReadDiscreteInputs: Invalid address -> IllegalDataAddress")
    fun testReadDiscreteInputs_IllegalDataAddress_when_invalid_address() {
        val readDiscreteInputs = ModbusRequest.ReadDiscreteInputs(address = 0, quantity = 1)
        val expected = ModbusResponse.ExceptionCode(code = ModbusExceptionCode.IllegalDataAddress)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadDiscreteInputs(service: ServiceRequestHandler.ServiceRequest<ReadDiscreteInputsRequest, ReadDiscreteInputsResponse>) {
                val request = ModbusRequest.ReadDiscreteInputs(address = service.request.address, quantity = service.request.quantity)
                assertEquals(readDiscreteInputs, request)

                service.sendException(ExceptionCode.IllegalDataAddress)
            }
        })

        test(readDiscreteInputs, expected)
    }

    @Test
    @DisplayName("ReadHoldingRegisters: Valid request -> Success")
    fun testReadHoldingRegisters_success_when_valid_request() {
        val expected = ModbusResponse.ReadHoldingRegisters(bytes = listOf(0, 1))
        val readHoldingRegisters = ModbusRequest.ReadHoldingRegisters(address = 0, quantity = expected.bytes.size / 2)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadHoldingRegisters(service: ServiceRequestHandler.ServiceRequest<ReadHoldingRegistersRequest, ReadHoldingRegistersResponse>) {
                val request = ModbusRequest.ReadHoldingRegisters(address = service.request.address, quantity = service.request.quantity)
                assertEquals(readHoldingRegisters, request)

                val response = ReadHoldingRegistersResponse(expected.bytes.toByteBuf())
                service.sendResponse(response)
            }
        })

        test(readHoldingRegisters, expected)
    }

    @Test
    @DisplayName("ReadHoldingRegisters: Failure -> SlaveDeviceFailure")
    fun testReadHoldingRegisters_SlaveDeviceFailure_when_failure() {
        val readHoldingRegisters = ModbusRequest.ReadHoldingRegisters(address = 0, quantity = 1)
        val expected = ModbusResponse.ExceptionCode(code = ModbusExceptionCode.SlaveDeviceFailure)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadHoldingRegisters(service: ServiceRequestHandler.ServiceRequest<ReadHoldingRegistersRequest, ReadHoldingRegistersResponse>) {
                val request = ModbusRequest.ReadHoldingRegisters(address = service.request.address, quantity = service.request.quantity)
                assertEquals(readHoldingRegisters, request)

                service.sendException(ExceptionCode.SlaveDeviceFailure)
            }
        })

        test(readHoldingRegisters, expected)
    }

    @Test
    @DisplayName("ReadInputRegisters: Valid request -> Success")
    fun testReadInputRegisters_success_when_valid_request() {
        val expected = ModbusResponse.ReadInputRegisters(bytes = listOf(0, 1))
        val readInputRegisters = ModbusRequest.ReadInputRegisters(address = 0, quantity = expected.bytes.size / 2)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadInputRegisters(service: ServiceRequestHandler.ServiceRequest<ReadInputRegistersRequest, ReadInputRegistersResponse>) {
                val request = ModbusRequest.ReadInputRegisters(address = service.request.address, quantity = service.request.quantity)
                assertEquals(readInputRegisters, request)

                val response = ReadInputRegistersResponse(expected.bytes.toByteBuf())
                service.sendResponse(response)
            }
        })

        test(readInputRegisters, expected)
    }

    @Test
    @DisplayName("ReadInputRegisters: Busy -> SlaveDeviceBusy")
    fun testReadInputRegisters_SlaveDeviceBusy_when_busy() {
        val readInputRegisters = ModbusRequest.ReadInputRegisters(address = 0, quantity = 1)
        val expected = ModbusResponse.ExceptionCode(code = ModbusExceptionCode.SlaveDeviceBusy)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadInputRegisters(service: ServiceRequestHandler.ServiceRequest<ReadInputRegistersRequest, ReadInputRegistersResponse>) {
                val request = ModbusRequest.ReadInputRegisters(address = service.request.address, quantity = service.request.quantity)
                assertEquals(readInputRegisters, request)

                service.sendException(ExceptionCode.SlaveDeviceBusy)
            }
        })

        test(readInputRegisters, expected)
    }

    @Test
    @DisplayName("WriteSingleCoil: Valid request -> Success")
    fun testWriteSingleCoil_success_when_valid_request() {
        val writeSingleCoil = ModbusRequest.WriteSingleCoil(address = 0, value = true)
        val expected = ModbusResponse.WriteSingleCoil(address = writeSingleCoil.address, value = writeSingleCoil.value)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onWriteSingleCoil(service: ServiceRequestHandler.ServiceRequest<WriteSingleCoilRequest, WriteSingleCoilResponse>) {
                val request = ModbusRequest.WriteSingleCoil(address = service.request.address, value = service.request.value != 0x0000)
                assertEquals(writeSingleCoil, request)

                val response = WriteSingleCoilResponse(expected.address, if (expected.value) 0xFF00 else 0x0000)
                service.sendResponse(response)
            }
        })

        test(writeSingleCoil, expected)
    }

    @Test
    @DisplayName("WriteSingleCoil: Invalid value -> IllegalDataValue")
    fun testWriteSingleCoil_IllegalDataValue_when_invalid_value() {
        val writeSingleCoil = ModbusRequest.WriteSingleCoil(address = 0, value = false)
        val expected = ModbusResponse.ExceptionCode(code = ModbusExceptionCode.IllegalDataValue)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onWriteSingleCoil(service: ServiceRequestHandler.ServiceRequest<WriteSingleCoilRequest, WriteSingleCoilResponse>) {
                val request = ModbusRequest.WriteSingleCoil(address = service.request.address, value = service.request.value != 0x0000)
                assertEquals(writeSingleCoil, request)

                service.sendException(ExceptionCode.IllegalDataValue)
            }
        })

        test(writeSingleCoil, expected)
    }

    @Test
    @DisplayName("WriteSingleRegister: Valid request -> Success")
    fun testWriteSingleRegister_success_when_valid_request() {
        val writeSingleRegister = ModbusRequest.WriteSingleRegister(address = 0, value = 1U)
        val expected = ModbusResponse.WriteSingleRegister(address = writeSingleRegister.address, value = writeSingleRegister.value)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onWriteSingleRegister(service: ServiceRequestHandler.ServiceRequest<WriteSingleRegisterRequest, WriteSingleRegisterResponse>) {
                val request = ModbusRequest.WriteSingleRegister(address = service.request.address, value = service.request.value.toUShort())
                assertEquals(writeSingleRegister, request)

                val response = WriteSingleRegisterResponse(expected.address, expected.value.toInt())
                service.sendResponse(response)
            }
        })

        test(writeSingleRegister, expected)
    }

    @Test
    @DisplayName("WriteSingleRegister: Unavailable -> GatewayPathUnavailable")
    fun testWriteSingleRegister_GatewayPathUnavailable_when_unavailable() {
        val writeSingleRegister = ModbusRequest.WriteSingleRegister(address = 0, value = 0U)
        val expected = ModbusResponse.ExceptionCode(code = ModbusExceptionCode.GatewayPathUnavailable)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onWriteSingleRegister(service: ServiceRequestHandler.ServiceRequest<WriteSingleRegisterRequest, WriteSingleRegisterResponse>) {
                val request = ModbusRequest.WriteSingleRegister(address = service.request.address, value = service.request.value.toUShort())
                assertEquals(writeSingleRegister, request)

                service.sendException(ExceptionCode.GatewayPathUnavailable)
            }
        })

        test(writeSingleRegister, expected)
    }

    @Test
    @DisplayName("WriteMultipleCoils: Valid request -> Success")
    fun testWriteMultipleCoils_success_when_valid_request() {
        val writeMultipleCoils = ModbusRequest.WriteMultipleCoils(address = 0, bits = listOf(true, false, true, false, true, false, true, false))
        val expected = ModbusResponse.WriteMultipleCoils(address = writeMultipleCoils.address, quantity = writeMultipleCoils.bits.size)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onWriteMultipleCoils(service: ServiceRequestHandler.ServiceRequest<WriteMultipleCoilsRequest, WriteMultipleCoilsResponse>) {
                service.request.use {
                    val request = ModbusRequest.WriteMultipleCoils(address = it.address, bits = it.values.readBitList(it.quantity))
                    assertEquals(writeMultipleCoils, request)

                    val response = WriteMultipleCoilsResponse(expected.address, expected.quantity)
                    service.sendResponse(response)
                }
            }
        })

        test(writeMultipleCoils, expected)
    }

    @Test
    @DisplayName("WriteMultipleCoils: Device unavailable -> GatewayTargetDeviceFailedToResponse")
    fun testWriteMultipleCoils_GatewayTargetDeviceFailedToResponse_when_device_unavailable() {
        val writeMultipleCoils = ModbusRequest.WriteMultipleCoils(address = 0, bits = listOf(false, true, false, true, false, true, false, true, false, true, false, true))
        val expected = ModbusResponse.ExceptionCode(code = ModbusExceptionCode.GatewayTargetDeviceFailedToResponse)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onWriteMultipleCoils(service: ServiceRequestHandler.ServiceRequest<WriteMultipleCoilsRequest, WriteMultipleCoilsResponse>) {
                service.request.use {
                    val request = ModbusRequest.WriteMultipleCoils(address = it.address, bits = it.values.readBitList(it.quantity))
                    assertEquals(writeMultipleCoils, request)

                    service.sendException(ExceptionCode.GatewayTargetDeviceFailedToResponse)
                }
            }
        })

        test(writeMultipleCoils, expected)
    }

    @Test
    @DisplayName("WriteMultipleRegisters: Valid request -> Success")
    fun testWriteMultipleRegisters_success_when_valid_request() {
        val writeMultipleRegisters = ModbusRequest.WriteMultipleRegisters(address = 0, bytes = listOf(0, 1))
        val expected = ModbusResponse.WriteMultipleRegisters(address = writeMultipleRegisters.address, quantity = writeMultipleRegisters.bytes.size / 2)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onWriteMultipleRegisters(service: ServiceRequestHandler.ServiceRequest<WriteMultipleRegistersRequest, WriteMultipleRegistersResponse>) {
                service.request.use {
                    val request = ModbusRequest.WriteMultipleRegisters(address = it.address, bytes = it.values.readByteArray(it.quantity * 2).toList())
                    assertEquals(writeMultipleRegisters, request)

                    val response = WriteMultipleRegistersResponse(expected.address, expected.quantity)
                    service.sendResponse(response)
                }
            }
        })

        test(writeMultipleRegisters, expected)
    }

    @Test
    @DisplayName("WriteMultipleRegisters: Timeout -> Timeout")
    fun testWriteMultipleRegisters_Timeout_when_timeout() {
        val writeMultipleRegisters = ModbusRequest.WriteMultipleRegisters(address = 0, bytes = listOf(0, 1))
        val expected = ModbusResponse.Timeout()

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onWriteMultipleRegisters(service: ServiceRequestHandler.ServiceRequest<WriteMultipleRegistersRequest, WriteMultipleRegistersResponse>) {
                service.request.use {
                    val request = ModbusRequest.WriteMultipleRegisters(address = it.address, bytes = it.values.readByteArray(it.quantity * 2).toList())
                    assertEquals(writeMultipleRegisters, request)

                    Thread.sleep(1500)
                }
            }
        })

        test(writeMultipleRegisters, expected, 1.0)
    }

    @Test
    @DisplayName("MaskWriteRegister: Valid request -> Success")
    fun testMaskWriteRegister_success_when_valid_request() {
        val maskWriteRegister = ModbusRequest.MaskWriteRegister(address = 0, andMask = 1, orMask = 2)
        val expected = ModbusResponse.MaskWriteRegister(address = maskWriteRegister.address, andMask = maskWriteRegister.andMask, orMask = maskWriteRegister.orMask)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onMaskWriteRegister(service: ServiceRequestHandler.ServiceRequest<MaskWriteRegisterRequest, MaskWriteRegisterResponse>) {
                val request = ModbusRequest.MaskWriteRegister(address = service.request.address, andMask = service.request.andMask, orMask = service.request.orMask)
                assertEquals(maskWriteRegister, request)

                val response = MaskWriteRegisterResponse(expected.address, expected.andMask, expected.orMask)
                service.sendResponse(response)
            }
        })

        test(maskWriteRegister, expected)
    }

    @Test
    @DisplayName("ReadWriteMultipleRegisters: Valid request -> Success")
    fun testReadWriteMultipleRegisters_success_when_valid_request() {
        val expected = ModbusResponse.ReadWriteMultipleRegisters(bytes = listOf(0, 1))
        val readWriteMultipleRegisters = ModbusRequest.ReadWriteMultipleRegisters(readAddress = 0, readQuantity = expected.bytes.size / 2, writeAddress = 10, bytes = listOf(1, 2))

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadWriteMultipleRegisters(service: ServiceRequestHandler.ServiceRequest<ReadWriteMultipleRegistersRequest, ReadWriteMultipleRegistersResponse>) {
                service.request.use {
                    val request = ModbusRequest.ReadWriteMultipleRegisters(readAddress = service.request.readAddress, readQuantity = service.request.readQuantity, writeAddress = service.request.writeAddress, bytes = service.request.values.readByteArray(service.request.writeQuantity * 2).toList())
                    assertEquals(readWriteMultipleRegisters, request)

                    val response = ReadWriteMultipleRegistersResponse(expected.bytes.toByteBuf())
                    service.sendResponse(response)
                }
            }
        })

        test(readWriteMultipleRegisters, expected)
    }

    @Test
    @DisplayName("Unusual Result: Acknowledge")
    fun testUnusualResult_Acknowledge() {
        val readCoils = ModbusRequest.ReadCoils(address = 0, quantity = 0)
        val expected = ModbusResponse.ExceptionCode(code = ModbusExceptionCode.Acknowledge)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadCoils(service: ServiceRequestHandler.ServiceRequest<ReadCoilsRequest, ReadCoilsResponse>) {
                val request = ModbusRequest.ReadCoils(address = service.request.address, quantity = service.request.quantity)
                assertEquals(readCoils, request)

                service.sendException(ExceptionCode.Acknowledge)
            }
        })

        test(readCoils, expected)
    }

    @Test
    @DisplayName("Unusual Result: MemoryParityError")
    fun testUnusualResult_MemoryParityError() {
        val readCoils = ModbusRequest.ReadCoils(address = 0, quantity = 0)
        val expected = ModbusResponse.ExceptionCode(code = ModbusExceptionCode.MemoryParityError)

        server.setRequestHandler(object : ServiceRequestHandler {
            override fun onReadCoils(service: ServiceRequestHandler.ServiceRequest<ReadCoilsRequest, ReadCoilsResponse>) {
                val request = ModbusRequest.ReadCoils(address = service.request.address, quantity = service.request.quantity)
                assertEquals(readCoils, request)

                service.sendException(ExceptionCode.MemoryParityError)
            }
        })

        test(readCoils, expected)
    }
}
