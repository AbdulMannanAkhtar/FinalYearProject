package com.example.mymotor

data class Schedule (
    var id: String = "",
    var name: String = "",
    var date: Long = 0L,
    var reminder1: Int = 30,
    var reminder2: Int = 7
)