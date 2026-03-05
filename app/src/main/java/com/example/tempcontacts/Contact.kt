package com.example.tempcontacts

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val website: String = "",
    val notes: String = "",
    val tag: String = "None",
    val deletionTimestamp: Long? = null
)
