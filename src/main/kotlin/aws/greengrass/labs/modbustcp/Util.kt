// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

import io.netty.buffer.ByteBuf
import kotlin.math.min

fun ByteBuf.readBitList(quantity: Int): List<Boolean> {
    val result = mutableListOf<Boolean>()

    val size = readableBytes()
    var remaining = quantity
    for(i in 0 until size) {
        var data = readByte().toInt()
        val bits = min(8, remaining)
        for(j in 0 until bits) {
            val bit = (data and 0x01 == 0x01)
            result.add(bit)

            data = data shr 1
            --remaining
        }
    }

    return result
}

fun ByteBuf.readByteArray(size: Int): ByteArray {
    val bytes = min(size, readableBytes())
    val result = ByteArray(bytes)
    readBytes(result)
    return result
}

fun List<Boolean>.toBytes(): List<Byte> {
    val result = mutableListOf<Byte>()

    val it = iterator()
    var remaining = size
    while(it.hasNext()) {
        var data = 0x00
        val bits = min(8, remaining)
        for(i in 0 until bits) {
            val bit = it.next()
            data = data or if (bit) 0x01 shl i else 0x00

            --remaining
        }

        result.add(data.toByte())
    }

    return result
}
