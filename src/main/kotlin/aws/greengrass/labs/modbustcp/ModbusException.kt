// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

import kotlinx.serialization.Serializable

@Serializable
enum class ModbusExceptionCode {
    IllegalFunction,
    IllegalDataAddress,
    IllegalDataValue,
    SlaveDeviceFailure,
    Acknowledge,
    SlaveDeviceBusy,
    MemoryParityError,
    GatewayPathUnavailable,
    GatewayTargetDeviceFailedToResponse,
}

class ModbusException : Exception {
    val code: ModbusExceptionCode

    constructor(code: ModbusExceptionCode) : super() {
        this.code = code
    }

    constructor(code: ModbusExceptionCode, cause: Throwable) : super(cause) {
        this.code = code
    }
}
