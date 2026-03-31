package com.example.tempexporter.model

data class TemperatureResponse(
    val water: Double,
    val dhw: Double,
    val modul: Int,
    val state: Int,
    val err: Int,
    val id: Int,
    val type: Int,
    val name: String,
)
