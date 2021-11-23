// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

interface ModbusEndpoint {
    suspend fun connect()
    suspend fun disconnect()

    suspend fun readCoils(unitId: Int, address: Int, quantity: Int): List<Boolean>
    suspend fun readDiscreteInputs(unitId: Int, address: Int, quantity: Int): List<Boolean>
    suspend fun readHoldingRegisters(unitId: Int, address: Int, quantity: Int): List<Byte>
    suspend fun readInputRegisters(unitId: Int, address: Int, quantity: Int): List<Byte>

    suspend fun writeSingleCoil(unitId: Int, address: Int, value: Boolean): Pair<Int, Boolean>
    suspend fun writeSingleRegister(unitId: Int, address: Int, value: UShort): Pair<Int, UShort>

    suspend fun writeMultipleCoils(unitId: Int, address: Int, values: List<Boolean>): Pair<Int, Int>
    suspend fun writeMultipleRegisters(unitId: Int, address: Int, values: List<Byte>): Pair<Int, Int>

    suspend fun maskWriteRegister(unitId: Int, address: Int, andMask: Int, orMask: Int): Triple<Int, Int, Int>

    suspend fun readWriteMultipleRegisters(unitId: Int, readAddress: Int, readQuantity: Int, writeAddress: Int, values: List<Byte>): List<Byte>
}
