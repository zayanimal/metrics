# Temperature Exporter

Spring Boot Kotlin WebFlux сервис, который подключается к WebSocket серверу котла, авторизуется, раз в 5 секунд запрашивает метрики температуры и экспортирует их в формате Prometheus (pull-модель).

## Требования

- Java 25+
- Maven 3.8+
- Запущенный WebSocket сервер (см. [Запуск сервера](#запуск-сервера))

## Быстрый старт

```bash
mvn spring-boot:run
```

Метрики доступны по адресу:

```
GET http://localhost:1099/actuator/prometheus
```

## Архитектура

```
WebSocket Server (ws://localhost:8089/ws)
        │
        │  1. {"user":"user","pass":"11223"}  →
        │  ← {"auth":200}
        │
        │  2. {"id":4100,"req_state":0}  →  (каждые 5 сек)
        │  ← {"water":44.0,"dhw":42.0,"modul":0,"state":0,"err":0,...}
        │
Temperature Exporter (Spring Boot WebFlux)
        │
        │  AtomicReference<Double> (потокобезопасное хранение)
        │
Prometheus (GET /actuator/prometheus)
```

### Компоненты

| Класс | Назначение |
|---|---|
| `TemperatureWebSocketService` | WebSocket клиент: авторизация, polling, reconnect |
| `TemperatureMetrics` | Регистрация и обновление Micrometer Gauges |
| `TemperatureResponse` | Data-класс для десериализации ответа сервера |

### Логика подключения (`TemperatureWebSocketService`)

1. При старте приложения (`@PostConstruct`) инициируется подключение к WebSocket серверу.
2. Первым сообщением отправляется auth-запрос с логином и паролем.
3. При получении `{"auth":200}` сигналится `Sinks.one<Boolean>` — это разблокирует `Flux.interval`, который раз в 5 секунд отправляет запрос температуры.
4. Ответ с температурой парсится и обновляет `AtomicReference` в `TemperatureMetrics`.
5. При разрыве соединения или любой ошибке — автоматическое переподключение через 5 секунд (`retryWhen`).

```
@PostConstruct
    └─► connectWithRetry()
            └─► connect()
                    ├─► send: [authMessage] → concat → [periodicRequests every 5s]
                    │                                        ↑
                    │                               разблокируется после auth
                    └─► receive: parse JSON
                            ├─► {"auth":200}  → authSink.tryEmitValue(true)
                            └─► {"water":...} → metrics.update(response)
```

## Метрики Prometheus

| Метрика | Тип | Описание |
|---|---|---|
| `boiler_water_temperature_celsius` | Gauge | Температура воды в контуре отопления |
| `boiler_dhw_temperature_celsius` | Gauge | Температура ГВС (горячее водоснабжение) |
| `boiler_modulation_state` | Gauge | Состояние модуляции (0 — норма) |
| `boiler_state` | Gauge | Состояние котла (0 — норма) |
| `boiler_error_code` | Gauge | Код ошибки (0 — нет ошибок) |

Пример вывода `/actuator/prometheus`:

```
# HELP boiler_water_temperature_celsius Water temperature in the heating system
# TYPE boiler_water_temperature_celsius gauge
boiler_water_temperature_celsius 44.0

# HELP boiler_dhw_temperature_celsius Domestic hot water temperature
# TYPE boiler_dhw_temperature_celsius gauge
boiler_dhw_temperature_celsius 42.0

# HELP boiler_modulation_state Module state
# TYPE boiler_modulation_state gauge
boiler_modulation_state 0.0

# HELP boiler_state Boiler state
# TYPE boiler_state gauge
boiler_state 0.0

# HELP boiler_error_code Boiler error code
# TYPE boiler_error_code gauge
boiler_error_code 0.0
```

## Конфигурация

Все параметры задаются в `src/main/resources/application.yml`:

```yaml
server:
  port: 8080              # порт HTTP сервера (actuator)

repeat:
  delay: ${WEBSOCKET_DELAY}  # интервал переподключения при разрыве соединения (секунды)

websocket:
  url: ${WEBSOCKET_URL}         # адрес WebSocket сервера
  user: ${WEBSOCKET_USER}       # логин для авторизации
  pass: ${WEBSOCKET_PASS}       # пароль для авторизации

management:
  endpoints:
    web:
      exposure:
        include: prometheus, health
```

Переменные необходимо задать через environment variables:

```bash
WEBSOCKET_DELAY=5 \
WEBSOCKET_URL=ws://192.168.1.10:8089/ws \
WEBSOCKET_USER=admin \
WEBSOCKET_PASS=secret \
mvn spring-boot:run
```

## Запуск сервера

WebSocket сервер находится в директории `../websocket-server`:

```bash
cd ../websocket-server
npm install
node server.js
```

Сервер запустится на `ws://localhost:8089/ws`.

## Docker

```bash
docker compose up -d
```

Контейнер доступен на порту `1099`. Переменные окружения задаются в `docker-compose.yaml`:

```yaml
environment:
  - WEBSOCKET_DELAY=5
  - WEBSOCKET_URL=ws://{server.ip}/ws
  - WEBSOCKET_USER=user
  - WEBSOCKET_PASS=11223
```

## Интеграция с Prometheus

Добавьте в `prometheus.yml` следующий scrape config:

```yaml
scrape_configs:
  - job_name: 'temperature-exporter'
    scrape_interval: 15s
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:1099']
```

## Сборка

```bash
# Собрать JAR
mvn clean package

# Запустить JAR
java -jar target/temperature-exporter-0.0.1-SNAPSHOT.jar
```
