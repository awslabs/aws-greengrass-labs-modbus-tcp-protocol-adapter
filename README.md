# Greengrass Modbus TCP Protocol Adapter

This component provides the ability to read / write device data using Modbus TCP protocol.<br>
The basic usage of this component is based on the same concept as Modbus RTU protocol adapter.<br>
In addition, planning to provide read / write capabilities for asset data based on simple asset modeling feature.<br>

## Installation

You can either use a prebuilt binary or build yourself.

### Use prebuilt binary

Download the following files from [releases](https://github.com/awslabs/aws-greengrass-labs-modbus-tcp-protocol-adapter/releases) page.

- `ModbusTCP-<version>.jar`: Executable JAR file
- `aws.greengrass.labs.ModbusTCP-<version>.yaml`: Recipe template

Edit the following placeholders in recipe template.

- `<MODBUS_TCP_HOST>`: Host name or IP address of Modbus TCP device.
- `<MODBUS_TCP_PORT>`: Port number of Modbus TCP device.
- `<MODBUS_TCP_UNIT_NAME>`: Unique name of Modbus TCP device. Used as the last part of IPC topic.
- `<BUCKET_NAME>`: S3 bucket name, if uploaded ModbusTCP-1.0.0.jar to your S3 bucket.

Follow the developer guide to [publish](https://docs.aws.amazon.com/greengrass/v2/developerguide/publish-components.html#publish-component-shell-commands) Modbus TCP protocol adapter or [deploy locally](https://docs.aws.amazon.com/greengrass/v2/developerguide/gg-cli-deployment.html#deployment-create).

### How to build

If you choose to build yourself, you can build by the following steps below.

#### Prerequisite

Install Java Development Kit 11 or above. We recommend that you use [Amazon Corretto 11](http://aws.amazon.com/corretto/) or [OpenJDK 11](https://openjdk.java.net/).

#### Build

Checkout this repository, `cd` to the repository root, and execute the following command.

For Linux
```bash
./gradlew jar
```

For Windows
```
.\gradlew jar
```

If successful, a JAR file will be created in `build/libs`.

## Usage

### Configure the component

This component has the following `ComponentConfiguration` items.

- `Modbus`: Root of Modbus related configuration.<br>
  - `Endpoints`: Modbus TCP endpoint configuration.<br>
    - `Host`: Host name or IP address of the Modbus TCP server.<br>
    - `Port`: (Optional) Port number of the Modbus TCP server. Default is `502`.<br>
    - `Timeout`: (Optional) Modbus TCP communication timeout in seconds. Default is `5`.<br>
    - `Devices`: Modbus TCP device (unit) configuration.<br>
      - `Name`: Name of the device (unit).<br>
      - `UnitId`: (Optional) Unit id of the device (unit). Default is `0`<br>

#### Example
```
ComponentConfiguration:
  DefaultConfiguration:
    Modbus:
      Endpoints:
      - Host: "localhost"
        Port: 5020
        Devices:
        - Name: "test"
          UnitId: 0
```

### Send Modbus TCP command using Greengrass IPC local messages

You can send a command to Modbus device by publishing a request to the following topic.

- `modbus/request/{device name}`

#### Sample request
```
{
  "id": "TestRequest",
  "function": "ReadCoils",
  "address": 1,
  "quantity": 1
}
```

And you can receive the result from the device as a response by subscribing to the following topic.

- `modbus/response/{device name}`

#### Sample response
```
{
  "id": "TestRequest",
  "type": "ReadCoils",
  "bits": [true]
}
```

#### Sample error response
```
{
  "id": "TestRequest",
  "type": "ExceptionCode",
  "code": "IllegalDataAddress"
}
```

```
{
  "id": "TestRequest",
  "type": "Timeout"
}
```

```
{
  "type": "BadRequest"
}
```

## Supported commands

### Common parameters

- `id`: string<br>
    An arbitrary ID for the request. Use this property to map an input request to an output response.

### ReadCoils

Request
- `function`: "ReadCoils"
- `address`: integer<br>
    Starting address of the coils for read.
- `quantity`: integer<br>
    Quantity of the coils for read.

Response
- `type`: "ReadCoils"
- `bits`: [boolean]<br>
    Values of the coils.

### ReadDiscreteInputs

Read discrete inputs

Request
- `function`: "ReadDiscreteInputs"
- `address`: integer<br>
    Starting address of the discrete inputs for read.
- `quantity`: integer<br>
    Quantity of the discrete inputs for read.

Response
- `type`: "ReadDiscreteInputs"
- `bits`: [boolean]<br>
    Values of the discrete inputs.

### ReadHoldingRegisters

Read holding registers

Request
- `function`: "ReadHoldingRegisters"
- `address`: integer<br>
    Starting address of the holding registers for read.
- `quantity`: integer<br>
    Quantity of the holding registers for read.

Response
- `type`: "ReadHoldingRegisters"
- `bytes`: [integer]<br>
    Values of the holding registers.

### ReadInputRegisters

Read input registers

Request
- `function`: "ReadInputRegisters"
- `address`: integer<br>
    Starting address of the input registers for read.
- `quantity`: integer<br>
    Quantity of the input registers for read.

Response
- `type`: "ReadInputRegisters"
- `bytes`: [integer]<br>
    Values of the input registers.

### WriteSingleCoil

Write single coil

Request
- `function`: "WriteSingleCoil"
- `address`: integer<br>
    Address of the coil for write.
- `value`: boolean<br>
    Value for write

Response
- `type`: "WriteSingleCoil"
- `address`: integer
- `value`: boolean<br>
    Same as the request

### WriteSingleRegister

Write single register

Request
- `function`: "WriteSingleRegister"
- `address`: integer<br>
    Address of the register for write.
- `value`: integer<br>
    Value for write

Response
- `type`: "WriteSingleRegister"
- `address`: integer
- `value`: integer<br>
    Same as the request

### WriteMultipleCoils

Write multiple coils

Request
- `function`: "WriteMultipleCoils"
- `address`: integer<br>
    Starting address of the coils for write.
- `bits`: [boolean]<br>
    Values for write.

Response
- `type`: "WriteMultipleCoils"
- `address`: integer<br>
    Same as the request
- `quantity`: integer<br>
    Quantity of written values

### WriteMultipleRegisters

Write multiple registers

Request
- `function`: "WriteMultipleRegisters"
- `address`: integer<br>
    Starting address of the registers for write.
- `bytes`: [integer]<br>
    Values for write.

Response
- `type`: "WriteMultipleRegisters"
- `address`: integer<br>
    Same as the request
- `quantity`: integer<br>
    Quantity of written values

### MaskWriteRegister

Mask write register

Request
- `function`: "MaskWriteRegister"
- `address`: integer<br>
    Address of the register for mask.
- `andMask`: integer<br>
    AND mask
- `orMask`: integer<br>
    OR mask

Response
- `type`: "MaskWriteRegister"
- `address`: integer
- `andMask`: integer
- `orMask`: integer<br>
    Same as the request

### ReadWriteMultipleRegisters

Read and write multiple registers

Request
- `function`: "ReadWriteMultipleRegisters"
- `readAddress`: integer<br>
    Starting address of the registers for read.
- `readQuantity`: integer<br>
    Quantity of the registers for read.
- `writeAddress`: integer<br>
    Starting address of the registers for write.
- `bytes`: [integer]<br>
    Values for write.

Response
- `type`: "ReadWriteMultipleRegisters"
- `bytes`: [integer]<br>
    Values of the registers.

### Error response

- `type`: "ExceptionCode"
- `code`: string<br>
    Exception code of the error.

## External libraries

This component includes the following external libraries:

- Digital Petri Modbus

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.
