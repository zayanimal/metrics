package com.example.tempexporter.metrics

import com.example.tempexporter.model.TemperatureResponse
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class TemperatureMetrics(registry: MeterRegistry) {

    private val water = AtomicReference(0.0)

    private val dhw   = AtomicReference(0.0)

    private val modul = AtomicReference(0.0)

    private val state = AtomicReference(0.0)

    private val err   = AtomicReference(0.0)

    init {
        Gauge.builder("boiler_water_temperature_celsius", water, AtomicReference<Double>::get)
            .description("Water temperature in the heating system")
            .register(registry)

        Gauge.builder("boiler_dhw_temperature_celsius", dhw, AtomicReference<Double>::get)
            .description("Domestic hot water temperature")
            .register(registry)

        Gauge.builder("boiler_modulation_state", modul, AtomicReference<Double>::get)
            .description("Module state")
            .register(registry)

        Gauge.builder("boiler_state", state, AtomicReference<Double>::get)
            .description("Boiler state")
            .register(registry)

        Gauge.builder("boiler_error_code", err, AtomicReference<Double>::get)
            .description("Boiler error code")
            .register(registry)
    }

    fun update(response: TemperatureResponse) {
        water.set(response.water)
        dhw.set(response.dhw)
        modul.set(response.modul.toDouble())
        state.set(response.state.toDouble())
        err.set(response.err.toDouble())
    }
}
