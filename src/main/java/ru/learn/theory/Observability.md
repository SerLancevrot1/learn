# Observability: метрики, логи, трейсы

> Конспект-тренажёр для подготовки к собеседованиям.
> PromQL-правила, алерт и примеры агрегации проверены через `promtool test rules` на **Prometheus 3.15.1**; поведение `up` и статистика кардинальности — на запущенном сервере Prometheus 3.15.1. Метрики Micrometer — на **Micrometer 1.17.2** и **OpenJDK 25**, вывод реальный. Grafana, Loki, Tempo и Alloy в среде проверки не запускались: их конфигурации и запросы приведены по документации. Исходный конспект основан на видео «Основы observability распределенных систем (Prometheus, Grafana, Alloy, Loki, Tempo)».

## Как пользоваться файлом

Каждая тема устроена одинаково:

1. **Короткий ответ** — то, что нужно сказать устно за 30–60 секунд.
2. **Подробности** — под спойлером `<details>`: механизм, ограничения, примеры.
3. **Уточняющие вопросы** — то, что интервьюер спросит следом.
4. **Практика** — запрос, правило или сценарий разбора инцидента.

Отмечайте уверенность прямо в файле:

- `[ ]` — не знаю;
- `[~]` — отвечаю с подсказкой;
- `[x]` — объясняю сам, с примером.

Повторяйте прежде всего то, что отмечено `[ ]` и `[~]`.

---

## Содержание

0. [Быстрое повторение](#0-быстрое-повторение)
1. [Мониторинг и observability](#1-мониторинг-и-observability)
2. [Сигналы телеметрии](#2-сигналы-телеметрии)
3. [Prometheus: модель сбора](#3-prometheus-модель-сбора)
4. [Типы метрик](#4-типы-метрик)
5. [PromQL: необходимый минимум](#5-promql-необходимый-минимум)
6. [Кардинальность](#6-кардинальность)
7. [Что измерять: RED, USE, SLO](#7-что-измерять-red-use-slo)
8. [Алертинг](#8-алертинг)
9. [Логи и Loki](#9-логи-и-loki)
10. [Трейсы, OpenTelemetry и Tempo](#10-трейсы-opentelemetry-и-tempo)
11. [Сбор телеметрии: Alloy и OTel Collector](#11-сбор-телеметрии-alloy-и-otel-collector)
12. [Grafana](#12-grafana)
13. [Java: Micrometer и Actuator](#13-java-micrometer-и-actuator)
14. [Разбор инцидента](#14-разбор-инцидента)
15. [Пробное собеседование](#15-пробное-собеседование)
16. [Источники](#16-источники)
- [Приложение: файлы проверки](#приложение-файлы-проверки)

---

## 0. Быстрое повторение

Ответьте вслух, не открывая разделы. Ответы — по ссылкам.

| Уверенность | Вопрос | Раздел |
|---|---|---|
| `[ ]` | Чем observability отличается от мониторинга? | [1](#1-мониторинг-и-observability) |
| `[ ]` | Чем лог отличается от трейса? | [2](#2-сигналы-телеметрии) |
| `[ ]` | Почему Prometheus использует pull? Что делать с короткими job? | [3.1](#31-pull-а-не-push) |
| `[ ]` | Кто маршрутизирует алерты — Prometheus или Alertmanager? | [3.2](#32-компоненты) |
| `[ ]` | Counter, gauge, histogram, summary — когда что? | [4](#4-типы-метрик) |
| `[ ]` | Почему нельзя усреднять перцентили? | [4.2](#42-histogram-или-summary) |
| `[ ]` | Почему `rate`, а не `delta`, для счётчика? | [5.1](#51-rate-increase-irate) |
| `[ ]` | Что такое кардинальность и чем она опасна? | [6](#6-кардинальность) |
| `[ ]` | RED и USE — что это? | [7.1](#71-red-use-четыре-золотых-сигнала) |
| `[ ]` | SLI, SLO, SLA, error budget? | [7.2](#72-sli-slo-error-budget) |
| `[ ]` | Зачем `for` в правиле алерта? Что такое burn rate? | [8](#8-алертинг) |
| `[ ]` | Почему Loki дешевле Elasticsearch и чем за это платит? | [9.2](#92-loki) |
| `[ ]` | Что такое span, trace id, context propagation? | [10.1](#101-устройство-трейса) |
| `[ ]` | Head-based и tail-based sampling? | [10.3](#103-сэмплирование) |
| `[ ]` | Что случилось с Promtail? | [11](#11-сбор-телеметрии-alloy-и-otel-collector) |
| `[ ]` | Почему `orders.created` в Prometheus превращается в `orders_total`? | [13.2](#132-кастомные-метрики) |
| `[ ]` | Есть ли у `http_server_requests` бакеты по умолчанию? | [13.1](#131-как-это-работает) |
| `[ ]` | p99 вырос в два раза. Порядок действий? | [14](#14-разбор-инцидента) |

---

## 1. Мониторинг и observability

`[ ]` **Короткий ответ:** мониторинг отвечает на вопросы, которые вы предусмотрели заранее: «жив ли сервис», «не превышена ли доля ошибок». Observability — свойство системы, позволяющее по её внешним сигналам (метрикам, логам, трейсам) ответить на вопрос, который вы не предусмотрели: «почему медленно только у клиентов из одного региона на одной версии приложения». Мониторинг — часть observability, а не противоположность.

| | Мониторинг | Observability |
|---|---|---|
| Вопрос | Работает ли система? | Почему она ведёт себя именно так? |
| Данные | Заранее выбранные метрики и пороги | Сигналы с богатым контекстом, связанные между собой |
| Подход | Алерт сработал — смотрим дашборд | Исследование: новый вопрос без нового кода |
| Ключевое требование | Правильные пороги | Корреляция сигналов: trace id в логах, exemplars в метриках |

---

## 2. Сигналы телеметрии

`[ ]` **Короткий ответ:** основные сигналы — метрики, логи и трейсы; часто добавляют события (деплой, смена конфигурации) и профили. Метрики дешёвые и агрегируемые — для алертов и трендов, но теряют детали. Логи детальны — что произошло в конкретной точке. Трейс — путь одного запроса через все сервисы с длительностью каждого шага, показывает, где задержка. Сила — в связке: алерт по метрике → exemplar ведёт к трейсу → trace id ведёт к логам.

| Сигнал | Что это | Примеры | Сильная сторона | Слабая сторона |
|---|---|---|---|---|
| Метрики | Числа во времени с лейблами | RPS, latency, доля ошибок, CPU | Дёшево, агрегация, алертинг | Нет деталей одного запроса, кардинальность |
| События | Дискретные факты | Деплой, feature flag, failover | Контекст «что изменилось» | Нужно сопоставлять с остальным |
| Логи | Записи о событиях в коде | Ошибка со stack trace, аудит | Детали, произвольный текст | Дорого хранить и искать при больших объёмах |
| Трейсы | Дерево span'ов одного запроса | Запрос → API → orders → БД | Где именно задержка в распределённой системе | Сэмплирование, нужна инструментация |
| Профили | Где тратится CPU и память | Flame graph | Почему медленно внутри процесса | Накладные расходы, специальные инструменты |

**Уточняющие вопросы:**

- Лог или трейс? Лог — «что произошло в этой точке». Трейс — «как запрос прошёл через систему и где задержался». Лог с trace id превращается в часть трейса.
- Что такое MELT? Metrics, Events, Logs, Traces — мнемоника для того же набора.

---

## 3. Prometheus: модель сбора

### 3.1. Pull, а не push

`[ ]` **Короткий ответ:** Prometheus сам опрашивает (scrape) цели по HTTP с фиксированным интервалом, обычно 15–30 секунд. Он знает полный список целей, поэтому недоступная цель видна явно — `up == 0`, а не тишина. Любую цель можно проверить вручную `curl`-ом. Ограничения: цель должна быть достижима по сети, а короткие batch-задачи могут завершиться между опросами — для них есть Pushgateway или отправка через OTLP.

**Практика.** Prometheus 3.15.1 опрашивает себя и несуществующий сервис `orders` на порту 9999 (реальный вывод API):

```text
query up:
prometheus 1
orders 0

/api/v1/targets:
orders down Get "http://localhost:9999/metrics": dial tcp 127.0.0.1:9999: connect: connection refused
prometheus up
```

Отсюда базовый алерт: `up == 0` дольше нескольких минут.

<details>
<summary>Pushgateway и remote write — что спросят</summary>

- Pushgateway хранит последнее значение, которое прислала задача, и отдаёт его Prometheus. Он не агрегирует и не удаляет метрики сам: упавшая задача будет «висеть» с последним успешным значением. Поэтому используйте его только для задач уровня сервиса (ночной batch), а не для обычных процессов.
- Remote write — Prometheus отправляет данные в долговременное хранилище (Thanos, Mimir, VictoriaMetrics, облачные сервисы). Так решают горизонтальное масштабирование и долгий retention: сам Prometheus — один узел с локальной TSDB.
- В Prometheus 3.x есть приём OTLP: приложения могут отправлять метрики по протоколу OpenTelemetry, то есть push.

</details>

### 3.2. Компоненты

| Компонент | Роль |
|---|---|
| Prometheus server | Scrape, хранение в TSDB, PromQL, вычисление recording и alerting rules |
| Exporters | Отдают `/metrics` за систему, которая сама не умеет: node_exporter, blackbox_exporter, postgres_exporter, kube-state-metrics |
| Client libraries, Micrometer | Приложение само отдаёт метрики |
| Service discovery | Находит цели автоматически: Kubernetes, Consul, облачные API |
| Alertmanager | Отдельный процесс: группировка, дедупликация, подавление (inhibition), тишина (silence), маршрутизация в Slack, PagerDuty, почту |
| Pushgateway | Метрики коротких задач |

`[ ]` **Частая ошибка:** приписывать маршрутизацию алертов Prometheus. Prometheus только вычисляет правила и отправляет сработавшие алерты в Alertmanager; кого и как оповестить, решает Alertmanager.

---

## 4. Типы метрик

### 4.1. Четыре типа

`[ ]` **Короткий ответ:**

| Тип | Что представляет | Пример | Как читать |
|---|---|---|---|
| Counter | Только растёт, сбрасывается при рестарте | `http_requests_total` | Всегда через `rate()` или `increase()`, сырое значение бессмысленно |
| Gauge | Растёт и падает | `queue_size`, `jvm_memory_used_bytes` | Как есть, `avg_over_time`, `max_over_time` |
| Histogram | Счётчики попаданий в бакеты плюс `_sum` и `_count` | `http_request_duration_seconds_bucket{le="0.1"}` | `histogram_quantile` по `rate` бакетов |
| Summary | Квантили, посчитанные на клиенте | `rpc_duration_seconds{quantile="0.99"}` | Как есть, но агрегировать нельзя |

Бакеты гистограммы кумулятивные: `le="0.5"` включает всё, что попало в `le="0.1"`. Среднее — `rate(_sum) / rate(_count)`.

### 4.2. Histogram или summary

`[ ]` **Короткий ответ:** histogram агрегируется на стороне сервера: бакеты всех инстансов можно сложить и посчитать честный общий перцентиль. Summary считает квантиль внутри процесса, и квантили разных инстансов математически нельзя сложить или усреднить. Плата за histogram — точность ограничена границами бакетов (интерполяция внутри бакета) и больше рядов. Выбор по умолчанию — histogram; в Prometheus 3 есть также native histograms с автоматическими экспоненциальными бакетами.

**Практика.** Инстанс `a` обслуживает 1000 запросов в минуту быстрее 0,1 с, инстанс `b` — 10 запросов в минуту по 0,1–2 с. Проверено `promtool test rules` (файл — в [приложении](#приложение-файлы-проверки)):

```promql
# честный p95: сначала сложить бакеты, потом квантиль
histogram_quantile(0.95, sum by (le) (rate(lat_bucket[5m])))                     # 0.096
# ошибка: усреднить p95 каждого инстанса
avg(histogram_quantile(0.95, sum by (instance, le) (rate(lat_bucket[5m]))))       # 1
```

Среднее перцентилей завысило p95 в десять раз: медленный инстанс с 1 % трафика получил такой же вес, как основной. Ровно эта ошибка неизбежна с summary.

**Уточняющие вопросы:**

- Почему p95 = 0,096, а не меньше 0,1 ровно? Квантиль интерполируется линейно внутри бакета `0..0.1`: точное значение внутри бакета неизвестно. Границы бакетов нужно ставить около SLO (например, 0,3 с), тогда «доля запросов быстрее SLO» считается точно, без интерполяции.
- Почему среднее latency плохо? Оно скрывает хвост: 99 запросов по 10 мс и один по 5 с дают среднее 60 мс, а пользователь с 5 с ушёл.

---

## 5. PromQL: необходимый минимум

### 5.1. rate, increase, irate

`[ ]` **Короткий ответ:** `rate(x[5m])` — средняя скорость роста счётчика в секунду за окно, учитывает сбросы при рестарте. `increase(x[5m])` — то же, умноженное на окно (прирост за 5 минут). `irate` — по двум последним точкам, для графиков быстрых всплесков, не для алертов. `delta` — для gauge; на счётчике со сбросом он даёт мусор. Окно — минимум 4 интервала scrape.

**Практика.** Счётчик растёт на 60 в минуту (1/с), на 5-й минуте процесс перезапускается и счётчик падает в 0 (проверено `promtool`):

```text
values: 0 60 120 180 240 0 60 120 180 240 300

rate(req_total[5m])  @7m  = 0.75     # сброс обработан, но прирост между 240 и рестартом потерян
delta(req_total[5m]) @7m  = -75      # отрицательный «прирост» — бессмыслица для счётчика
```

`rate` показал 0,75 вместо 1: прирост между последним опросом до рестарта и самим рестартом неизвестен. Частые рестарты занижают `rate` — ещё одна причина смотреть на `changes(process_start_time_seconds[1h])`.

### 5.2. Шпаргалка запросов

```promql
# RPS по сервису
sum by (service) (rate(http_requests_total[5m]))

# доля ошибок 5xx
sum by (service) (rate(http_requests_total{status=~"5.."}[5m]))
  / sum by (service) (rate(http_requests_total[5m]))

# p99 latency
histogram_quantile(0.99, sum by (service, le) (rate(http_request_duration_seconds_bucket[5m])))

# средняя latency
rate(http_request_duration_seconds_sum[5m]) / rate(http_request_duration_seconds_count[5m])

# топ-5 инстансов по CPU
topk(5, rate(process_cpu_seconds_total[5m]))

# цель недоступна
up == 0

# заполнение диска через 4 часа по текущему тренду
predict_linear(node_filesystem_avail_bytes[1h], 4 * 3600) < 0
```

<details>
<summary>Recording rules и правило порядка операций</summary>

- Сначала `rate`, потом `sum`. `sum` по сырым счётчикам, а затем `rate`, ломает обработку сбросов: рестарт одного инстанса выглядит как падение общего счётчика.
- `histogram_quantile` — по `sum by (le, ...)`, лейбл `le` обязан остаться.
- Recording rules заранее вычисляют тяжёлые выражения и сохраняют как новый ряд. Соглашение об именах: `level:metric:operations`, например `job:http_requests:rate5m`. Дашборды и алерты читают готовый ряд — быстрее и одинаково.

</details>

---

## 6. Кардинальность

`[ ]` **Короткий ответ:** кардинальность — число уникальных временных рядов. Каждая уникальная комбинация имени метрики и значений лейблов — отдельный ряд в памяти и на диске. Лейбл с неограниченным набором значений — `user_id`, `order_id`, полный URL с идентификаторами, текст ошибки — умножает ряды и роняет Prometheus по памяти. Правило: значения лейбла должны составлять маленькое ограниченное множество. Детали отдельного запроса — в логи и трейсы.

```text
http_requests_total × 20 эндпоинтов × 5 методов × 10 статусов × 30 подов = 30 000 рядов
+ лейбл user_id на 100 000 пользователей                                = 3 000 000 000 рядов
```

**Практика.** Голый Prometheus, опрашивающий только себя, уже держит 716 рядов; эндпоинт статуса TSDB показывает, какие метрики дают больше всего (реальный вывод):

```bash
curl -s localhost:9090/api/v1/status/tsdb
```

```text
headStats.numSeries: 716
{'name': 'prometheus_http_requests_total', 'value': 64}
{'name': 'prometheus_engine_query_duration_histogram_seconds_bucket', 'value': 20}
{'name': 'prometheus_sd_kubernetes_events_total', 'value': 18}
```

Этот эндпоинт (и страница Status → TSDB Status) — первое, куда смотреть при росте памяти Prometheus.

<details>
<summary>Как бороться с кардинальностью</summary>

- Шаблон пути вместо фактического: `/orders/{id}`, а не `/orders/123`. Micrometer и Spring делают это для `uri` автоматически, если не строить URL вручную.
- `metric_relabel_configs` с `action: labeldrop` или `drop` — удалить лейбл или ряд при приёме.
- `sample_limit` и `label_limit` в scrape config — защита от взрыва одного сервиса.
- Меньше бакетов гистограммы: каждый бакет — отдельный ряд на каждую комбинацию лейблов.
- Высокая кардинальность нужна? Это задача трейсов и логов, где индексируются немногие поля.

</details>

---

## 7. Что измерять: RED, USE, SLO

### 7.1. RED, USE, четыре золотых сигнала

`[ ]` **Короткий ответ:** для сервисов — RED: Rate (запросы в секунду), Errors (доля ошибок), Duration (распределение latency). Для ресурсов — USE: Utilization (занятость), Saturation (очередь ожидающих), Errors. Четыре золотых сигнала из Google SRE — latency, traffic, errors, saturation — объединяют оба подхода.

| Метод | Для чего | Пример для сервиса заказов |
|---|---|---|
| RED | Сервисы, эндпоинты | RPS `POST /orders`, доля 5xx, p99 |
| USE | CPU, память, диск, пул соединений, очередь | Активные соединения пула / максимум; потоки, ждущие соединения; таймауты |

Saturation чаще всего предсказывает проблему раньше остальных: пул соединений HikariCP заполнен, и запросы ждут — latency вырастет следом.

### 7.2. SLI, SLO, error budget

`[ ]` **Короткий ответ:** SLI — измеряемый показатель, важный пользователю: доля успешных запросов, доля запросов быстрее 300 мс. SLO — цель для SLI на периоде: «99,9 % успешных за 30 дней». SLA — договор с последствиями для бизнеса, обычно мягче SLO. Error budget — допустимая доля неудач: 0,1 % = около 43 минут полной недоступности в месяц. Пока бюджет есть — выкатываем фичи; кончился — приоритет надёжности.

```promql
# SLI доступности за 30 дней
sum(rate(http_requests_total{status!~"5.."}[30d])) / sum(rate(http_requests_total[30d]))

# SLI latency: доля запросов быстрее 0.3 с (граница бакета совпадает с SLO)
sum(rate(http_request_duration_seconds_bucket{le="0.3"}[5m]))
  / sum(rate(http_request_duration_seconds_count[5m]))
```

---

## 8. Алертинг

`[ ]` **Короткий ответ:** алерт должен будить человека только тогда, когда страдают пользователи и нужно действие. Алертим по симптомам (доля ошибок, latency, SLO), а не по причинам (CPU 90 % сам по себе не проблема). `for: 5m` требует, чтобы условие держалось 5 минут, — отсекает кратковременные всплески. Лучшая практика — алерт по скорости сжигания error budget (burn rate) в двух окнах: быстрое сжигание — будить сразу, медленное — тикет.

**Практика.** Recording rules и алерт с юнит-тестом (проверено `promtool test rules`, Prometheus 3.15.1):

```yaml
groups:
  - name: orders-api
    rules:
      - record: job:http_requests:rate5m
        expr: sum by (job) (rate(http_requests_total[5m]))
      - record: job:http_errors:ratio5m
        expr: |
          sum by (job) (rate(http_requests_total{status=~"5.."}[5m]))
          /
          sum by (job) (rate(http_requests_total[5m]))
      - record: job:http_latency:p95_5m
        expr: histogram_quantile(0.95, sum by (job, le) (rate(http_request_duration_seconds_bucket[5m])))
      - alert: HighErrorRate
        expr: job:http_errors:ratio5m > 0.05
        for: 5m
        labels:
          severity: page
        annotations:
          summary: "Доля 5xx выше 5% в {{ $labels.job }}"
```

Тест: 600 запросов в минуту, 10 % — ошибки; 90 % запросов быстрее 0,1 с, 98 % быстрее 0,5 с. Ожидается RPS = 10, доля ошибок = 0,1, p95 = 0,35, на 4-й минуте алерта нет (действует `for`), на 10-й — есть.

```text
$ promtool check rules rules.yml
Checking rules.yml
  SUCCESS: 4 rules found

$ promtool test rules test.yml
  SUCCESS
```

Деталь из проверки: без `round(..., 0.001)` тест p95 падает — `histogram_quantile` вернул `0.34999999999999987`. Сравнивать вещественные результаты на точное равенство нельзя.

<details>
<summary>Burn rate: формула и пороги</summary>

Burn rate — во сколько раз быстрее нормы тратится бюджет. Для SLO 99,9 % бюджет 0,1 %; доля ошибок 1,44 % = burn rate 14,4: при такой скорости месячный бюджет закончится за двое суток, а 2 % бюджета сгорит за час.

```promql
# будить: быстрое сжигание, подтверждённое коротким окном
(
  sum(rate(http_requests_total{status=~"5.."}[1h])) / sum(rate(http_requests_total[1h])) > 14.4 * 0.001
and
  sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m])) > 14.4 * 0.001
)
```

Короткое окно нужно, чтобы алерт быстро погас после исправления. Медленное сжигание (например, burn rate 1–3 за 1–3 дня) — тикет, а не звонок ночью.

</details>

**Уточняющие вопросы:**

- Что делает Alertmanager с сотней одинаковых алертов от ста подов? Группирует по `group_by` (например, `alertname`, `service`) и отправляет одно уведомление.
- Inhibition? Подавление зависимых алертов: если упал весь кластер, не слать алерт по каждому сервису.
- Алерт на отсутствие данных? `absent(up{job="orders"})` — job пропал из service discovery целиком, и `up == 0` уже нечему сработать.

---

## 9. Логи и Loki

### 9.1. Структурированные логи

`[ ]` **Короткий ответ:** логи пишем в stdout в структурированном виде (JSON), с полями: время, уровень, сервис, сообщение, `trace_id`, `span_id`, ключевые бизнес-идентификаторы. Тогда их можно фильтровать по полям и связать с трейсом. Не логируем секреты и персональные данные, не логируем в цикле на каждый запрос уровнем INFO при большой нагрузке.

```json
{"ts":"2026-10-26T10:15:02.114Z","level":"ERROR","service":"orders","trace_id":"4bf92f3577b34da6a3ce929d0e0e4736",
 "span_id":"00f067aa0ba902b7","order_id":"o-1842","msg":"payment declined","error":"PaymentTimeout"}
```

### 9.2. Loki

`[ ]` **Короткий ответ:** Loki индексирует только лейблы потока (`service`, `namespace`, `level`), а сам текст хранит сжатыми кусками в объектном хранилище. Запрос сначала отбирает потоки по лейблам, затем просматривает их содержимое. Плюсы — дешёвое хранение и быстрая запись. Минусы — полнотекстовый поиск по большому объёму медленнее, чем в Elasticsearch, и всё решает выбор лейблов. Лейблы Loki подчиняются тем же правилам кардинальности, что и в Prometheus: `trace_id` или `user_id` — не лейбл, а поле внутри строки.

| | Loki | Elasticsearch / OpenSearch |
|---|---|---|
| Индекс | Только лейблы | Полнотекстовый по полям |
| Стоимость хранения | Низкая, объектное хранилище | Высокая: индекс сравним с данными |
| Произвольный поиск по тексту | Перебор отобранных потоков | Быстрый |
| Аналитика по полям | Ограничена | Сильная |

```logql
# ошибки сервиса orders, где в JSON поле error = PaymentTimeout
{service="orders", level="error"} | json | error = "PaymentTimeout"

# число ошибок в минуту по сервисам — метрика из логов
sum by (service) (count_over_time({level="error"}[1m]))

# все строки одного запроса
{namespace="prod"} |= "4bf92f3577b34da6a3ce929d0e0e4736"
```

---

## 10. Трейсы, OpenTelemetry и Tempo

### 10.1. Устройство трейса

`[ ]` **Короткий ответ:** трейс — дерево span'ов одного запроса. Span — одна операция (входящий HTTP-запрос, SQL, вызов другого сервиса) с началом, длительностью, атрибутами, статусом и ссылкой на родителя. Все span'ы трейса разделяют trace id. Context propagation — передача trace id и id родительского span'а между сервисами в заголовке; стандарт — W3C Trace Context, заголовок `traceparent`. Если хоть один сервис в цепочке не передаёт заголовок, трейс рвётся на два.

```text
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
             версия-trace id (16 байт)-------------parent span id--флаги (01 = sampled)

POST /checkout  ───────────────────────────────── 820 мс   gateway
  POST /orders  ──────────────────────────────── 790 мс    orders
    SELECT orders  ─ 12 мс
    POST /payments ─────────────────────── 740 мс          payments
      HTTP bank-api ────────────────────── 730 мс          ← здесь
```

<details>
<summary>Асинхронные границы и OpenTelemetry</summary>

- Через Kafka контекст передаётся в заголовках сообщения; consumer создаёт span, связанный с producer'ом. Обычно это делает инструментация автоматически.
- Внутри процесса контекст живёт в ThreadLocal; при переходе в другой пул потоков его нужно переносить (обёртки executor'ов, Micrometer Context Propagation). Иначе span'ы асинхронной части окажутся в отдельном трейсе.
- OpenTelemetry — открытый стандарт и набор SDK для всех трёх сигналов плюс протокол OTLP. Инструментируем один раз через OTel или Micrometer Tracing с OTel-мостом, бэкенд можно менять: Tempo, Jaeger, облачные сервисы.
- Java-агент OpenTelemetry инструментирует популярные библиотеки (HTTP-клиенты, JDBC, Kafka) без изменения кода.

</details>

### 10.2. Tempo

`[ ]` **Короткий ответ:** Tempo хранит трейсы в объектном хранилище (S3, GCS, MinIO) в колоночном формате Parquet и не строит тяжёлый индекс по всем span'ам. Поэтому хранение дешёвое. Поиск — по trace id или языком TraceQL, который просматривает колонки атрибутов ([TraceQL в Tempo 2.1](https://grafana.com/docs/tempo/latest/release-notes/version-2/v2-1/)). Все виды поиска требуют блоков формата vParquet, старый формат v2 удалён в Tempo 3.0 ([поиск в Tempo](https://grafana.com/docs/tempo/latest/operations/backend_search/)).

```traceql
# трейсы, где сервис payments отвечал дольше 500 мс с ошибкой
{ resource.service.name = "payments" && duration > 500ms && status = error }
```

### 10.3. Сэмплирование

`[ ]` **Короткий ответ:** хранить все трейсы дорого, поэтому сэмплируют. Head-based — решение в начале запроса (например, 10 %), передаётся дальше флагом в `traceparent`; просто и дёшево, но редкие ошибки и медленные запросы теряются в 90 % случаев. Tail-based — решение после завершения трейса в коллекторе: сохранить все ошибки и медленные, а успешные — 1 %; точнее, но коллектор должен держать все span'ы трейса в памяти до решения, и все span'ы одного трейса должны прийти на один экземпляр коллектора.

---

## 11. Сбор телеметрии: Alloy и OTel Collector

`[ ]` **Короткий ответ:** между приложениями и хранилищами ставят агент или коллектор: он принимает метрики, логи и трейсы, обогащает их (лейблы Kubernetes), фильтрует, сэмплирует и отправляет в бэкенды. Grafana Alloy — сборщик экосистемы Grafana, совместимый с OpenTelemetry Collector; он заменил Grafana Agent и Promtail. Promtail достиг конца жизни 2 марта 2026 года, развитие идёт только в Alloy ([документация Loki о Promtail](https://grafana.com/docs/loki/latest/send-data/promtail/)), есть официальное руководство по миграции ([миграция с Promtail на Alloy](https://grafana.com/docs/alloy/latest/set-up/migrate/from-promtail/)). Язык конфигурации River перенесён в Alloy и теперь называется «Grafana Alloy configuration syntax» ([репозиторий River](https://github.com/grafana/river/blob/main/README.md)).

```alloy
// логи подов Kubernetes → Loki
discovery.kubernetes "pods" { role = "pod" }

loki.source.kubernetes "pods" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint { url = "http://loki:3100/loki/api/v1/push" }
}

// приём OTLP от приложений → трейсы в Tempo
otelcol.receiver.otlp "default" {
  grpc {}
  http {}
  output { traces = [otelcol.exporter.otlp.tempo.input] }
}

otelcol.exporter.otlp "tempo" {
  client { endpoint = "tempo:4317" }
}
```

Пример не запускался; имена компонентов — по документации Alloy.

| Где ставить | Плюсы | Минусы |
|---|---|---|
| DaemonSet (агент на узле) | Логи с узла, мало сетевых переходов | Tail sampling сложно: span'ы трейса на разных узлах |
| Gateway (отдельный пул коллекторов) | Tail sampling, централизованные политики | Ещё один компонент, нужна балансировка по trace id |
| Часто — оба уровня | Агент собирает, gateway обрабатывает | Сложнее |

---

## 12. Grafana

`[ ]` **Короткий ответ:** Grafana — слой визуализации и единая точка запросов к разным источникам: Prometheus, Loki, Tempo, SQL. Сама она телеметрию не хранит. Дашборды — JSON, их хранят в Git и раскатывают через provisioning или Terraform. Аннотации накладывают события (деплой) на графики. Главная ценность для расследования — переходы между сигналами: из точки на графике latency по exemplar к трейсу, из span'а — к логам с тем же trace id.

<details>
<summary>Каким должен быть хороший дашборд сервиса</summary>

- Сверху — RED по сервису и статус SLO с остатком error budget.
- Ниже — зависимости: latency и ошибки исходящих вызовов, пул соединений БД, lag consumer'а Kafka.
- Ниже — ресурсы: CPU, память, GC, потоки, рестарты подов.
- Переменные (`$service`, `$namespace`) вместо копий дашборда на каждый сервис.
- Аннотации деплоев: первый вопрос при инциденте — «что изменилось».

</details>

---

## 13. Java: Micrometer и Actuator

### 13.1. Как это работает

`[ ]` **Короткий ответ:** Micrometer — фасад метрик, аналог SLF4J для логов: код пишет в `MeterRegistry`, а формат определяет подключённый реестр (Prometheus, OTLP и другие). Spring Boot Actuator с зависимостью `micrometer-registry-prometheus` публикует `/actuator/prometheus`, если эндпоинт открыт в `management.endpoints.web.exposure.include`. Автоматически собираются JVM (память, GC, потоки), HTTP-сервер и клиенты, пулы соединений, executors, кеши.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true      # без этого бакетов нет: только count, sum, max
    tags:
      application: orders
```

`[ ]` **Важно:** по умолчанию таймер `http.server.requests` публикует только `_count`, `_sum` и `_max` — посчитать p99 через `histogram_quantile` нельзя, пока не включены бакеты. Это частый сюрприз при первом дашборде.

### 13.2. Кастомные метрики

**Практика.** Счётчик, таймер с границами SLO и gauge на Micrometer 1.17.2 (реальный вывод `scrape()`):

```java
PrometheusMeterRegistry reg = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

Counter ok = Counter.builder("orders.created").tag("status", "ok").register(reg);
ok.increment(3);

Timer t = Timer.builder("http.client.latency").tag("target", "payments")
    .serviceLevelObjectives(Duration.ofMillis(100), Duration.ofMillis(500))
    .register(reg);
for (int ms : new int[]{40, 80, 120, 300, 900}) t.record(Duration.ofMillis(ms));

AtomicInteger queue = reg.gauge("queue.size", new AtomicInteger(7));
System.out.print(reg.scrape());
```

```text
# TYPE http_client_latency_seconds histogram
http_client_latency_seconds_bucket{target="payments",le="0.1"} 2
http_client_latency_seconds_bucket{target="payments",le="0.5"} 4
http_client_latency_seconds_bucket{target="payments",le="+Inf"} 5
http_client_latency_seconds_count{target="payments"} 5
http_client_latency_seconds_sum{target="payments"} 1.44
# TYPE http_client_latency_seconds_max gauge
http_client_latency_seconds_max{target="payments"} 0.9
# TYPE orders_total counter
orders_total{status="ok"} 3.0
# TYPE queue_size gauge
queue_size 7.0
```

Что видно из вывода:

- Точки в именах стали подчёркиваниями, к таймеру добавлена единица `_seconds`, к счётчику — `_total`.
- Счётчик `orders.created` превратился в `orders_total`: суффикс `_created` в формате OpenMetrics зарезервирован под время создания ряда, и реестр его отбросил. Называйте счётчик `orders.placed` или `orders` с тегом события.
- `serviceLevelObjectives` дал бакеты ровно на 100 и 500 мс — доля запросов быстрее SLO считается точно.
- `_max` — gauge максимума за последнее окно, его нельзя агрегировать как гистограмму.
- `reg.gauge` хранит слабую ссылку на объект: если `AtomicInteger` не сохранить в поле, его соберёт GC, и gauge начнёт отдавать `NaN`.

<details>
<summary>Trace id в логах и Observation API</summary>

- Micrometer Tracing кладёт `traceId` и `spanId` в MDC, и их можно вывести в шаблоне лога. С structured logging в Spring Boot поля попадают в JSON автоматически.
- `Observation` API — одна инструментация, из которой получаются и метрика (таймер), и span:

```java
Observation.createNotStarted("order.process", observationRegistry)
    .lowCardinalityKeyValue("channel", "web")       // в метрику и span
    .highCardinalityKeyValue("order.id", orderId)   // только в span
    .observe(() -> process(orderId));
```

Разделение low/high cardinality — то же правило из раздела про кардинальность: `order.id` в метрику не попадёт.

</details>

---

## 14. Разбор инцидента

`[ ]` **Короткий ответ:** алерт: p99 `POST /orders` вырос с 200 мс до 2 с. Порядок: (1) масштаб — все инстансы или один, все эндпоинты или один, все клиенты или часть; (2) что изменилось — деплой, конфиг, трафик, зависимость; (3) RED зависимостей — какой исходящий вызов вырос; (4) насыщение — пул соединений, потоки, GC, CPU throttling; (5) трейсы медленных запросов из exemplars — какой span занимает время; (6) логи по trace id. Сначала остановить ущерб (откат, переключение), потом искать корень.

| Наблюдение | Гипотеза | Чем проверить |
|---|---|---|
| Медленный только один под | Шумный сосед, GC, плохой узел | `topk` по инстансам, GC pause, CPU throttling |
| Совпало с деплоем | Регрессия кода | Аннотации, сравнение версий, откат |
| Растёт `hikaricp_connections_pending` | Пул исчерпан: медленные запросы БД или утечка соединений | Длительность SQL в трейсах, `pg_stat_activity` |
| Вырос span внешнего API | Деградация зависимости | Latency исходящих вызовов, таймауты, circuit breaker |
| Растёт lag consumer'а Kafka | Обработка не успевает | Время обработки, rebalance, партиции |
| p99 растёт, p50 нет | Хвост: ретраи, GC, блокировки | Распределение по бакетам, трейсы самых медленных |

---

## 15. Пробное собеседование

<details>
<summary>1. Как инструментировать новый сервис с нуля?</summary>

Метрики RED на входящих запросах и исходящих вызовах (гистограммы с бакетами вокруг SLO), USE для пулов и очередей, JVM-метрики. Структурированные логи в stdout с trace id. Трейсы через OpenTelemetry или Micrometer Tracing с передачей контекста через HTTP и Kafka. Коллектор (Alloy, OTel Collector) отправляет в Prometheus/Mimir, Loki, Tempo. Дашборд RED + SLO, алерты по burn rate, аннотации деплоев.

</details>

<details>
<summary>2. Prometheus съедает всю память после релиза. Что случилось?</summary>

Почти наверняка взрыв кардинальности: в релизе появился лейбл с неограниченными значениями — id, полный URL, текст исключения. Смотрим `/api/v1/status/tsdb` — топ метрик и лейблов по числу рядов. Чиним в коде (шаблон пути, убрать лейбл), а на время — `metric_relabel_configs` с `labeldrop` и `sample_limit` на job.

</details>

<details>
<summary>3. Как посчитать p99 по десяти подам?</summary>

Сложить `rate` бакетов гистограммы по всем подам с сохранением `le` и только потом `histogram_quantile(0.99, ...)`. Усреднять p99 подов нельзя — в проверке выше это завысило перцентиль в десять раз. С summary общий p99 не посчитать вовсе.

</details>

<details>
<summary>4. Почему алерт «CPU > 80 %» плохой?</summary>

Это причина, а не симптом: при 80 % CPU пользователи могут не страдать, а при 30 % — страдать из-за блокировок. Он будит без нужды и пропускает реальные проблемы. Лучше алертить по SLO (ошибки, latency) и burn rate; CPU и насыщение — на дашборд и в тикеты, особенно прогноз (`predict_linear`) для диска.

</details>

<details>
<summary>5. В трейсе есть gateway и orders, но нет payments, хотя вызов был. Почему?</summary>

Разрыв контекста: payments не принимает или orders не передаёт `traceparent` (самописный HTTP-клиент без инструментации, асинхронный вызов в другом пуле потоков без переноса контекста, отправка через Kafka без заголовков). Ещё вариант — разное сэмплирование: payments не уважает флаг sampled от родителя.

</details>

<details>
<summary>6. Loki тормозит на запросе по order_id. Что делать — сделать order_id лейблом?</summary>

Нет: это взорвёт число потоков. Сузить запрос лейблами (`service`, `namespace`, `level`) и временем, затем фильтр `|= "o-1842"` или `| json | order_id = "o-1842"`. Если нужен частый поиск по id — переход от id к trace id через трейсы или structured metadata, а не лейбл.

</details>

---

## 16. Источники

- [Promtail agent — документация Loki](https://grafana.com/docs/loki/latest/send-data/promtail/) — конец жизни Promtail 2 марта 2026.
- [Миграция с Promtail на Alloy](https://grafana.com/docs/alloy/latest/set-up/migrate/from-promtail/).
- [Grafana River](https://github.com/grafana/river/blob/main/README.md) — переименование в синтаксис конфигурации Alloy.
- [Tempo: backend search](https://grafana.com/docs/tempo/latest/operations/backend_search/) и [Tempo 2.1 release notes](https://grafana.com/docs/tempo/latest/release-notes/version-2/v2-1/) — TraceQL и форматы блоков.
- [Grafana Alloy как OpenTelemetry Collector](https://grafana.com/docs/opentelemetry/collector/grafana-alloy/).
- Prometheus 3.15.1 — версия, на которой выполнены проверки ([последний релиз Prometheus](https://api.github.com/repos/prometheus/prometheus/releases/latest)).

---

## Приложение: файлы проверки

<details>
<summary>test.yml — юнит-тест recording rules и алерта</summary>

```yaml
rule_files: [rules.yml]
evaluation_interval: 1m
tests:
  - interval: 1m
    input_series:
      # 600 запросов в минуту, из них 60 с кодом 500 (10%)
      - series: 'http_requests_total{job="orders",status="200"}'
        values: '0+540x20'
      - series: 'http_requests_total{job="orders",status="500"}'
        values: '0+60x20'
      # каждую минуту 100 запросов, 90 быстрее 0.1 с, 98 быстрее 0.5 с
      - series: 'http_request_duration_seconds_bucket{job="orders",le="0.1"}'
        values: '0+90x20'
      - series: 'http_request_duration_seconds_bucket{job="orders",le="0.5"}'
        values: '0+98x20'
      - series: 'http_request_duration_seconds_bucket{job="orders",le="+Inf"}'
        values: '0+100x20'
    promql_expr_test:
      - expr: job:http_requests:rate5m
        eval_time: 10m
        exp_samples:
          - labels: 'job:http_requests:rate5m{job="orders"}'
            value: 10
      - expr: job:http_errors:ratio5m
        eval_time: 10m
        exp_samples:
          - labels: 'job:http_errors:ratio5m{job="orders"}'
            value: 0.1
      - expr: round(job:http_latency:p95_5m, 0.001)
        eval_time: 10m
        exp_samples:
          - labels: '{job="orders"}'
            value: 0.35
    alert_rule_test:
      - alertname: HighErrorRate
        eval_time: 4m
        exp_alerts: []
      - alertname: HighErrorRate
        eval_time: 10m
        exp_alerts:
          - exp_labels: {severity: page, job: orders}
            exp_annotations:
              summary: "Доля 5xx выше 5% в orders"
```

</details>

<details>
<summary>test2.yml — агрегация перцентилей и сброс счётчика</summary>

```yaml
evaluation_interval: 1m
tests:
  - interval: 1m
    input_series:
      # инстанс a: 1000 запросов/мин, все быстрее 0.1 с
      - series: 'lat_bucket{instance="a",le="0.1"}'
        values: '0+1000x20'
      - series: 'lat_bucket{instance="a",le="2"}'
        values: '0+1000x20'
      - series: 'lat_bucket{instance="a",le="+Inf"}'
        values: '0+1000x20'
      # инстанс b: 10 запросов/мин, все между 0.1 и 2 с
      - series: 'lat_bucket{instance="b",le="0.1"}'
        values: '0+0x20'
      - series: 'lat_bucket{instance="b",le="2"}'
        values: '0+10x20'
      - series: 'lat_bucket{instance="b",le="+Inf"}'
        values: '0+10x20'
      # счётчик со сбросом (рестарт процесса на 5-й минуте)
      - series: 'req_total{instance="a"}'
        values: '0 60 120 180 240 0 60 120 180 240 300'
    promql_expr_test:
      - expr: round(histogram_quantile(0.95, sum by (le) (rate(lat_bucket[5m]))), 0.001)
        eval_time: 10m
        exp_samples:
          - labels: '{}'
            value: 0.096
      - expr: round(avg(histogram_quantile(0.95, sum by (instance, le) (rate(lat_bucket[5m])))), 0.001)
        eval_time: 10m
        exp_samples:
          - labels: '{}'
            value: 1
      - expr: rate(req_total[5m])
        eval_time: 7m
        exp_samples:
          - labels: '{instance="a"}'
            value: 0.75
      - expr: delta(req_total[5m])
        eval_time: 7m
        exp_samples:
          - labels: '{instance="a"}'
            value: -75
```

```text
$ promtool test rules test2.yml
  SUCCESS
```

</details>
