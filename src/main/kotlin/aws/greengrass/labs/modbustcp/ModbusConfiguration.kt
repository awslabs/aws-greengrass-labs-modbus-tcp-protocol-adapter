// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

data class ModbusConfiguration(val endpoints: List<Endpoint>) {
    data class Endpoint(val host: String, val port: Int? = null, val timeout: Double? = null, val devices: List<Device>)
    data class Device(val name: String, val unitId: Int)
}
