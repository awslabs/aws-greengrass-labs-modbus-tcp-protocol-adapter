// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val jsonForRequest = Json

fun ModbusRequest.toJson() = jsonForRequest.encodeToString(this)
fun ModbusRequest.Companion.fromJson(value: String): ModbusRequest = jsonForRequest.decodeFromString(value)

private val jsonForResponse = Json

fun ModbusResponse.toJson() = jsonForResponse.encodeToString(this)
fun ModbusResponse.Companion.fromJson(value: String): ModbusResponse = jsonForResponse.decodeFromString(value)
