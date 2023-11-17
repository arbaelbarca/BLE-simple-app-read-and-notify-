package com.lorenzofelletti.simpleblescanner.blescanner

import java.util.UUID

class BleDeviceModel(
    val uuidService: UUID? = null,
    val uuidCharacteristic: UUID? = null,
    val name: String? = "",
    val address: String? = ""
)