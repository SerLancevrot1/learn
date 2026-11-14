# OAuth 2.0 и OpenID Connect с Keycloak

[Конспект](../../theory/security/application-security.md) · [План Tech Lead и вопросы](../../interview/tech-lead.md#oauth-20-и-openid-connect) · [Docker для начинающих](../../interview/tech-lead.md#как-запускать-стенды-в-docker) · [Разбор конфигурации](#как-устроены-файлы-стенда)

PowerShell играет роль публичного клиента `lab-client`, браузер — user agent, Keycloak — Authorization Server. Локальный API `lab-api` проверяет один и тот же access token двумя способами. `/api/jwt` проверяет подпись и claims через JWKS, `/api/introspection` запрашивает Keycloak от имени конфиденциального клиента. Для упражнения `email` служит проверяемым scope. Стенд публикуется на loopback и использует учебные учётные данные.

## Получи код и обменяй его на токены

Открой PowerShell 7 в корне репозитория. Все блоки в этом упражнении выполняй в одном окне, чтобы переменные с параметрами авторизации оставались доступны. В браузер переходи только там, где шаг просит войти или скопировать URL.

```powershell
Set-Location .\docs\labs\oauth
docker compose up -d --build
docker compose ps -a
docker compose logs keycloak --tail 20
```

`up -d --build` скачивает готовый образ Keycloak и собирает образ `api` по `Dockerfile`. Затем запускает оба контейнера в фоне. В `ps -a` ищи сервисы `keycloak` и `api`. `logs keycloak --tail 20` покажет последние строки запуска и импорта realm. Для API при ошибке используй `docker compose logs api --tail 20`.

Получим адреса из discovery endpoint. Если Keycloak ещё запускается и соединение отклонено, повтори блок через несколько секунд.

```powershell
$oidc = Invoke-RestMethod 'http://localhost:8180/realms/lab/.well-known/openid-configuration'
$oidc | Select-Object issuer, authorization_endpoint, token_endpoint, jwks_uri, introspection_endpoint
```

`Invoke-RestMethod` делает GET и преобразует ответ JSON в объект `$oidc`. `Select-Object` показывает только выбранные поля. `issuer` должен быть `http://localhost:8180/realms/lab`, а остальные поля содержат адреса входа, обмена кода, ключей и introspection. PowerShell использует адреса из `$oidc` в следующих шагах.

### Сгенерируй параметры и открой форму входа

Через браузер проходят challenge, state и nonce. Verifier нужен только при обмене кода.

```powershell
function ConvertTo-Base64Url([byte[]]$bytes) {
    [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function New-LabVerifier {
    ConvertTo-Base64Url ([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
}

$verifier = New-LabVerifier
$state = New-LabVerifier
$nonce = New-LabVerifier
$challenge = ConvertTo-Base64Url ([Security.Cryptography.SHA256]::HashData([Text.Encoding]::ASCII.GetBytes($verifier)))
$redirect = 'http://localhost:8181/callback'
$authorization = @{
    client_id = 'lab-client'
    redirect_uri = $redirect
    response_type = 'code'
    scope = 'openid email'
    state = $state
    nonce = $nonce
    code_challenge = $challenge
    code_challenge_method = 'S256'
}
$query = ($authorization.GetEnumerator() | ForEach-Object {
    "$($_.Key)=$([uri]::EscapeDataString($_.Value))"
}) -join '&'
Start-Process "$($oidc.authorization_endpoint)?$query"
```

| Строка или блок | Что происходит |
| --- | --- |
| `ConvertTo-Base64Url` | Кодирует массив байтов в Base64 и приводит его к виду для URL. Убирает `=` в конце и заменяет `+` и `/` на `-` и `_`. |
| `New-LabVerifier` | Запрашивает у генератора случайных чисел 32 байта. После Base64URL получается строка из 43 символов. Вызываем функцию отдельно для каждого из трёх параметров. |
| `$verifier`, `$state`, `$nonce` | Verifier — исходная строка PKCE, которую отправим с кодом. State связывает redirect с нашим запросом. Nonce проверяем по ID token. |
| `$challenge` | Хеширует ASCII-байты verifier через SHA-256, затем кодирует результат в Base64URL. Это и есть преобразование `S256`. |
| `$authorization = @{ … }` | Создаёт таблицу параметров запроса. `response_type=code` просит код. `scope='openid email'` включает OIDC и запрашивает scope `email`. `redirect_uri` задаёт адрес возврата, который должен совпасть с настройкой клиента Keycloak. |
| `GetEnumerator`, `EscapeDataString`, `-join '&'` | Превращают таблицу в строку URL вида `имя=значение&имя=значение`. Кодирование сохраняет пробел в `openid email` и символы адреса callback. `$()` подставляет результат выражения в строку. |
| `Start-Process` | Открывает сформированный URL в браузере по умолчанию. Ожидай форму входа Keycloak или сразу callback, если браузер уже авторизован. |

### Прими callback и обменяй код

Войди как `learner` с паролем `learn-local`. Скопируй адрес страницы callback из браузера в PowerShell. После проверки `state` отправь форму на token endpoint.

```powershell
$callback = [uri](Read-Host 'URL страницы callback')
if ($callback.GetLeftPart([UriPartial]::Path) -cne $redirect) { throw 'Unexpected redirect URI' }
$params = [System.Web.HttpUtility]::ParseQueryString($callback.Query)
if ($params['state'] -cne $state -or -not $params['code']) { throw 'Invalid callback' }
$exchange = @{
    grant_type = 'authorization_code'
    client_id = 'lab-client'
    redirect_uri = $redirect
    code = $params['code']
    code_verifier = $verifier
}
$tokens = Invoke-RestMethod $oidc.token_endpoint -Method Post -Body $exchange
$tokens | Select-Object token_type, expires_in, scope
```

`Read-Host` ждёт **полный URL из адресной строки**. `[uri]` разбирает его на части. `GetLeftPart([UriPartial]::Path)` берёт адрес до `?` и проверяет его точное совпадение с `$redirect`. `ParseQueryString` извлекает параметры из части после `?`. Проверка `-cne` учитывает регистр, `-or -not $params['code']` ловит отсутствие кода. Если условия нарушены, `throw` прерывает блок до обмена.

`$exchange` собирает форму с `grant_type=authorization_code`, кодом, исходным verifier, идентификатором публичного клиента и redirect URI. `Invoke-RestMethod … -Method Post -Body $exchange` посылает её на token endpoint. Сохрани `$tokens`. Следующая строка показывает тип, срок жизни и scope, поэтому в выводе обычно видно `Bearer` и около `180` секунд. Значения токенов остаются в переменной и пригодятся дальше.

### Посмотри поля токенов

Посмотри на различия в `aud` и `nonce`. Декодирование здесь показывает claims для разбора. В клиенте проверка ID token также включает подпись, issuer, аудиторию, срок жизни и привязку `nonce` к текущему сеансу.

```powershell
function Read-LabJwtPayload([string]$jwt) {
    $part = $jwt.Split('.')[1].Replace('-', '+').Replace('_', '/')
    $part = $part.PadRight($part.Length + ((4 - $part.Length % 4) % 4), '=')
    [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($part)) | ConvertFrom-Json
}

Read-LabJwtPayload $tokens.access_token | Select-Object typ, iss, aud, exp, scope
$idClaims = Read-LabJwtPayload $tokens.id_token
$idClaims | Select-Object typ, iss, aud, exp, nonce
$idClaims.nonce -ceq $nonce
```

В `Read-LabJwtPayload` `.Split('.')[1]` выбирает среднюю часть JWT, то есть payload. Обратные замены и `PadRight` возвращают её к обычному Base64 с длиной, кратной четырём. После декодирования UTF-8 `ConvertFrom-Json` даёт объект с полями. У access token ищи `lab-api` в `aud` и `email` в `scope`. У ID token аудитория — `lab-client`. Строка с `-ceq` проверяет точное совпадение `nonce` и должна вывести `True`.

## Сравни JWKS и introspection

Проверь ответы для валидного access token и токенов другого назначения. Если прошло больше трёх минут с момента выпуска, получи новый access token через refresh или повтори авторизацию.

```powershell
function Get-LabStatus([string]$mode, [string]$token) {
    (Invoke-WebRequest "http://localhost:8181/api/$mode" -SkipHttpErrorCheck `
        -Headers @{ Authorization = "Bearer $token" }).StatusCode
}

Get-LabStatus 'jwt' $tokens.access_token           # 200
Get-LabStatus 'introspection' $tokens.access_token # 200
Get-LabStatus 'jwt' $tokens.id_token               # 401
Get-LabStatus 'introspection' 'inactive-token'     # 401
```

`Get-LabStatus` делает GET на указанный вариант API и добавляет токен в заголовок `Authorization`. `-SkipHttpErrorCheck` позволяет увидеть ожидаемый HTTP `401`, `403` или `503` как обычный результат. `.StatusCode` оставляет только число. Первые два вызова отправляют access token обоим проверяющим, третий проверяет ограничение по назначению токена, четвёртый показывает отказ для неактивной строки.

### Проверь зависимость от Keycloak

Сразу после успешного вызова `/api/jwt`, пока access token действует, выполни следующий блок. `stop` останавливает только Keycloak, поэтому API остаётся доступен. Для `jwt` ожидай `200` с уже закешированным ключом. Для `introspection` ожидай `503`, потому что API не может обратиться к провайдеру.

```powershell
docker compose stop keycloak
Get-LabStatus 'jwt' $tokens.access_token
Get-LabStatus 'introspection' $tokens.access_token
docker compose start keycloak
```

`start` поднимает прежний контейнер. Дождись успешного запроса к discovery endpoint перед следующим входом.

### Проверь scope и одноразовый код

1. Повтори блок генерации и входа с `scope = 'openid'` вместо `openid email`, затем получи новые `$tokens`. Подпись и аудитория остаются валидными. Проверь `Get-LabStatus 'jwt' $tokens.access_token` и `Get-LabStatus 'introspection' $tokens.access_token`. Оба пути должны вернуть `403` за отсутствующий scope.
2. Сразу после успешного обмена повтори `Invoke-RestMethod $oidc.token_endpoint -Method Post -Body $exchange` с той же формой. Код уже использован, поэтому ожидай `invalid_grant`.
3. Получи свежий код. В блоке callback перед запросом token endpoint поставь `$exchange['code_verifier'] = New-LabVerifier`. Так уйдёт другая строка подходящей длины, а у Keycloak останется challenge исходной. Ожидай отказ при обмене. Для следующего успешного обмена получи новый код.

Keycloak в этом упражнении выдаёт JWT. В режиме introspection API обращается с ним как с непрозрачной строкой. Подставить произвольную строку полезно для проверки `active=false`. Для проверки opaque token нужен issuer, который выпускает такой формат.

## Проверь rotation refresh token

Снова получи токены с `openid email`. Обнови их на token endpoint. Повтори запрос со старым refresh token и посмотри ошибку. Новый запрос к API использует новый access token.

```powershell
$refresh = @{
    grant_type = 'refresh_token'
    client_id = 'lab-client'
    refresh_token = $tokens.refresh_token
}
$updated = Invoke-RestMethod $oidc.token_endpoint -Method Post -Body $refresh
$updated.refresh_token -cne $tokens.refresh_token
Get-LabStatus 'jwt' $updated.access_token
$response = Invoke-WebRequest $oidc.token_endpoint -Method Post -Body $refresh -SkipHttpErrorCheck
$response.StatusCode
$response.Content
```

`$refresh` — форма с `grant_type=refresh_token` и исходным refresh token. После успешного запроса ответ сохранится в `$updated`. `-cne` сравнит старый и новый refresh token и должен показать `True`. Вызов `Get-LabStatus` проверяет, что новый access token даёт `200`. Последний POST снова использует старый токен из `$refresh`. `Invoke-WebRequest` сохраняет статус и тело ошибки, чтобы ты увидел `400` и `invalid_grant`.

Закончи, когда сможешь назвать, какой компонент выпускает каждый токен, какую аудиторию проверяет API и почему отзыв сессии и локальная JWT-валидация могут давать разные ответы. Выполни `docker compose down` из каталога OAuth. Compose удалит контейнеры. При следующем `up` Keycloak снова прочитает realm из файла, а ход прошлых сеансов храниться не будет.

## Как устроены файлы стенда

### compose.yaml

В [compose.yaml](compose.yaml) `keycloak` запускается из готового образа. `start-dev` включает режим разработки, `--import-realm` читает JSON из каталога `/opt/keycloak/data/import`. `volumes` монтирует туда `lab-realm.json` для чтения. Порт `8180` на компьютере направляется на `8080` в контейнере. `KC_HOSTNAME` задаёт публичную базу URL, которые Keycloak отдаёт браузеру и вписывает в issuer.

`api` имеет `build: .`, поэтому использует локальный `Dockerfile`. Порт `8181` на компьютере направляется на `8080` в этом контейнере. Оба сервиса могут слушать `8080` внутри своих контейнеров, поскольку у них разные сетевые пространства. Переменные API имеют разные задачи.

| Переменная | Как используется |
| --- | --- |
| `ISSUER` | Ожидаемое значение `iss` в токене и introspection. Оно совпадает с публичным адресом realm `http://localhost:8180/realms/lab`. |
| `KEYCLOAK_REALM_URL` | Внутренний адрес `http://keycloak:8080/realms/lab` для сетевых вызовов API к Keycloak. Имя `keycloak` разрешает сеть Compose. `localhost` внутри `api` указывал бы на сам `api`. |
| `INTROSPECTION_SECRET` | Учебный секрет конфиденциального клиента `lab-api`. Значение совпадает с `secret` у этого клиента в JSON. API аутентифицирует им свой запрос к introspection. |

### Dockerfile

| Строка [Dockerfile](Dockerfile) | Что делает |
| --- | --- |
| `FROM node:22-alpine` | Выбирает базовый Linux-образ с Node.js. |
| `WORKDIR /lab` | Задаёт рабочий каталог сборки и запуска в контейнере. |
| `RUN npm install … jose@6.2.12` | Во время сборки ставит конкретную версию библиотеки для JWT и JWKS. `--no-save` оставляет зависимости без записи в манифест проекта, `--no-audit` и `--no-fund` отключают соответствующий вывод при установке. |
| `COPY api.mjs ./` | Копирует код с компьютера в образ. Изменения кода попадают в контейнер после повторной сборки `up -d --build`. |
| `USER node` | Запускает приложение от пользователя `node` внутри контейнера. |
| `CMD ["node", "api.mjs"]` | Команда по умолчанию при старте контейнера. |

### lab-realm.json

[lab-realm.json](lab-realm.json) описывает учебный realm `lab`, то есть отдельную область пользователей, клиентов и настроек Keycloak. `enabled: true` включает realm. `sslRequired: none` допускает HTTP в локальном опыте. `accessTokenLifespan: 180` задаёт срок access token в секундах. `revokeRefreshToken: true` и `refreshTokenMaxReuse: 0` включают замену refresh token с запретом повторно использовать старый.

| Запись | Как проявляется в опыте |
| --- | --- |
| `clients → lab-client` | `publicClient: true` означает клиент без секрета. `standardFlowEnabled` включает Code Flow, `directAccessGrantsEnabled: false` отключает прямой обмен пароля на токен. `redirectUris` содержит точный callback. `pkce.code.challenge.method: S256` требует нужный метод PKCE. |
| `defaultClientScopes`, `optionalClientScopes` | `basic` и `profile` подключены по умолчанию. `email` подключается по запросу, поэтому `openid` без `email` даёт `403` на учебном API. |
| `protocolMappers → lab-api-audience` | Добавляет `lab-api` в аудиторию access token и ответа introspection. `id.token.claim: false` оставляет ID token предназначенным для `lab-client`. |
| `clients → lab-api` | `publicClient: false` и `clientAuthenticatorType: client-secret` задают конфиденциального клиента. Флаг `standardFlowEnabled: false` отключает браузерный вход от его имени. Здесь он нужен для аутентификации introspection. |
| `users → learner` | Создаёт учебную учётную запись. `emailVerified: true` и `temporary: false` позволяют сразу войти и пройти упражнение с указанным паролем. |

### api.mjs

В [api.mjs](api.mjs) `/callback` показывает инструкцию скопировать URL. Обмен выполняет PowerShell. `/api/jwt` проверяет подпись `RS256` по JWKS, ожидаемые `iss` и `aud`, обязательные `exp` и `sub` и тип `Bearer`. Библиотека `jose` кеширует набор ключей для повторных проверок. `/api/introspection` вызывает Keycloak через POST с токеном и Basic-аутентификацией, ждёт до трёх секунд и проверяет `active`, issuer, аудиторию, тип токена и субъект в ответе. После любой из проверок API требует scope `email`. Поэтому исправный токен без разрешения даёт `403`, неподходящий токен — `401`, а сбой связи при проверке — `503`.

[Keycloak OIDC endpoints](https://www.keycloak.org/securing-apps/oidc-layers) · [PKCE](https://www.rfc-editor.org/rfc/rfc7636.html) · [Token Introspection](https://www.rfc-editor.org/rfc/rfc7662.html)
