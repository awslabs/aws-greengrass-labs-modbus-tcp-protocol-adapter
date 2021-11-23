// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import aws.greengrass.labs.modbustcp.*
import com.aws.greengrass.integrationtests.ipc.IPCTestUtils
import com.aws.greengrass.lifecyclemanager.Kernel
import com.aws.greengrass.testcommons.testutilities.GGExtension
import com.aws.greengrass.testcommons.testutilities.TestUtils
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import software.amazon.awssdk.aws.greengrass.GreengrassCoreIPCClient
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TEST_DEVICE_NAME = "test"
private const val TEST_UNIT_ID = 0
private const val TEST_REQUEST_ID = "test"

private val topicForTestRequest get() = "modbus/request/$TEST_DEVICE_NAME"
private val topicForTestResponse get() = "modbus/response/$TEST_DEVICE_NAME"

@ExtendWith(MockKExtension::class, GGExtension::class)
class DeviceTest {
    private lateinit var kernel: Kernel

    companion object {
        @TempDir
        @JvmStatic
        lateinit var rootDir: Path
    }

    @BeforeTest
    fun beforeTest() {
        kernel = TestUtil.launchKernel(rootDir, "device_test.yaml")
    }

    @AfterTest
    fun afterTest() {
        kernel.shutdown()
    }

    private fun test(request: ModbusRequest, expected: ModbusResponse, endpoint: ModbusEndpoint) {
        withTestKernel(kernel, "SubscribeAndPublish") {
            runBlocking {
                connectEventStreamIPC().use { connection ->
                    val ipcClient = GreengrassCoreIPCClient(connection)

                    val manager = DeviceManager(ipcClient, coroutineContext)
                    manager.addDevice(ModbusConfiguration.Device(TEST_DEVICE_NAME, TEST_UNIT_ID), endpoint)
                    manager.connect()
                    try {
                        val (future, consumer) = TestUtils.asyncAssertOnConsumer<ByteArray> { message ->
                            val response = ModbusResponse.fromJson(String(message))
                            assertEquals(expected, response)
                        }

                        IPCTestUtils.subscribeToTopicOveripcForBinaryMessages(ipcClient, topicForTestResponse, consumer)
                        IPCTestUtils.publishToTopicOverIpcAsBinaryMessage(ipcClient, topicForTestRequest, request.toJson())

                        future.await()
                    } finally {
                        manager.disconnect()
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("ReadCoils: Valid request -> Success")
    fun testReadCoils_success_when_valid_request(@MockK endpoint: ModbusEndpoint) {
        val expected = ModbusResponse.ReadCoils(TEST_REQUEST_ID, bits = listOf(true, false, true, false, true, false, true, false, true, false, true, false))
        val readCoils = ModbusRequest.ReadCoils(TEST_REQUEST_ID, address = 0, quantity = expected.bits.size)

        coEvery {
            endpoint.readCoils(TEST_UNIT_ID, readCoils.address, readCoils.quantity)
        } returns expected.bits

        test(readCoils, expected, endpoint)
    }

    @Test
    @DisplayName("ReadCoils: Unsupported -> IllegalFunction")
    fun testReadCoils_IllegalFunction_when_unsupported(@MockK endpoint: ModbusEndpoint) {
        val readCoils = ModbusRequest.ReadCoils(TEST_REQUEST_ID, address = 0, quantity = 0)
        val expected = ModbusResponse.ExceptionCode(TEST_REQUEST_ID, ModbusExceptionCode.IllegalFunction)

        coEvery {
            endpoint.readCoils(TEST_UNIT_ID, readCoils.address, readCoils.quantity)
        } throws ModbusException(expected.code)

        test(readCoils, expected, endpoint)
    }

    @Test
    @DisplayName("ReadDiscreteInputs: Valid request -> Success")
    fun testReadDiscreteInputs_success_when_valid_request(@MockK endpoint: ModbusEndpoint) {
        val expected = ModbusResponse.ReadDiscreteInputs(TEST_REQUEST_ID, bits = listOf(false, true, false, true, false, true, false, true))
        val readDiscreteInputs = ModbusRequest.ReadDiscreteInputs(TEST_REQUEST_ID, address = 0, quantity = expected.bits.size)

        coEvery {
            endpoint.readDiscreteInputs(TEST_UNIT_ID, readDiscreteInputs.address, readDiscreteInputs.quantity)
        } returns expected.bits

        test(readDiscreteInputs, expected, endpoint)
    }

    @Test
    @DisplayName("ReadDiscreteInputs: Invalid address -> IllegalDataAddress")
    fun testReadDiscreteInputs_IllegalDataAddress_when_invalid_address(@MockK endpoint: ModbusEndpoint) {
        val readDiscreteInputs = ModbusRequest.ReadDiscreteInputs(TEST_REQUEST_ID, address = 0, quantity = 1)
        val expected = ModbusResponse.ExceptionCode(TEST_REQUEST_ID, ModbusExceptionCode.IllegalDataAddress)

        coEvery {
            endpoint.readDiscreteInputs(TEST_UNIT_ID, readDiscreteInputs.address, readDiscreteInputs.quantity)
        } throws ModbusException(expected.code)

        test(readDiscreteInputs, expected, endpoint)
    }

    @Test
    @DisplayName("ReadHoldingRegisters: Valid request -> Success")
    fun testReadHoldingRegisters_success_when_valid_request(@MockK endpoint: ModbusEndpoint) {
        val expected = ModbusResponse.ReadHoldingRegisters(TEST_REQUEST_ID, bytes = listOf(0, 1))
        val readHoldingRegisters = ModbusRequest.ReadHoldingRegisters(TEST_REQUEST_ID, address = 0, quantity = expected.bytes.size / 2)

        coEvery {
            endpoint.readHoldingRegisters(TEST_UNIT_ID, readHoldingRegisters.address, readHoldingRegisters.quantity)
        } returns expected.bytes

        test(readHoldingRegisters, expected, endpoint)
    }

    @Test
    @DisplayName("ReadHoldingRegisters: Failure -> SlaveDeviceFailure")
    fun testReadHoldingRegisters_SlaveDeviceFailure_when_failure(@MockK endpoint: ModbusEndpoint) {
        val readHoldingRegisters = ModbusRequest.ReadHoldingRegisters(TEST_REQUEST_ID, address = 0, quantity = 1)
        val expected = ModbusResponse.ExceptionCode(TEST_REQUEST_ID, ModbusExceptionCode.SlaveDeviceFailure)

        coEvery {
            endpoint.readHoldingRegisters(TEST_UNIT_ID, readHoldingRegisters.address, readHoldingRegisters.quantity)
        } throws ModbusException(expected.code)

        test(readHoldingRegisters, expected, endpoint)
    }

    @Test
    @DisplayName("ReadInputRegisters: Valid request -> Success")
    fun testReadInputRegisters_success_when_valid_request(@MockK endpoint: ModbusEndpoint) {
        val expected = ModbusResponse.ReadInputRegisters(TEST_REQUEST_ID, bytes = listOf(0, 1))
        val readInputRegisters = ModbusRequest.ReadInputRegisters(TEST_REQUEST_ID, address = 0, quantity = expected.bytes.size / 2)

        coEvery {
            endpoint.readInputRegisters(TEST_UNIT_ID, readInputRegisters.address, readInputRegisters.quantity)
        } returns expected.bytes

        test(readInputRegisters, expected, endpoint)
    }

    @Test
    @DisplayName("ReadInputRegisters: Busy -> SlaveDeviceBusy")
    fun testReadInputRegisters_SlaveDeviceBusy_when_busy(@MockK endpoint: ModbusEndpoint) {
        val readInputRegisters = ModbusRequest.ReadInputRegisters(TEST_REQUEST_ID, address = 0, quantity = 1)
        val expected = ModbusResponse.ExceptionCode(TEST_REQUEST_ID, ModbusExceptionCode.SlaveDeviceBusy)

        coEvery {
            endpoint.readInputRegisters(TEST_UNIT_ID, readInputRegisters.address, readInputRegisters.quantity)
        } throws ModbusException(expected.code)

        test(readInputRegisters, expected, endpoint)
    }

    @Test
    @DisplayName("WriteSingleCoil: Valid request -> Success")
    fun testWriteSingleCoil_success_when_valid_request(@MockK endpoint: ModbusEndpoint) {
        val writeSingleCoil = ModbusRequest.WriteSingleCoil(TEST_REQUEST_ID, address = 0, value = true)
        val expected = ModbusResponse.WriteSingleCoil(TEST_REQUEST_ID, writeSingleCoil.address, writeSingleCoil.value)

        coEvery {
            endpoint.writeSingleCoil(TEST_UNIT_ID, writeSingleCoil.address, writeSingleCoil.value)
        } returns (expected.address to expected.value)

        test(writeSingleCoil, expected, endpoint)
    }

    @Test
    @DisplayName("WriteSingleCoil: Invalid value -> IllegalDataValue")
    fun testWriteSingleCoil_IllegalDataValue_when_invalid_value(@MockK endpoint: ModbusEndpoint) {
        val writeSingleCoil = ModbusRequest.WriteSingleCoil(TEST_REQUEST_ID, address = 0, value = false)
        val expected = ModbusResponse.ExceptionCode(TEST_REQUEST_ID, ModbusExceptionCode.IllegalDataValue)

        coEvery {
            endpoint.writeSingleCoil(TEST_UNIT_ID, writeSingleCoil.address, writeSingleCoil.value)
        } throws ModbusException(expected.code)

        test(writeSingleCoil, expected, endpoint)
    }

    @Test
    @DisplayName("WriteSingleRegister: Valid request -> Success")
    fun testWriteSingleRegister_success_when_valid_request(@MockK endpoint: ModbusEndpoint) {
        val writeSingleRegister = ModbusRequest.WriteSingleRegister(TEST_REQUEST_ID, address = 0, value = 1U)
        val expected = ModbusResponse.WriteSingleRegister(TEST_REQUEST_ID, writeSingleRegister.address, writeSingleRegister.value)

        coEvery {
            endpoint.writeSingleRegister(TEST_UNIT_ID, writeSingleRegister.address, writeSingleRegister.value)
        } returns (expected.address to expected.value)

        test(writeSingleRegister, expected, endpoint)
    }

    @Test
    @DisplayName("WriteSingleRegister: Unavailable -> GatewayPathUnavailable")
    fun testWriteSingleRegister_GatewayPathUnavailable_when_unavailable(@MockK endpoint: ModbusEndpoint) {
        val writeSingleRegister = ModbusRequest.WriteSingleRegister(TEST_REQUEST_ID, address = 0, value = 0U)
        val expected = ModbusResponse.ExceptionCode(TEST_REQUEST_ID, ModbusExceptionCode.GatewayPathUnavailable)

        coEvery {
            endpoint.writeSingleRegister(TEST_UNIT_ID, writeSingleRegister.address, writeSingleRegister.value)
        } throws ModbusException(expected.code)

        test(writeSingleRegister, expected, endpoint)
    }

    @Test
    @DisplayName("WriteMultipleCoils: Valid request -> Success")
    fun testWriteMultipleCoils_success_when_valid_request(@MockK endpoint: ModbusEndpoint) {
        val writeMultipleCoils = ModbusRequest.WriteMultipleCoils(TEST_REQUEST_ID, address = 0, bits = listOf(true, false, true, false, true, false, true, false))
        val expected = ModbusResponse.WriteMultipleCoils(TEST_REQUEST_ID, address = writeMultipleCoils.address, quantity = writeMultipleCoils.bits.size)

        coEvery {
            endpoint.writeMultipleCoils(TEST_UNIT_ID, writeMultipleCoils.address, writeMultipleCoils.bits)
        } returns (expected.address to expected.quantity)

        test(writeMultipleCoils, expected, endpoint)
    }

    @Test
    @DisplayName("WriteMultipleCoils: Device_unavailable -> GatewayTargetDeviceFailedToResponse")
    fun testWriteMultipleCoils_GatewayTargetDeviceFailedToResponse_when_device_unavailable(@MockK endpoint: ModbusEndpoint) {
        val writeMultipleCoils = ModbusRequest.WriteMultipleCoils(TEST_REQUEST_ID, address = 0, bits = listOf(false, true, false, true, false, true, false, true, false, true, false, true))
        val expected = ModbusResponse.ExceptionCode(TEST_REQUEST_ID, code = ModbusExceptionCode.GatewayTargetDeviceFailedToResponse)

        coEvery {
            endpoint.writeMultipleCoils(TEST_UNIT_ID, writeMultipleCoils.address, writeMultipleCoils.bits)
        } throws ModbusException(expected.code)

        test(writeMultipleCoils, expected, endpoint)
    }

    @Test
    @DisplayName("WriteMultipleRegisters: Valid request -> Success")
    fun testWriteMultipleRegisters_success_when_valid_request(@MockK endpoint: ModbusEndpoint) {
        val writeMultipleRegisters = ModbusRequest.WriteMultipleRegisters(TEST_REQUEST_ID, address = 0, bytes = listOf(0, 1))
        val expected = ModbusResponse.WriteMultipleRegisters(TEST_REQUEST_ID, writeMultipleRegisters.address, writeMultipleRegisters.bytes.size / 2)

        coEvery {
            endpoint.writeMultipleRegisters(TEST_UNIT_ID, writeMultipleRegisters.address, writeMultipleRegisters.bytes)
        } returns (expected.address to expected.quantity)

        test(writeMultipleRegisters, expected, endpoint)
    }

    @Test
    @DisplayName("WriteMultipleRegisters: Timeout -> Timeout")
    fun testWriteMultipleRegisters_Timeout_when_timeout(@MockK endpoint: ModbusEndpoint) {
        val writeMultipleRegisters = ModbusRequest.WriteMultipleRegisters(TEST_REQUEST_ID, address = 0, bytes = listOf(0, 1))
        val expected = ModbusResponse.Timeout(TEST_REQUEST_ID)

        coEvery {
            endpoint.writeMultipleRegisters(TEST_UNIT_ID, writeMultipleRegisters.address, writeMultipleRegisters.bytes)
        } throws EndpointTimeoutException()

        test(writeMultipleRegisters, expected, endpoint)
    }

    @Test
    @DisplayName("MaskWriteRegister: Valid request -> Success")
    fun testMaskWriteRegister_success_when_valid_request(@MockK endpoint: ModbusEndpoint) {
        val maskWriteRegister = ModbusRequest.MaskWriteRegister(TEST_REQUEST_ID, address = 0, andMask = 1, orMask = 2)
        val expected = ModbusResponse.MaskWriteRegister(TEST_REQUEST_ID, maskWriteRegister.address, maskWriteRegister.andMask, maskWriteRegister.orMask)

        coEvery {
            endpoint.maskWriteRegister(TEST_UNIT_ID, maskWriteRegister.address, maskWriteRegister.andMask, maskWriteRegister.orMask)
        } returns Triple(expected.address, expected.andMask, expected.orMask)

        test(maskWriteRegister, expected, endpoint)
    }

    @Test
    @DisplayName("ReadWriteMultipleRegisters: Valid request -> Success")
    fun testReadWriteMultipleRegisters_success_when_valid_request(@MockK endpoint: ModbusEndpoint) {
        val expected = ModbusResponse.ReadWriteMultipleRegisters(TEST_REQUEST_ID, bytes = listOf(0, 1))
        val readWriteMultipleRegisters = ModbusRequest.ReadWriteMultipleRegisters(TEST_REQUEST_ID, readAddress = 0, readQuantity = expected.bytes.size / 2, writeAddress = 10, bytes = listOf(1, 2))

        coEvery {
            endpoint.readWriteMultipleRegisters(TEST_UNIT_ID, readWriteMultipleRegisters.readAddress, readWriteMultipleRegisters.readQuantity, readWriteMultipleRegisters.writeAddress, readWriteMultipleRegisters.bytes)
        } returns expected.bytes

        test(readWriteMultipleRegisters, expected, endpoint)
    }

    @Test
    @DisplayName("MaskWriteRegister: Bad request -> BadRequest")
    fun testRequest_BadRequest_when_bad_request(@MockK endpoint: ModbusEndpoint) {
        val expected = ModbusResponse.BadRequest()

        runBlocking {
            IPCTestUtils.getEventStreamRpcConnection(kernel, "SubscribeAndPublish").use { connection ->
                val ipcClient = GreengrassCoreIPCClient(connection)

                val manager = DeviceManager(ipcClient, coroutineContext)
                manager.addDevice(ModbusConfiguration.Device(TEST_DEVICE_NAME, TEST_UNIT_ID), endpoint)
                manager.connect()
                try {
                    val (future, consumer) = TestUtils.asyncAssertOnConsumer<ByteArray> { message ->
                        val response = ModbusResponse.fromJson(String(message))
                        assertEquals(expected, response)
                    }

                    IPCTestUtils.subscribeToTopicOveripcForBinaryMessages(ipcClient, topicForTestResponse, consumer)
                    IPCTestUtils.publishToTopicOverIpcAsBinaryMessage(ipcClient, topicForTestRequest, "Bad Request")

                    future.await()
                } finally {
                    manager.disconnect()
                }
            }
        }
    }

    @Test
    @DisplayName("Unusual Error: Unexpected exception -> BadRequest")
    fun testUnusualError_BadRequest_when_unexpected_exception(@MockK endpoint: ModbusEndpoint) {
        val readCoils = ModbusRequest.ReadCoils(TEST_REQUEST_ID, address = 0, quantity = 0)
        val expected = ModbusResponse.BadRequest(TEST_REQUEST_ID)

        coEvery {
            endpoint.readCoils(TEST_UNIT_ID, readCoils.address, readCoils.quantity)
        } throws NullPointerException()

        test(readCoils, expected, endpoint)
    }

    @Test
    @DisplayName("Unusual Result: Acknowledge")
    fun testUnusualResult_Acknowledge(@MockK endpoint: ModbusEndpoint) {
        val readCoils = ModbusRequest.ReadCoils(TEST_REQUEST_ID, address = 0, quantity = 0)
        val expected = ModbusResponse.ExceptionCode(TEST_REQUEST_ID, ModbusExceptionCode.Acknowledge)

        coEvery {
            endpoint.readCoils(TEST_UNIT_ID, readCoils.address, readCoils.quantity)
        } throws ModbusException(expected.code)

        test(readCoils, expected, endpoint)
    }

    @Test
    @DisplayName("Unusual Result: MemoryParityError")
    fun testUnusualResult_MemoryParityError(@MockK endpoint: ModbusEndpoint) {
        val readCoils = ModbusRequest.ReadCoils(TEST_REQUEST_ID, address = 0, quantity = 0)
        val expected = ModbusResponse.ExceptionCode(TEST_REQUEST_ID, ModbusExceptionCode.MemoryParityError)

        coEvery {
            endpoint.readCoils(TEST_UNIT_ID, readCoils.address, readCoils.quantity)
        } throws ModbusException(expected.code)

        test(readCoils, expected, endpoint)
    }
}
