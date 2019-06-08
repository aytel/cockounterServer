package com.example.cockounter

import xyz.morphia.annotations.Entity
import xyz.morphia.annotations.Id
import java.util.*

@Entity
data class ClientAddress(
    @Id val uuid: UUID = UUID.randomUUID(),
    val token: String = ""
)