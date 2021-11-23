// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package aws.greengrass.labs.modbustcp

class EndpointTimeoutException : Exception {
    constructor()
    constructor(cause: Throwable) : super(cause)
}
