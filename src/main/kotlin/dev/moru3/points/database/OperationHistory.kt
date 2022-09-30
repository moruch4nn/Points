package dev.moru3.points.database

import org.jetbrains.exposed.sql.Table

object OperationHistory: Table("operation_history") {
    val id = long("id").primaryKey()
    val uniqueId = uuid("uniqueId")
    val cancelled = bool("cancelled").default(false)
}