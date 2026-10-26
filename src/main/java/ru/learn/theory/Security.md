# Безопасность: TLS и mutual TLS (mTLS)

> Конспект-тренажёр для подготовки к собеседованиям.
> Примеры проверены на **OpenSSL 3.5.5**, **curl** и Python 3 (`ssl`, TLS 1.3): собственный CA, сервер с обязательным клиентским сертификатом, семь сценариев отказа и успеха, `openssl verify`. Вывод в разделах реальный. Настройки Java и Spring Boot, service mesh не запускались — они приведены по документации.

## Как пользоваться файлом

Каждая тема устроена одинаково:

1. **Короткий ответ** — то, что нужно сказать устно за 30–60 секунд.
2. **Подробности** — под спойлером `<details>`: механизм, ограничения, примеры.
3. **Уточняющие вопросы** — то, что интервьюер спросит следом.
4. **Практика** — команда, код или сценарий отказа.

Отмечайте уверенность прямо в файле:

- `[ ]` — не знаю;
- `[~]` — отвечаю с подсказкой;
- `[x]` — объясняю сам, с примером.

Повторяйте прежде всего то, что отмечено `[ ]` и `[~]`.

---

## Содержание

0. [Быстрое повторение](#0-быстрое-повторение)
1. [Что даёт TLS](#1-что-даёт-tls)
2. [Сертификаты и цепочка доверия](#2-сертификаты-и-цепочка-доверия)
3. [Рукопожатие TLS 1.3](#3-рукопожатие-tls-13)
4. [Как работает mTLS](#4-как-работает-mtls)
5. [Аутентификация не равна авторизации](#5-аутентификация-не-равна-авторизации)
6. [Где завершается TLS](#6-где-завершается-tls)
7. [Ротация, отзыв, срок жизни](#7-ротация-отзыв-срок-жизни)
8. [mTLS и OAuth/JWT](#8-mtls-и-oauthjwt)
9. [mTLS в Java и Spring Boot](#9-mtls-в-java-и-spring-boot)
10. [Диагностика ошибок рукопожатия](#10-диагностика-ошибок-рукопожатия)
11. [Пробное собеседование](#11-пробное-собеседование)
12. [Источники](#12-источники)
- [Приложение: скрипты лаборатории](#приложение-скрипты-лаборатории)

---

## 0. Быстрое повторение

Ответьте вслух, не открывая разделы. Ответы — по ссылкам.

| Уверенность | Вопрос | Раздел |
|---|---|---|
| `[ ]` | От чего защищает TLS и от чего нет? | [1](#1-что-даёт-tls) |
| `[ ]` | Что проверяет клиент в сертификате сервера? | [2](#2-сертификаты-и-цепочка-доверия) |
| `[ ]` | Зачем SAN, если есть CN? Что такое EKU? | [2](#2-сертификаты-и-цепочка-доверия) |
| `[ ]` | Чем TLS 1.3 отличается от 1.2? Что такое forward secrecy? | [3](#3-рукопожатие-tls-13) |
| `[ ]` | Чем mTLS отличается от обычного HTTPS? | [4](#4-как-работает-mtls) |
| `[ ]` | Что доказывает клиентский сертификат? | [4](#4-как-работает-mtls) |
| `[ ]` | Сертификат валиден — значит, доступ разрешён? | [5](#5-аутентификация-не-равна-авторизации) |
| `[ ]` | Что такое SPIFFE ID? | [5](#5-аутентификация-не-равна-авторизации) |
| `[ ]` | TLS завершается на балансировщике. Как сервис узнаёт клиента? | [6](#6-где-завершается-tls) |
| `[ ]` | Как ротировать сертификаты без простоя? | [7](#7-ротация-отзыв-срок-жизни) |
| `[ ]` | Каким будет максимальный срок публичного TLS-сертификата в 2026 и 2029? | [7](#7-ротация-отзыв-срок-жизни) |
| `[ ]` | mTLS или JWT между сервисами? | [8](#8-mtls-и-oauthjwt) |
| `[ ]` | Keystore и truststore — в чём разница? | [9](#9-mtls-в-java-и-spring-boot) |
| `[ ]` | `unknown ca`, `certificate required`, `unsupported certificate` — что значит? | [10](#10-диагностика-ошибок-рукопожатия) |

---

## 1. Что даёт TLS

`[ ]` **Короткий ответ:** TLS защищает канал: конфиденциальность (трафик зашифрован), целостность (подмену заметят) и аутентификацию сервера (клиент проверил, что говорит с владельцем ключа сертификата на нужное имя). TLS не защищает данные на концах (в логах, БД), не решает, что клиенту разрешено, и в обычном HTTPS не сообщает серверу, кто клиент: личность пользователя устанавливается на прикладном уровне — пароль, сессия, токен.

<details>
<summary>Место TLS в стеке</summary>

TLS работает поверх транспорта (обычно TCP; для QUIC — встроен в сам протокол). Отнесение к L4 или L5 модели OSI — упрощение, зависящее от схемы: на собеседовании достаточно сказать «между TCP и прикладным протоколом». Если TLS завершается на балансировщике или в sidecar, дальше трафик идёт по отдельному соединению, которое требует собственного решения (раздел 6).

</details>

---

## 2. Сертификаты и цепочка доверия

`[ ]` **Короткий ответ:** сертификат X.509 связывает открытый ключ с именем и подписан центром сертификации (CA). Проверяющая сторона строит цепочку от сертификата до корня в своём trust store и проверяет: подписи по цепочке, срок действия, имя (hostname должно совпасть с записью в Subject Alternative Name; поле CN для проверки имени современные клиенты не используют), назначение (Extended Key Usage: `serverAuth` для сервера, `clientAuth` для клиента) и, если настроено, отзыв. Сертификат публичен; секрет — только закрытый ключ.

**Практика.** Свой CA выпускает сертификат сервера с `SAN=DNS:localhost` и клиента с `SAN=URI:spiffe://demo.local/ns/prod/sa/billing` (реальный вывод):

```bash
openssl x509 -in client.crt -noout -subject -issuer -ext subjectAltName,extendedKeyUsage -dates
```

```text
subject=CN=billing-service
issuer=CN=Demo Internal CA
X509v3 Subject Alternative Name:
    URI:spiffe://demo.local/ns/prod/sa/billing
X509v3 Extended Key Usage:
    TLS Web Client Authentication
notBefore=Oct 26 08:04:21 2026 GMT
notAfter=Oct 27 08:04:21 2026 GMT
```

Проверка цепочки, срока и назначения `sslclient` для трёх сертификатов:

```bash
openssl verify -CAfile ca.crt -purpose sslclient expired.crt client.crt server.crt
```

```text
error 10 at 0 depth lookup: certificate has expired
error expired.crt: verification failed
error 26 at 0 depth lookup: unsuitable certificate purpose
error server.crt: verification failed
client.crt: OK
```

Сертификат сервера подписан тем же CA, но не годится как клиентский: у него EKU `serverAuth`.

**Уточняющие вопросы:**

- Что такое промежуточный CA? Корневой ключ хранят офлайн, а сертификаты выпускает промежуточный CA, подписанный корнем. Сервер должен отдавать свой сертификат вместе с промежуточными, иначе часть клиентов не построит цепочку.
- Self-signed сертификат — это плохо? Корневой CA всегда самоподписан. Плохо, когда самоподписанный конечный сертификат принимают, отключив проверку (`-k`, `trustAll`): тогда TLS не защищает от подмены сервера.

---

## 3. Рукопожатие TLS 1.3

`[ ]` **Короткий ответ:** клиент отправляет ClientHello с поддерживаемыми шифрами и сразу свою долю обмена ключами (ECDHE). Сервер отвечает ServerHello со своей долей, после чего всё дальнейшее, включая сертификат сервера, уже зашифровано. Сервер подтверждает владение ключом подписью (CertificateVerify) и завершает Finished. Полное рукопожатие занимает один round-trip против двух в TLS 1.2. Ключи сессии получены эфемерным Диффи — Хеллманом, поэтому кража ключа сервера позже не позволит расшифровать записанный ранее трафик — это forward secrecy, в TLS 1.3 она обязательна.

```text
клиент                                       сервер
ClientHello + key_share  ─────────────────▶
                         ◀─────────────────  ServerHello + key_share
                                             {EncryptedExtensions}
                                             {CertificateRequest}   ← только в mTLS
                                             {Certificate, CertificateVerify, Finished}
{Certificate, CertificateVerify}  ← только в mTLS
{Finished}               ─────────────────▶
HTTP-запрос              ─────────────────▶
```

**Практика.** Параметры соединения лабораторного сервера:

```text
New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384
Protocol: TLSv1.3
Verify return code: 0 (ok)
```

<details>
<summary>Что ещё изменилось в TLS 1.3</summary>

- Удалены статический RSA-обмен ключами (нет forward secrecy), CBC-шифры, RC4, SHA-1 в подписях, компрессия и пересогласование.
- Остались пять AEAD-наборов (AES-GCM, ChaCha20-Poly1305 и AES-CCM).
- 0-RTT (early data) при возобновлении сессии позволяет отправить данные сразу, но они не защищены от повторного воспроизведения: допустимы только идемпотентные запросы.
- Платёжные и облачные провайдеры постепенно требуют TLS 1.2+ и переход на 1.3 ([пример уведомления Cybersource](https://developer.cybersource.com/docs/cybs/en-us/platform/relnote/all/na/rn-2026-04-17/rn-announce/tls-1-3.md)).

</details>

---

## 4. Как работает mTLS

`[ ]` **Короткий ответ:** в mTLS сервер в рукопожатии отправляет CertificateRequest, клиент отвечает своим сертификатом и подписью CertificateVerify, доказывая владение закрытым ключом. Сервер проверяет цепочку до своего доверенного CA, срок и назначение. Если проверка не прошла, рукопожатие обрывается и HTTP-запрос до приложения не доходит. Корректно проверенный клиентский сертификат доказывает одно: собеседник владеет ключом сертификата, выпущенного доверенным CA. Что ему разрешено — отдельный вопрос.

**Практика.** Сервер на Python требует клиентский сертификат (`CERT_REQUIRED`), доверяет только своему CA, а после рукопожатия разрешает доступ только `spiffe://demo.local/ns/prod/sa/billing`. Семь сценариев (реальный вывод curl):

| Сценарий | Результат |
|---|---|
| Без клиентского сертификата | `tlsv13 alert certificate required` |
| Чужой самоподписанный сертификат | `tlsv1 alert unknown ca` |
| Наш CA, истёкший срок | `ssl/tls alert certificate expired` |
| Наш CA, но EKU `serverAuth` вместо `clientAuth` | `ssl/tls alert unsupported certificate` |
| Клиент не доверяет CA сервера | `SSL certificate OpenSSL verify result: self-signed certificate in certificate chain (19)` |
| Обращение по `127.0.0.1`, а в SAN сервера только `localhost` | `no alternative certificate subject name matches target ipv4 address '127.0.0.1'` |
| Наш CA, сервис `reports` | `HTTP 403`, `allowed=False` |
| Наш CA, сервис `billing` | `HTTP 200`, `allowed=True` |

Первые четыре отказа — на стороне сервера, на уровне TLS, до HTTP. Пятый и шестой — клиент сам отказался продолжать, проверяя сервер: mTLS взаимен. Последние два прошли TLS, а решение принято приложением.

---

## 5. Аутентификация не равна авторизации

`[ ]` **Короткий ответ:** mTLS отвечает «кто ты» — и то только «владелец ключа сертификата от нашего CA». Если сервер доверяет внутреннему CA, сертификат от него может получить любой сервис компании, в том числе скомпрометированный. Поэтому после рукопожатия сервер сопоставляет идентичность из сертификата (обычно URI в SAN) с политикой: какие сервисы могут вызывать какие операции. В лаборатории `reports` получил сертификат от того же CA, прошёл TLS и получил 403.

```python
ALLOWED = {"spiffe://demo.local/ns/prod/sa/billing"}   # политика

cert = self.connection.getpeercert()
uris = [v for k, v in cert.get("subjectAltName", ()) if k == "URI"]
ok = any(u in ALLOWED for u in uris)
self.send_response(200 if ok else 403)
```

<details>
<summary>SPIFFE и workload identity</summary>

- SPIFFE ID — URI вида `spiffe://<trust-domain>/<путь>`, например `spiffe://prod.example/ns/payments/sa/api`. Он кладётся в SAN сертификата (SVID), который выдаётся сервису автоматически на короткий срок.
- Идентичность привязана к рабочей нагрузке (namespace, service account), а не к IP: IP в Kubernetes переиспользуются, а сетевые правила не говорят, кто внутри пода.
- Service mesh (Istio, Linkerd) выпускает такие сертификаты, ротирует их и делает mTLS между sidecar'ами прозрачно для кода; авторизацию задают политиками mesh (например, `AuthorizationPolicy` с `principals`).
- Правила по CN хрупки: CN — свободный текст без структуры. Опирайтесь на SAN.

</details>

---

## 6. Где завершается TLS

`[ ]` **Короткий ответ:** если mTLS завершается на балансировщике, ingress или sidecar, приложение видит уже обычное соединение от прокси. Тогда прокси проверяет клиентский сертификат и передаёт идентичность приложению — в заголовке (например, `X-Forwarded-Client-Cert`) или через внутренний mTLS. Главное правило: приложение доверяет такому заголовку только от своего прокси, а прокси обязан удалять одноимённый заголовок, пришедший от клиента. Иначе внешний клиент просто отправит заголовок с чужой идентичностью.

| Вариант | Плюсы | Минусы |
|---|---|---|
| mTLS до приложения | Приложение само видит сертификат | Управление ключами в каждом сервисе |
| Завершение на ingress, дальше открытый трафик | Просто | Внутри кластера трафик не защищён, идентичность — только заголовок |
| Завершение на ingress, дальше mesh mTLS | Защита на каждом hop, ротация автоматически | Сложность mesh |

---

## 7. Ротация, отзыв, срок жизни

`[ ]` **Короткий ответ:** ротация без простоя: новый сертификат выпускают и раскладывают заранее, до истечения старого; при смене CA сначала добавляют новый CA в trust store всех сторон, затем переводят сертификаты, и только потом удаляют старый CA. Долгоживущие соединения продолжают работать со старым сертификатом до переподключения, поэтому сертификат должен перечитываться без рестарта, а соединения — периодически пересоздаваться. Отзыв (CRL, OCSP) зависит от инфраструктуры и не проверяется автоматически: многие клиенты его не делают или не блокируют соединение при недоступности OCSP. Поэтому индустрия идёт к коротким срокам жизни.

**Срок жизни публичных TLS-сертификатов.** CA/Browser Forum в апреле 2025 принял поэтапное сокращение максимального срока с 398 дней ([BusinessWire](https://www.businesswire.com/news/home/20250414207334/en/CABrowser-Forum-Passes-Ballot-to-Reduce-SSLTLS-Certificates-to-47-Day-Maximum-Term), [PwC](https://www.pwc.de/en/cyber-security/tls-certificates-valid-for-47-days.html)):

| С какой даты | Максимальный срок |
|---|---|
| 15 марта 2026 | 200 дней |
| 15 марта 2027 | 100 дней |
| 15 марта 2029 | 47 дней |

Отдельные CA вводят ограничения раньше: DigiCert перешёл на 199 дней с 24 февраля 2026 ([DigiCert](https://knowledge.digicert.com/alerts/public-tls-certificates-199-day-validity)). Вывод для собеседования: ручной выпуск сертификатов больше не работает — нужна автоматизация (ACME, cert-manager, mesh) и мониторинг сроков. Внутренние CA этим правилам не подчиняются, но для сервисных сертификатов типичны сроки от часов до дней (в лаборатории клиентский сертификат выпущен на 1 день).

<details>
<summary>Что мониторить</summary>

- Срок до истечения каждого сертификата (алерт заранее, например за 1/3 срока).
- Ошибки рукопожатия по типу на прокси и серверах: рост `certificate expired` или `unknown ca` после ротации — первый сигнал.
- Работу механизма выпуска (cert-manager, ACME): упавший выпуск обнаруживается только при истечении.
- Хранение закрытых ключей: права на файл, секреты Kubernetes, HSM/KMS для CA; ключ CA — никогда в репозитории.

</details>

---

## 8. mTLS и OAuth/JWT

`[ ]` **Короткий ответ:** mTLS аутентифицирует собеседника на уровне соединения — какой сервис подключился. OAuth 2.0, OIDC и JWT несут прикладную идентичность и права: от имени какого пользователя или клиента выполняется запрос и какие scopes ему выданы. Это разные слои, их комбинируют: mTLS между сервисами плюс токен пользователя, который сервис передаёт дальше или обменивает. Токен можно привязать к сертификату клиента (certificate-bound access tokens в OAuth mTLS): украденный токен без закрытого ключа бесполезен.

| | mTLS | JWT access token |
|---|---|---|
| Что подтверждает | Сервис владеет ключом сертификата | Выдавший сервер авторизации утверждает claims |
| Уровень | Соединение | Запрос |
| Пользовательский контекст | Нет | Да (`sub`, scopes, роли) |
| Кража | Нужен закрытый ключ | Bearer: кто держит, тот и использует (если не привязан) |
| Отзыв | Сложен, короткие сроки | Короткий срок, introspection |
| Через прокси | Завершается на каждом hop | Проходит насквозь в заголовке |

---

## 9. mTLS в Java и Spring Boot

> Пример не запускался.

`[ ]` **Короткий ответ:** keystore содержит свой закрытый ключ и сертификат (чем я представляюсь), truststore — сертификаты CA, которым я доверяю (кого я принимаю). В mTLS они нужны обеим сторонам. В Spring Boot их удобно описывать через SSL bundles и использовать и для сервера, и для клиентов.

```yaml
spring:
  ssl:
    bundle:
      pem:
        service:
          reload-on-update: true            # перечитать при ротации файлов
          keystore:
            certificate: file:/etc/tls/tls.crt
            private-key: file:/etc/tls/tls.key
          truststore:
            certificate: file:/etc/tls/ca.crt
server:
  port: 8443
  ssl:
    bundle: service
    client-auth: need                       # need — обязателен; want — необязателен
```

```java
// клиент с тем же бандлом
RestClient client = RestClient.builder()
    .requestFactory(ClientHttpRequestFactoryBuilder.detect()
        .build(ClientHttpRequestFactorySettings.ofSslBundle(sslBundles.getBundle("service"))))
    .build();
```

- `client-auth: want` пропустит клиентов без сертификата — тогда проверка обязательна в коде.
- В Spring Security идентичность из сертификата извлекается через `http.x509(...)`; по умолчанию используется CN из subject — лучше настроить извлечение SAN.
- Отладка в JVM: `-Djavax.net.debug=ssl:handshake`.

---

## 10. Диагностика ошибок рукопожатия

| Ошибка | Где возникла | Причина |
|---|---|---|
| `alert certificate required` | Сервер | Клиент не отправил сертификат (не настроен keystore, не тот путь) |
| `alert unknown ca` | Сервер (или клиент) | Сертификат собеседника выпущен не доверенным CA, или не отправлен промежуточный |
| `alert certificate expired` | Получатель | Истёк срок, или неверные часы на одной из сторон |
| `alert unsupported certificate`, `unsuitable certificate purpose` | Получатель | Не тот EKU: сертификат сервера вместо клиентского |
| `no alternative certificate subject name matches` | Клиент | Имя в URL не совпадает с SAN сервера |
| `self-signed certificate in certificate chain` | Клиент | Клиент не доверяет CA сервера (нет в truststore) |
| `HTTP 403` после успешного TLS | Приложение | Идентичность не разрешена политикой |

```bash
# что отдаёт сервер и какие CA он принимает для клиентских сертификатов
openssl s_client -connect host:443 -servername host -showcerts </dev/null
openssl s_client -connect host:443 -cert client.crt -key client.key -CAfile ca.crt </dev/null
# проверить цепочку и назначение локально
openssl verify -CAfile ca.crt -untrusted intermediate.crt -purpose sslclient client.crt
```

Если рукопожатие падает, в логах приложения ничего нет — смотреть нужно логи прокси или TLS-уровня.

---

## 11. Пробное собеседование

<details>
<summary>1. Сервисы внутри кластера общаются по HTTP. Как защитить трафик и кто кого может вызывать?</summary>

mTLS между сервисами — проще всего через service mesh, который выдаёт короткоживущие сертификаты с SPIFFE ID и ротирует их автоматически. Поверх — авторизация по идентичности сервиса (политики mesh или проверка SAN в приложении), принцип наименьших привилегий. Пользовательский контекст — JWT, который передаётся дальше. Сетевые политики Kubernetes как дополнительный слой.

</details>

<details>
<summary>2. После ночной ротации CA все вызовы падают с unknown ca. Что пошло не так?</summary>

Сертификаты выпустили новым CA до того, как новый CA попал в truststore всех сторон. Правильный порядок: добавить новый CA в trust store везде (доверие к старому и новому), дождаться раскатки, перевыпустить сертификаты, затем убрать старый CA. Откат — вернуть старые сертификаты или добавить новый CA в truststore.

</details>

<details>
<summary>3. Сервис принимает заголовок X-Client-Id от ingress. В чём риск?</summary>

Если ingress не удаляет такой заголовок из внешних запросов или к сервису можно подключиться в обход ingress, клиент подставит любую идентичность. Нужно: ingress перезаписывает заголовок, сервис принимает трафик только от ingress (сетевые политики или mTLS до ingress), а лучше — mTLS на внутреннем hop.

</details>

<details>
<summary>4. Зачем mTLS, если у нас есть JWT?</summary>

JWT — bearer-токен: кто его перехватил, тот и использует, а сервис без mTLS не знает, какой сервис реально подключился. mTLS шифрует внутренний трафик, аутентифицирует сервисы без общих секретов и позволяет привязать токен к сертификату. JWT несёт пользователя и права. Вместе они закрывают разные угрозы.

</details>

<details>
<summary>5. Клиент говорит, что сертификат валиден, а сервер отвечает unsupported certificate. Почему?</summary>

У сертификата нет `clientAuth` в Extended Key Usage — например, клиенту выдали серверный сертификат. Цепочка и срок при этом в порядке. Проверка: `openssl x509 -noout -ext extendedKeyUsage` и `openssl verify -purpose sslclient`.

</details>

---

## 12. Источники

- [CA/Browser Forum: сокращение срока сертификатов до 47 дней — BusinessWire](https://www.businesswire.com/news/home/20250414207334/en/CABrowser-Forum-Passes-Ballot-to-Reduce-SSLTLS-Certificates-to-47-Day-Maximum-Term).
- [PwC: TLS certificates valid for 47 days](https://www.pwc.de/en/cyber-security/tls-certificates-valid-for-47-days.html).
- [DigiCert: 199-day validity](https://knowledge.digicert.com/alerts/public-tls-certificates-199-day-validity).
- [Cybersource: переход на TLS 1.3](https://developer.cybersource.com/docs/cybs/en-us/platform/relnote/all/na/rn-2026-04-17/rn-announce/tls-1-3.md).
- [Документация curl по SSL-сертификатам](https://curl.se/docs/sslcerts.html).

---

## Приложение: скрипты лаборатории

<details>
<summary>run.sh — CA и сертификаты</summary>

```bash
set -e
# 1. Корневой CA
openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:P-256 -nodes -days 30 \
  -keyout ca.key -out ca.crt -subj "/CN=Demo Internal CA"
# 2. Сертификат сервера (SAN обязателен для проверки имени)
openssl req -newkey ec -pkeyopt ec_paramgen_curve:P-256 -nodes -keyout server.key -out server.csr -subj "/CN=orders-api"
printf "subjectAltName=DNS:localhost\nextendedKeyUsage=serverAuth\n" > server.ext
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -days 7 -extfile server.ext -out server.crt
# 3. Клиент billing (разрешён) и reports (не разрешён)
for svc in billing reports; do
  openssl req -newkey ec -pkeyopt ec_paramgen_curve:P-256 -nodes -keyout $svc.key -out $svc.csr -subj "/CN=$svc-service"
  printf "subjectAltName=URI:spiffe://demo.local/ns/prod/sa/$svc\nextendedKeyUsage=clientAuth\n" > $svc.ext
  openssl x509 -req -in $svc.csr -CA ca.crt -CAkey ca.key -CAcreateserial -days 1 -extfile $svc.ext -out $svc.crt
done
# 4. Истёкший сертификат billing (OpenSSL 3.4+ поддерживает явные даты)
openssl x509 -req -in billing.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -not_before 20260101000000Z -not_after 20260201000000Z -extfile billing.ext -out expired.crt
# 5. Чужой клиент: самоподписанный, не от нашего CA
openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:P-256 -nodes -days 1 -keyout rogue.key -out rogue.crt -subj "/CN=rogue"
```

Скрипт и сервер из приложения перезапущены отдельно в чистом каталоге: billing — `HTTP 200`, reports — `HTTP 403`, истёкший — `certificate expired`. В основной лаборатории сертификат billing назывался `client.crt`.

</details>

<details>
<summary>server.py — сервер с обязательным клиентским сертификатом</summary>

```python
import http.server, ssl

ALLOWED = {"spiffe://demo.local/ns/prod/sa/billing"}  # авторизация после аутентификации

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        cert = self.connection.getpeercert()
        uris = [v for k, v in cert.get("subjectAltName", ()) if k == "URI"]
        ok = any(u in ALLOWED for u in uris)
        self.send_response(200 if ok else 403)
        self.end_headers()
        self.wfile.write(f"client={uris} allowed={ok}\n".encode())
    def log_message(self, *args):
        pass

ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
ctx.minimum_version = ssl.TLSVersion.TLSv1_3
ctx.load_cert_chain("server.crt", "server.key")
ctx.load_verify_locations("ca.crt")
ctx.verify_mode = ssl.CERT_REQUIRED          # без этого получится обычный TLS
srv = http.server.HTTPServer(("localhost", 8443), Handler)
srv.socket = ctx.wrap_socket(srv.socket, server_side=True)
srv.serve_forever()
```

```bash
curl --cacert ca.crt --cert billing.crt --key billing.key -w "HTTP %{http_code}\n" https://localhost:8443/
```

</details>
