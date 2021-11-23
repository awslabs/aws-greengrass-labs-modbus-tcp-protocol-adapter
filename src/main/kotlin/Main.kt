// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import aws.greengrass.labs.modbustcp.*
import kotlinx.coroutines.*

suspend fun main(args: Array<String>): Unit = coroutineScope {
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Invoked shutdown hook.")
        cancel()
    })

    try {
        ProtocolAdapter(coroutineContext).execute {
            while (isActive) {
                delay(5000)
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}
