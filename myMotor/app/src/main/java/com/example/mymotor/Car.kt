package com.example.mymotor

data class Car(
    val make: String,
    val model: String,
    val year: Int,
    val imageUri: String? = null
) {
}