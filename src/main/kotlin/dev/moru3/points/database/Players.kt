package dev.moru3.points.database

import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.Table

object Players: Table("players") {
    val uniqueId = uuid("uniqueId").primaryKey()
    val name = varchar("name", 16)
}