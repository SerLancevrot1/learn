package ru.learn.interview;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Локальный worker в роли теста. Breakpoint перед complete показывает границу между БД и Zeebe. */
@EnabledIfEnvironmentVariable(named = "LAB_CAMUNDA_URL", matches = ".+")
class CamundaLabIntegrationTest {
    private static RestClient camunda;
    private static String definitionKey;

    private String orderId;
    private String instanceKey;

    @BeforeAll
    static void deploy() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        camunda = RestClient.builder().baseUrl(System.getenv("LAB_CAMUNDA_URL"))
                .requestFactory(factory).build();

        // У каждого деплоя есть ключ определения. Используем именно его, чтобы тест не зависел
        // от новых версий той же модели, которые могли вручную загрузить в этот стенд.
        var resources = new LinkedMultiValueMap<String, Object>();
        resources.add("resources", new FileSystemResource("docs/labs/camunda/order.bpmn"));
        var deployment = camunda.post().uri("/v2/deployments")
                .contentType(MediaType.MULTIPART_FORM_DATA).body(resources)
                .retrieve().body(JsonNode.class);
        definitionKey = deployment.path("deployments").path(0).path("processDefinition")
                .path("processDefinitionKey").asText();
        assertFalse(definitionKey.isBlank(), () -> "Ключ определения отсутствует в ответе: " + deployment);
    }

    @BeforeEach
    void startOrder() {
        // Модель включает orderId в типы заданий и ключ корреляции. Параллельные запуски
        // и ранее оставленные задания не могут забрать работу или оплату этого заказа.
        orderId = UUID.randomUUID().toString();
        var instance = post("/v2/process-instances", Map.of(
                "processDefinitionKey", definitionKey,
                "variables", Map.of("orderId", orderId)));
        instanceKey = instance.path("processInstanceKey").asText();
        assertFalse(instanceKey.isBlank());
        System.out.println("Camunda lab order " + orderId + " · instance " + instanceKey);
    }

    @Test
    void buffersPaymentReceivedBeforeTheProcessSubscribes() {
        var reserve = activate("reserve", "warehouse", 30_000);

        // Публикуем оплату до complete: подписки ещё нет, потому что процесс на резерве.
        // Положительный TTL даёт Zeebe время сохранить сообщение до открытия подписки.
        publishPayment();
        complete(reserve);

        awaitEnd("OrderPaid");
    }

    @Test
    void routesBusinessRejectionToTheBoundaryEvent() {
        var reserve = activate("reserve", "warehouse", 30_000);

        // OUT_OF_STOCK предусмотрен схемой. Граничное событие прерывает резерв,
        // потом запускает ветку отказа. Здесь важен конечный узел, а не только статус COMPLETED.
        post("/v2/jobs/" + reserve.path("jobKey").asText() + "/error", Map.of(
                "errorCode", "OUT_OF_STOCK", "errorMessage", "No stock for this order"));

        awaitEnd("OrderRejected");
    }

    @Test
    void releasesTheReservationWhenPaymentDoesNotArrive() {
        complete(activate("reserve", "warehouse", 30_000));

        // Оплату не публикуем. Задание на снятие резерва появляется только после таймера.
        // До complete движок ждёт worker, после complete заказ доходит до OrderExpired.
        complete(activate("release", "warehouse", 30_000));

        awaitEnd("OrderExpired");
    }

    @Test
    void retriesATechnicalFailureAndResumesAfterResolvingTheIncident() {
        var reserve = activate("reserve", "warehouse", 30_000);
        assertEquals(3, reserve.path("retries").asInt());
        fail(reserve, 2);
        var retry = activate("reserve", "warehouse", 30_000);
        assertEquals(reserve.path("jobKey"), retry.path("jobKey"));
        assertEquals(2, retry.path("retries").asInt());

        // На первой ошибке оставили два повтора. Теперь имитируем остановку после исчерпания
        // попыток. Incident появляется в проекции асинхронно, поэтому ждём его через поиск.
        fail(retry, 0);
        var incident = await().alias("incident for " + instanceKey).atMost(Duration.ofSeconds(30))
                .until(() -> post("/v2/incidents/search", Map.of("filter", Map.of("processInstanceKey", instanceKey)))
                        .path("items").path(0), JsonNode::isObject);
        assertEquals("JOB_NO_RETRIES", incident.path("errorType").asText());

        // Считаем зависимость восстановленной. Сначала возвращаем попытку, затем снимаем incident.
        // Это те же два шага, которые выполняет оператор при повторе job из Operate.
        camunda.patch().uri("/v2/jobs/{jobKey}", retry.path("jobKey").asText())
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("changeset", Map.of("retries", 1)))
                .retrieve().toBodilessEntity();
        post("/v2/incidents/" + incident.path("incidentKey").asText() + "/resolution", Map.of());

        var recovered = activate("reserve", "warehouse", 30_000);
        assertEquals(retry.path("jobKey"), recovered.path("jobKey"));
        assertEquals(1, recovered.path("retries").asInt());
        publishPayment();
        complete(recovered);
        awaitEnd("OrderPaid");
    }

    @Test
    void repeatsTheJobAfterTimeoutAndKeepsOneReservationInTheDatabase() {
        var database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true).build();
        try {
            var jdbc = new JdbcTemplate(database);
            jdbc.execute("CREATE TABLE reservation (order_id VARCHAR(36) PRIMARY KEY)");

            // Worker выполнил отдельную запись в БД и исчез до команды complete. Первый запуск
            // ограничен коротким таймаутом. Второй worker получит ту же job после его истечения.
            var first = activate("reserve", "first-worker", 4_000);
            insertReservation(jdbc);
            assertEquals(1, reservationCount(jdbc));
            var redelivered = activate("reserve", "second-worker", 30_000);
            assertEquals(first.path("jobKey"), redelivered.path("jobKey"));
            assertEquals(first.path("retries"), redelivered.path("retries"));

            // Уникальность обеспечивает сама БД. Второй вызов подтверждает прежний эффект.
            insertReservation(jdbc);
            assertEquals(1, reservationCount(jdbc));
            publishPayment();
            complete(redelivered);
            awaitEnd("OrderPaid");
        } finally {
            database.shutdown();
        }
    }

    private void insertReservation(JdbcTemplate jdbc) {
        try {
            jdbc.update("INSERT INTO reservation (order_id) VALUES (?)", orderId);
        } catch (DuplicateKeyException duplicate) {
            // В этой таблице один ключ и один эффект, поэтому дубликат означает «резерв уже есть».
            // Если операция принимает сумму или количество, проверяй прежние параметры по тому же ID.
        }
    }

    private int reservationCount(JdbcTemplate jdbc) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE order_id = ?", Integer.class, orderId);
    }

    private JsonNode activate(String operation, String worker, long timeoutMillis) {
        String type = "learn-" + operation + "-" + orderId;
        // Короткие опросы можно поставить на паузу или повторить вручную. timeout здесь задаёт
        // время владения заданием после выдачи, requestTimeout — длительность ожидания выдачи.
        var job = await().alias(type).atMost(Duration.ofSeconds(30)).until(
                () -> post("/v2/jobs/activation", Map.of("type", type, "worker", worker,
                        "maxJobsToActivate", 1, "timeout", timeoutMillis, "requestTimeout", 500))
                        .path("jobs").path(0), JsonNode::isObject);
        assertEquals(instanceKey, job.path("processInstanceKey").asText());
        return job;
    }

    private void complete(JsonNode job) {
        post("/v2/jobs/" + job.path("jobKey").asText() + "/completion", Map.of());
    }

    private void fail(JsonNode job, int retries) {
        // REST принимает остаток попыток. Небольшая пауза перед повтором даёт зависимости
        // время восстановиться. Таймаут выдачи сам по себе остаток попыток не меняет.
        post("/v2/jobs/" + job.path("jobKey").asText() + "/failure", Map.of(
                "retries", retries, "retryBackOff", 300, "errorMessage", "Warehouse temporarily unavailable"));
    }

    private void publishPayment() {
        post("/v2/messages/publication", Map.of("name", "LearnPaymentReceived", "correlationKey", orderId,
                "timeToLive", 30_000, "messageId", orderId + "-payment"));
    }

    private void awaitEnd(String endEvent) {
        // Поиск читает вторичное хранилище. Успешный complete и запись конечного события
        // могут стать видимыми в разное время, поэтому опрашиваем конечный узел с таймаутом.
        await().alias(endEvent + " for " + instanceKey).atMost(Duration.ofSeconds(30)).until(() -> {
            var result = post("/v2/element-instances/search", Map.of("filter", Map.of(
                    "processInstanceKey", instanceKey, "elementId", endEvent, "state", "COMPLETED")));
            return !result.path("items").isEmpty();
        });
    }

    private static JsonNode post(String path, Map<String, ?> body) {
        return camunda.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class);
    }
}
