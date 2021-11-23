// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import com.aws.greengrass.integrationtests.ipc.IPCTestUtils
import com.aws.greengrass.lifecyclemanager.Kernel
import com.aws.greengrass.testcommons.testutilities.NoOpPathOwnershipHandler
import com.aws.greengrass.util.Coerce
import com.github.stefanbirkner.systemlambda.SystemLambda
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.file.Path
import java.util.concurrent.Callable

operator fun <L, R> com.aws.greengrass.util.Pair<L, R>.component1(): L = left
operator fun <L, R> com.aws.greengrass.util.Pair<L, R>.component2(): R = right

object TestUtil {
    fun launchKernel(rootDir: Path, configFile: String) = Kernel().also { kernel ->
        NoOpPathOwnershipHandler.register(kernel)
        val filePath = javaClass.getResource(configFile)!!.toString()
        kernel.parseArgs("-r", rootDir.toAbsolutePath().toString(), "-i", filePath)
        kernel.launch()
    }

    fun getIpcServerSocketPath(kernel: Kernel): String? = Coerce.toString(kernel.config.root.lookup("setenv", "AWS_GG_NUCLEUS_DOMAIN_SOCKET_FILEPATH_FOR_COMPONENT"))
    fun getAuthToken(kernel: Kernel, serviceName: String): String? = IPCTestUtils.getAuthTokeForService(kernel, serviceName)
}

fun <T> withTestKernel(kernel: Kernel, serviceName: String, callable: Callable<T>) {
    val socketPath = TestUtil.getIpcServerSocketPath(kernel)
    val authToken = TestUtil.getAuthToken(kernel, serviceName)
    SystemLambda.withEnvironmentVariable("AWS_GG_NUCLEUS_DOMAIN_SOCKET_FILEPATH_FOR_COMPONENT", socketPath)
        .and("SVCUID", authToken).execute(callable)
}

fun List<Byte>.toByteBuf(): ByteBuf = Unpooled.wrappedBuffer(toByteArray())
