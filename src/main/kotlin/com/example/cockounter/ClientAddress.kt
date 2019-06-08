package com.example.cockounter

import xyz.morphia.annotations.Entity
import xyz.morphia.annotations.Id
import xyz.morphia.annotations.Indexed
import java.util.*

@Entity
data class ClientAddress(
    @Indexed val uuid: UUID = UUID.randomUUID(),
    @Indexed val token: String = "",
    @Id val shadowedUUId: UUID = UUID.randomUUID()
)