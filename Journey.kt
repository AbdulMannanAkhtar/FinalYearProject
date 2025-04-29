package com.example.mymotor

data class Journey(
    var id: String = "",
    var name: String = "",
    var to: String = "",
    var from: String = "",
    var timestamp: Long = 0L,
    var startLat: Double = 0.0,
    var startLng: Double = 0.0,
    var startFuel: Float = 0f,
    var endFuel: Float = 0f,
    var usedFuel: Float = 0f,
    var distance: String = "",
    var timeTaken: String = "",
    var endTime: Long = 0L,
    var avgSpeed: Float = 0f,
    var endLat: Double = 0.0,
    var endLng: Double = 0.0,
    var totalAccelerations: Int = 0,
    var aggressiveAccelerations: Int = 0,
    var totalBrakings: Int = 0,
    var aggressiveBrakings: Int = 0,
    var drivingStyle: String = "",
    var accTime: Float = 0f,
    var brakeTime: Float = 0f
)
