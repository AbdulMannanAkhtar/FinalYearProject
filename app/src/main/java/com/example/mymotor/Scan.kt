package com.example.mymotor

data class Scan (
    var scanID: String = "",
    var timestamp: Long = 0L,
    var obdResponse: String = "",
    //var description: String = ""
)

