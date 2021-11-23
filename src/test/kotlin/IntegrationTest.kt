// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import aws.greengrass.labs.modbustcp.*
import com.aws.greengrass.integrationtests.ipc.IPCTestUtils
import com.aws.greengrass.lifecyclemanager.Kernel
import com.aws.greengrass.testcommons.testutilities.GGExtension
import com.aws.greengrass.testcommons.testutilities.TestUtils
import com.digitalpetri.modbus.requests.ReadCoilsRequest
import com.digitalpetri.modbus.responses.ReadCoilsResponse
import com.digitalpetri.modbus.slave.ModbusTcpSlave
import com.digitalpetri.modbus.slave.ModbusTcpSlaveConfig
import com.digitalpetri.modbus.slave.ServiceRequestHandler
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import software.amazon.awssdk.aws.greengrass.GreengrassCoreIPCClient
import java.nio.file.Path
import kotlin.test.*

private const val TEST_HOST = "localhost"
private const val TEST_PORT = 5020

private const val TEST_REQUEST_ID = "test"

private const val TEST_DEVICE_NAME = "test"
private val topicForTestRequest get() = "modbus/request/$TEST_DEVICE_NAME"
private val topicForTestResponse get() = "modbus/response/$TEST_DEVICE_NAME"

@ExtendWith(MockKExtension::class, GGExtension::class)
class IntegrationTest {
    private lateinit var kernel: Kernel

    companion object {
        @TempDir
        @JvmStatic
        lateinit var rootDir: Path
    }

    @BeforeTest
    fun beforeTest() {
        kernel = TestUtil.launchKernel(rootDir, "integration_test.yaml")
    }

    @AfterTest
    fun afterTest() {
        kernel.shutdown()
    }

    @Test
    @DisplayName("ReadCoils: Valid request -> Success")
    fun testReadCoils_success_when_valid_request() {
        val expected = ModbusResponse.ReadCoils(TEST_REQUEST_ID, bits = listOf(true, false, true, false, true, false, true, false, true, false, true, false))
        val readCoils = ModbusRequest.ReadCoils(TEST_REQUEST_ID, address = 0, quantity = expected.bits.size)

        withTestKernel(kernel, "Integration") {
            runBlocking {
                val server = ModbusTcpSlave(ModbusTcpSlaveConfig.Builder().build())
                server.setRequestHandler(object : ServiceRequestHandler {
                    override fun onReadCoils(service: ServiceRequestHandler.ServiceRequest<ReadCoilsRequest, ReadCoilsResponse>) {
                        val request = ModbusRequest.ReadCoils(TEST_REQUEST_ID, address = service.request.address, quantity = service.request.quantity)
                        assertEquals(readCoils, request)

                        val response = ReadCoilsResponse(expected.bits.toBytes().toByteBuf())
                        service.sendResponse(response)
                    }
                })

                server.bind(TEST_HOST, TEST_PORT).await()
                try {
                    ProtocolAdapter(coroutineContext).execute {
                        connectEventStreamIPC().use { connection ->
                            val ipcClient = GreengrassCoreIPCClient(connection)

                            val (future, consumer) = TestUtils.asyncAssertOnConsumer<ByteArray> { message ->
                                val response = ModbusResponse.fromJson(String(message))
                                assertEquals(expected, response)
                            }

                            IPCTestUtils.subscribeToTopicOveripcForBinaryMessages(ipcClient, topicForTestResponse, consumer)
                            IPCTestUtils.publishToTopicOverIpcAsBinaryMessage(ipcClient, topicForTestRequest, readCoils.toJson())

                            future.await()
                        }
                    }
                } finally {
                    server.shutdown()
                }
            }
        }
    }
}
