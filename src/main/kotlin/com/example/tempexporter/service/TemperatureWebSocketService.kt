package com.example.tempexporter.service

import com.example.tempexporter.metrics.TemperatureMetrics
import com.example.tempexporter.model.TemperatureResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.util.retry.Retry
import java.net.URI
import java.time.Duration

@Service
class TemperatureWebSocketService(

    private val objectMapper: ObjectMapper,

    private val metrics: TemperatureMetrics,

    @Value("\${websocket.url}") private val wsUrl: String,

    @Value("\${websocket.user}") private val wsUser: String,

    @Value("\${websocket.pass}") private val wsPass: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = ReactorNettyWebSocketClient()

    @PostConstruct
    fun start() {
        connectWithRetry().subscribe()
    }

    private fun connectWithRetry(): Mono<Void> =
        connect()
            .doOnError { log.error("WebSocket error: ${it.message}") }
            .retryWhen(
                Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(5))
                    .doBeforeRetry { log.warn("Reconnecting to $wsUrl...") }
            )

    private fun connect(): Mono<Void> =
        client.execute(URI.create(wsUrl)) { session ->
            val authSink = Sinks.one<Boolean>()

            val authMessage = Flux.just(
                session.textMessage(
                    objectMapper.writeValueAsString(mapOf("user" to wsUser, "pass" to wsPass))
                )
            )

            val periodicRequests = authSink.asMono()
                .filter { authenticated -> authenticated }
                .flatMapMany {
                    Flux.interval(Duration.ZERO, Duration.ofSeconds(5))
                        .map { session.textMessage("""{"id":4100,"req_state":0}""") }
                }.log()

            val send = session.send(Flux.concat(authMessage, periodicRequests))

            val receive = session.receive()
                .map { it.payloadAsText }
                .doOnNext { text ->
                    val node = objectMapper.readTree(text)
                    when {
                        node.has("auth") -> {
                            val code = node["auth"].asInt()
                            if (code == 200) {
                                log.info("Authenticated successfully")
                                authSink.tryEmitValue(true)
                            } else {
                                log.error("Authentication failed, code: $code")
                                authSink.tryEmitValue(false)
                            }
                        }

                        node.has("water") -> {
                            val response = objectMapper.treeToValue(node, TemperatureResponse::class.java)
                            metrics.update(response)
                            log.debug("Metrics updated: water={}, dhw={}", response.water, response.dhw)
                        }

                        else -> log.warn("Unknown message: $text")
                    }
                }
                .then()

            Mono.zip(send, receive).then()
        }
}
