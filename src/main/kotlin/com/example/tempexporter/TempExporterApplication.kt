package com.example.tempexporter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TempExporterApplication

fun main(args: Array<String>) {
    runApplication<TempExporterApplication>(*args)
}
