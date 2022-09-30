package dev.moru3.points.database

import org.jetbrains.exposed.sql.CurrentDateTime
import org.jetbrains.exposed.sql.Table

object Histories: Table("histories") {
    val id = long("id")
    val uniqueId = uuid("uniqueId")
    val point = long("point")
    val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime())
    val cancelled = bool("cancelled").default(false)
}