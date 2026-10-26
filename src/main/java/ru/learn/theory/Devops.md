# DevOps: контейнеры и Kubernetes

> Конспект-тренажёр для подготовки к собеседованиям.
> Параметры JVM в контейнере проверены на **OpenJDK 25** (cgroup v2). Манифесты Kubernetes проверены на корректность YAML. Docker и кластера в среде проверки не было: Dockerfile и команды `kubectl` приведены по документации и не запускались. Факты о версиях Kubernetes — по официальной документации, ссылки внутри.

## Как пользоваться файлом

Каждая тема устроена одинаково:

1. **Короткий ответ** — то, что нужно сказать устно за 30–60 секунд.
2. **Подробности** — под спойлером `<details>`: механизм, ограничения, примеры.
3. **Уточняющие вопросы** — то, что интервьюер спросит следом.
4. **Практика** — команды, манифест или сценарий отладки.

Отмечайте уверенность прямо в файле:

- `[ ]` — не знаю;
- `[~]` — отвечаю с подсказкой;
- `[x]` — объясняю сам, с примером.

Повторяйте прежде всего то, что отмечено `[ ]` и `[~]`.

---

## Содержание

0. [Быстрое повторение](#0-быстрое-повторение)
1. [Контейнеры и образы](#1-контейнеры-и-образы)
2. [Dockerfile](#2-dockerfile)
3. [Docker на практике](#3-docker-на-практике)
4. [Архитектура Kubernetes](#4-архитектура-kubernetes)
5. [Объекты Kubernetes](#5-объекты-kubernetes)
6. [Сеть: Service, Ingress, Gateway API](#6-сеть-service-ingress-gateway-api)
7. [Пробы и жизненный цикл пода](#7-пробы-и-жизненный-цикл-пода)
8. [Ресурсы, QoS и автомасштабирование](#8-ресурсы-qos-и-автомасштабирование)
9. [JVM в контейнере](#9-jvm-в-контейнере)
10. [Конфигурация и секреты](#10-конфигурация-и-секреты)
11. [Выкатка и CI/CD](#11-выкатка-и-cicd)
12. [Отладка: что сломалось](#12-отладка-что-сломалось)
13. [Пробное собеседование](#13-пробное-собеседование)
14. [Источники](#14-источники)
- [Приложение: полный манифест сервиса](#приложение-полный-манифест-сервиса)

---

## 0. Быстрое повторение

Ответьте вслух, не открывая разделы. Ответы — по ссылкам.

| Уверенность | Вопрос | Раздел |
|---|---|---|
| `[ ]` | Чем контейнер отличается от виртуальной машины? | [1](#1-контейнеры-и-образы) |
| `[ ]` | Что такое слой образа и почему важен порядок инструкций? | [2.2](#22-слои-и-кеш-сборки) |
| `[ ]` | `CMD` и `ENTRYPOINT` — в чём разница? | [2.1](#21-основные-инструкции) |
| `[ ]` | Чем `EXPOSE` отличается от `-p`? | [2.1](#21-основные-инструкции) |
| `[ ]` | Зачем multi-stage сборка? | [2.3](#23-образ-для-spring-boot) |
| `[ ]` | Почему процесс в контейнере не получает SIGTERM? | [3.2](#32-pid-1-и-сигналы) |
| `[ ]` | Из чего состоит control plane? | [4](#4-архитектура-kubernetes) |
| `[ ]` | Deployment, StatefulSet, DaemonSet — когда что? | [5](#5-объекты-kubernetes) |
| `[ ]` | Типы Service. Что такое headless Service? | [6.1](#61-service) |
| `[ ]` | Что случилось с ingress-nginx и что такое Gateway API? | [6.2](#62-ingress-и-gateway-api) |
| `[ ]` | Readiness, liveness, startup — в чём разница? | [7.1](#71-пробы) |
| `[ ]` | Как под корректно завершается? Зачем preStop? | [7.2](#72-завершение-пода) |
| `[ ]` | Requests и limits: что будет при превышении CPU и памяти? | [8.1](#81-requests-и-limits) |
| `[ ]` | Классы QoS? | [8.2](#82-классы-qos) |
| `[ ]` | Можно ли изменить ресурсы пода без пересоздания? | [8.4](#84-изменение-ресурсов-без-пересоздания) |
| `[ ]` | Сколько heap получит JVM в контейнере по умолчанию? | [9](#9-jvm-в-контейнере) |
| `[ ]` | Почему Secret — это не шифрование? | [10](#10-конфигурация-и-секреты) |
| `[ ]` | Под в `CrashLoopBackOff` — порядок действий? | [12](#12-отладка-что-сломалось) |

---

## 1. Контейнеры и образы

`[ ]` **Короткий ответ:** образ — неизменяемый шаблон: слои файловой системы плюс метаданные (команда запуска, переменные, порты). Контейнер — изолированный процесс, запущенный из образа. Это не виртуальная машина: контейнер разделяет ядро хоста, а изоляция обеспечивается механизмами ядра Linux — namespaces (что процесс видит) и cgroups (сколько ресурсов может потребить).

| | Контейнер | Виртуальная машина |
|---|---|---|
| Ядро | Общее с хостом | Своё |
| Запуск | Миллисекунды–секунды | Десятки секунд |
| Размер | Мегабайты | Гигабайты |
| Изоляция | Процессы ядра, слабее | Гипервизор, сильнее |
| Что внутри | Приложение и его зависимости | Целая ОС |

<details>
<summary>Namespaces и cgroups подробнее</summary>

- **Namespaces:** `pid` (свои номера процессов, приложение видит себя PID 1), `net` (свой сетевой стек), `mnt` (своя файловая система), `uts` (имя хоста), `ipc`, `user` (отображение пользователей).
- **cgroups:** ограничения CPU, памяти, ввода-вывода, числа процессов. Превышение лимита памяти — процесс убивает OOM killer ядра.
- Образ хранится как набор слоёв. При запуске сверху добавляется тонкий записываемый слой контейнера; при удалении контейнера он исчезает. Для постоянных данных нужны тома (volumes).
- Стандарты образов и среды выполнения — OCI. Docker — один из инструментов; Kubernetes запускает контейнеры через CRI (containerd, CRI-O), Docker там не обязателен.

</details>

---

## 2. Dockerfile

### 2.1. Основные инструкции

| Инструкция | Что делает |
|---|---|
| `FROM` | Базовый образ (не обязательно образ ОС: бывают `scratch`, distroless) |
| `WORKDIR` | Рабочий каталог для следующих инструкций |
| `COPY` / `ADD` | Копировать файлы в образ. `ADD` умеет распаковывать архивы и скачивать URL — обычно лучше `COPY` |
| `RUN` | Выполняется **при сборке**, создаёт слой |
| `ENV` | Переменная окружения в образе и контейнере |
| `ARG` | Переменная только для сборки (не попадает в окружение контейнера, но видна в истории образа) |
| `EXPOSE` | Документирует порт. Сам по себе **не публикует** его на хосте |
| `USER` | Пользователь для следующих инструкций и запуска |
| `HEALTHCHECK` | Проверка здоровья для Docker (Kubernetes её игнорирует, там свои пробы) |
| `ENTRYPOINT` | Основной исполняемый файл |
| `CMD` | Аргументы по умолчанию (или команда, если нет `ENTRYPOINT`) |

`[ ]` **CMD и ENTRYPOINT:** `ENTRYPOINT ["java", "-jar", "app.jar"]` + `CMD ["--server.port=8080"]` — при `docker run image --debug` заменится только `CMD`. Если указан только `CMD`, его целиком заменяют аргументы `docker run`.

**Exec-форма и shell-форма:** `CMD ["java", "-jar", "app.jar"]` (exec) запускает java как PID 1. `CMD java -jar app.jar` (shell) запускает `/bin/sh -c ...`, и PID 1 — оболочка, которая не передаёт сигналы приложению. См. [3.2](#32-pid-1-и-сигналы).

`[ ]` **EXPOSE и -p:** `EXPOSE` — метаданные образа. `docker run -p 8080:80` публикует порт 80 контейнера на порту 8080 хоста. Без `-p` порт не доступен через интерфейс хоста.

### 2.2. Слои и кеш сборки

`[ ]` **Короткий ответ:** каждая инструкция `RUN`, `COPY`, `ADD` создаёт слой. При пересборке Docker переиспользует слои из кеша до первой изменившейся инструкции; всё после неё пересобирается. Поэтому редко меняющееся (зависимости) копируют раньше часто меняющегося (исходный код).

```dockerfile
# Плохо: любое изменение кода сбрасывает кеш зависимостей
COPY . .
RUN ./mvnw package

# Хорошо: зависимости кешируются, пока не изменился pom.xml
COPY pom.xml .
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests
```

Удаление файла в следующем слое не уменьшает образ и **не убирает секрет**: он остаётся в предыдущем слое и извлекается через `docker history` или распаковку образа. Секреты на этапе сборки передают через `RUN --mount=type=secret`.

### 2.3. Образ для Spring Boot

`[ ]` **Короткий ответ:** multi-stage сборка: в первой стадии — JDK и Maven для компиляции, в финальной — только JRE и результат. Образ меньше, в нём нет компилятора и исходников, меньше поверхность атаки. Плюс: не-root пользователь, фиксированная версия базового образа, exec-форма запуска.

```dockerfile
# Стадия сборки
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q package -DskipTests \
 && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /extracted

# Финальная стадия
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
# Слои от редко к часто меняющимся: зависимости, загрузчик, snapshot-зависимости, код
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
```

<details>
<summary>Чек-лист образа для продакшена</summary>

- Минимальная база (JRE вместо JDK, distroless), фиксированный тег или digest вместо `latest`.
- Не-root пользователь (`USER`), в Kubernetes дополнительно `runAsNonRoot: true`.
- Секреты не в образе и не в `ENV`/`ARG`.
- `.dockerignore`: исключить `.git`, `target`, локальные конфиги.
- Сканирование уязвимостей (Trivy, Grype) в CI.
- Exec-форма `ENTRYPOINT`, корректная обработка SIGTERM.
- Альтернативы Dockerfile для Java: Jib, Cloud Native Buildpacks (`./mvnw spring-boot:build-image`).

</details>

---

## 3. Docker на практике

### 3.1. Основные команды

```bash
docker build -t orders:1.4.2 .                     # собрать образ
docker run --rm -d --name orders -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev --memory=1g orders:1.4.2
docker ps                                          # запущенные контейнеры
docker logs -f orders                              # логи (stdout/stderr)
docker exec -it orders sh                          # оболочка внутри (если она есть в образе)
docker inspect orders                              # конфигурация, сеть, тома
docker stats                                       # потребление CPU и памяти
docker history orders:1.4.2                        # слои образа
docker compose up -d                               # поднять сервисы из compose.yaml
```

**Docker Compose** — описание нескольких сервисов (приложение, БД, Redis) в одном файле для локальной разработки и простых окружений. Сервисы видят друг друга по имени в общей сети. `depends_on` с `condition: service_healthy` ждёт проверки здоровья зависимости, а не просто её запуска.

### 3.2. PID 1 и сигналы

`[ ]` **Короткий ответ:** `docker stop` и Kubernetes отправляют SIGTERM процессу PID 1, ждут (по умолчанию 10 с в Docker, 30 с в Kubernetes) и затем отправляют SIGKILL. Если PID 1 — оболочка (`sh -c`), она не передаёт сигнал приложению, и оно убивается без корректного завершения: незавершённые запросы обрываются, соединения не закрываются.

Решения: exec-форма `ENTRYPOINT`; в shell-скрипте запускать приложение через `exec java ...`; `docker run --init` или `tini` в качестве PID 1 для пересылки сигналов и сбора зомби-процессов.

---

## 4. Архитектура Kubernetes

`[ ]` **Короткий ответ:** Kubernetes — платформа оркестрации контейнеров по декларативной модели: вы описываете желаемое состояние, контроллеры постоянно приводят к нему фактическое. Control plane — API-сервер, etcd, планировщик, менеджер контроллеров. На каждом узле — kubelet, среда выполнения контейнеров и, обычно, kube-proxy.

| Компонент | Где | Роль |
|---|---|---|
| `kube-apiserver` | Control plane | Единственная точка входа к API; аутентификация, авторизация, валидация, запись в etcd |
| `etcd` | Control plane | Согласованное key-value хранилище (Raft) всего состояния кластера |
| `kube-scheduler` | Control plane | Выбирает узел для новых подов по ресурсам, affinity, taints |
| `kube-controller-manager` | Control plane | Контроллеры: Deployment, ReplicaSet, Node, Job и др. — цикл «желаемое vs фактическое» |
| `cloud-controller-manager` | Control plane | Интеграция с облаком: балансировщики, диски, узлы |
| `kubelet` | Узел | Агент: получает спецификации подов, запускает контейнеры, выполняет пробы |
| Container runtime | Узел | containerd, CRI-O — через интерфейс CRI |
| `kube-proxy` | Узел | Правила iptables/IPVS для Service. В некоторых кластерах заменён eBPF-реализацией CNI (Cilium) |
| CNI-плагин | Узел | Сеть подов: адреса, маршрутизация, NetworkPolicy |

Control plane для доступности разворачивают в нескольких экземплярах (обычно 3 узла etcd для кворума). Объекты хранятся в etcd, но выполняются не в нём.

<details>
<summary>Что происходит при kubectl apply -f deployment.yaml</summary>

1. `kubectl` отправляет объект в API-сервер; тот проверяет права, валидирует и сохраняет в etcd.
2. Контроллер Deployment видит новый объект и создаёт ReplicaSet.
3. Контроллер ReplicaSet создаёт объекты Pod без назначенного узла.
4. Планировщик выбирает узел для каждого пода и записывает назначение.
5. kubelet на этом узле видит назначенный под, скачивает образ, запускает контейнеры через CRI, выполняет пробы.
6. Когда readiness-проба прошла, под попадает в EndpointSlice сервиса и начинает получать трафик.

Все компоненты общаются только через API-сервер, а не друг с другом напрямую.

</details>

---

## 5. Объекты Kubernetes

`[ ]` **Короткий ответ:** Pod — минимальная единица (один или несколько контейнеров с общей сетью и томами). Поды почти никогда не создают вручную: ими управляют контроллеры — Deployment для stateless-сервисов, StatefulSet для сервисов со стабильной идентичностью, DaemonSet — по поду на узел, Job/CronJob — для разовых и периодических задач.

| Объект | Назначение |
|---|---|
| **Namespace** | Логическая область: имена, квоты, права. Не сетевая изоляция сама по себе |
| **Pod** | Контейнеры с общим сетевым пространством (`localhost`) и томами |
| **ReplicaSet** | Поддерживает заданное число подов. Обычно создаётся Deployment-ом |
| **Deployment** | Stateless-приложение: число реплик, rolling update, откат |
| **StatefulSet** | Стабильные имена (`db-0`, `db-1`), упорядоченный запуск, свой PVC на под. Сам не делает данные отказоустойчивыми — репликацию обеспечивает приложение |
| **DaemonSet** | Под на каждом (или выбранном) узле: сборщики логов и метрик, CNI |
| **Job** | Довести число успешных завершений до заданного |
| **CronJob** | Job по расписанию. Возможны пропуски и повторы — обработчик должен быть идемпотентным; `concurrencyPolicy: Forbid` запрещает параллельные запуски |
| **Service** | Стабильный адрес и DNS-имя для группы подов |
| **Ingress / Gateway API** | HTTP(S)-маршрутизация извне |
| **ConfigMap / Secret** | Конфигурация и секретные данные |
| **PersistentVolumeClaim** | Запрос на постоянное хранилище |
| **ServiceAccount** | Учётная запись для процессов в подах (доступ к API, облачные роли) |
| **HorizontalPodAutoscaler** | Число реплик по метрикам |
| **PodDisruptionBudget** | Сколько подов можно одновременно выключить при плановых операциях (обновление узлов) |
| **NetworkPolicy** | Правила сетевого доступа между подами. Работает, только если CNI их поддерживает |
| **LimitRange** | Значения ресурсов по умолчанию, минимум и максимум для контейнеров в namespace |
| **ResourceQuota** | Суммарный лимит ресурсов и объектов на namespace |

### 5.1. Нативные sidecar-контейнеры

`[ ]` **Короткий ответ:** sidecar объявляется в `initContainers` с `restartPolicy: Always`. Он стартует до основных контейнеров, работает всё время жизни пода и останавливается **после** основного контейнера. Функция стабильна с Kubernetes 1.33 ([Kubernetes: Sidecar Containers](https://kubernetes.io/docs/concepts/workloads/pods/sidecar-containers/)).

```yaml
spec:
  initContainers:
    - name: log-shipper
      image: example/log-shipper:2.1
      restartPolicy: Always        # это делает init-контейнер sidecar-ом
  containers:
    - name: app
      image: example/app:1.0
```

Раньше sidecar был обычным контейнером, и возникали проблемы: Job не завершался, потому что sidecar продолжал работать; при остановке sidecar-прокси умирал раньше приложения и обрывал запросы. Нативный sidecar решает обе: в Job он не мешает завершению, а при остановке kubelet ждёт полной остановки основного контейнера и только затем завершает sidecar-ы в обратном порядке ([Kubernetes: Sidecar Containers](https://kubernetes.io/docs/concepts/workloads/pods/sidecar-containers/)).

---

## 6. Сеть: Service, Ingress, Gateway API

### 6.1. Service

`[ ]` **Короткий ответ:** поды смертны, их адреса меняются. Service даёт стабильный виртуальный адрес и DNS-имя (`orders.shop.svc.cluster.local`) и распределяет трафик по подам, выбранным по меткам (selector). В балансировку попадают только поды, прошедшие readiness-пробу.

| Тип | Доступен | Когда |
|---|---|---|
| `ClusterIP` (по умолчанию) | Только внутри кластера | Взаимодействие сервисов |
| `NodePort` | `IP-узла:порт` (30000–32767) | Тесты, свои балансировщики |
| `LoadBalancer` | Внешний балансировщик облака | Публичный TCP/UDP-сервис |
| `ExternalName` | DNS CNAME на внешнее имя | Внешняя БД под внутренним именем |
| Headless (`clusterIP: None`) | DNS возвращает адреса подов напрямую | StatefulSet, клиентская балансировка, кластеры БД |

**Уточнение интервьюера:** «gRPC-клиент держит одно соединение с Service — нагрузка ложится на один под. Почему?» — ClusterIP балансирует на уровне TCP-соединений (L4). Одно долгоживущее HTTP/2-соединение = один под. Решения: headless Service и клиентская балансировка, L7-прокси (service mesh), периодическое переподключение.

### 6.2. Ingress и Gateway API

`[ ]` **Короткий ответ:** Ingress описывает HTTP(S)-маршруты (хост, путь → Service), а применяет их Ingress Controller — без контроллера объект ничего не делает. Gateway API — современная замена Ingress: разделяет роли (инфраструктура описывает `Gateway`, команды приложений — `HTTPRoute`), поддерживает разделение трафика по весам, заголовки, gRPC, TCP. Самый популярный контроллер ingress-nginx выведен из поддержки в марте 2026 года.

<details>
<summary>Что именно случилось с ingress-nginx</summary>

Kubernetes SIG Network и Security Response Committee объявили: после марта 2026 года не будет выпусков, исправлений ошибок и уязвимостей; репозитории станут только для чтения. Существующие установки продолжат работать, образы и Helm-чарты останутся доступными. Рекомендация — мигрировать на Gateway API или другой Ingress-контроллер ([Kubernetes Blog](https://kubernetes.io/blog/2025/11/11/ingress-nginx-retirement/)).

Важно не путать: выведен из поддержки **контроллер** ingress-nginx, а не API `Ingress`. Также это не NGINX Ingress Controller от F5/NGINX — это другой продукт.

</details>

<details>
<summary>Маршрут в Gateway API</summary>

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: orders
spec:
  parentRefs:
    - name: public-gateway          # Gateway описывает команда платформы
  hostnames: ["shop.example.com"]
  rules:
    - matches:
        - path: { type: PathPrefix, value: /api/orders }
      backendRefs:
        - name: orders
          port: 80
```

Канареечная выкатка — два `backendRefs` с полем `weight` (например, 90 и 10).

</details>

### 6.3. NetworkPolicy

По умолчанию все поды могут общаться со всеми. NetworkPolicy ограничивает входящий и исходящий трафик по меткам подов, namespace и портам. Обычная практика — политика «запретить всё» на namespace и явные разрешения. Без поддержки в CNI (Calico, Cilium) политики молча не действуют.

---

## 7. Пробы и жизненный цикл пода

### 7.1. Пробы

`[ ]` **Короткий ответ:** **readiness** — готов ли под принимать трафик; при неудаче его убирают из Service, но не перезапускают. **liveness** — жив ли процесс; при неудаче контейнер перезапускается. **startup** — для медленного старта: пока не пройдёт, остальные пробы не выполняются.

| Проба | При неудаче | Что проверять | Ошибка |
|---|---|---|---|
| Readiness | Убрать из Service | Готовность обслуживать: прогрев, доступ к критичным зависимостям | Не настроить → трафик идёт в незапущенное приложение |
| Liveness | Перезапустить контейнер | Только сам процесс: не завис, не в deadlock | Проверять БД → при сбое БД перезапускаются все поды, усугубляя аварию |
| Startup | Перезапустить после `failureThreshold × periodSeconds` | То же, что liveness | Без неё приходится ставить большой `initialDelaySeconds` у liveness |

В Spring Boot Actuator на Kubernetes автоматически появляются группы `/actuator/health/liveness` и `/actuator/health/readiness`. Внешние зависимости по умолчанию не входят в liveness — и не должны.

**Уточнение интервьюера:** «Под под нагрузкой медленно отвечает на liveness — что будет?» — liveness упадёт по таймауту, контейнер перезапустится, его нагрузка уйдёт на соседей, те тоже не успеют ответить — каскад перезапусков. Поэтому у liveness щадящие пороги, а перегрузку обрабатывает readiness и ограничение нагрузки.

### 7.2. Завершение пода

`[ ]` **Короткий ответ:** при удалении пода параллельно происходят две вещи: под убирается из точек балансировки Service, и контейнеры получают preStop-хук, затем SIGTERM. По истечении `terminationGracePeriodSeconds` (30 с по умолчанию) — SIGKILL. Поскольку обновление балансировки распространяется не мгновенно, короткая пауза в preStop даёт ей время, и новые запросы не попадают в завершающийся под.

<details>
<summary>Последовательность по документации</summary>

1. Запрос на удаление; под получает статус `Terminating` и срок завершения (по умолчанию 30 с).
2. Если есть preStop-хук и срок не 0 — kubelet выполняет его. Если хук не успел за срок, даётся однократное продление на 2 секунды.
3. Одновременно control plane помечает под в EndpointSlice как не готовый (`ready: false`), балансировщики перестают слать ему обычный трафик.
4. Основному процессу каждого контейнера отправляется SIGTERM (или `STOPSIGNAL` образа).
5. По истечении срока оставшиеся процессы получают SIGKILL.

([Kubernetes: Pod Lifecycle](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/))

Приложение должно по SIGTERM перестать принимать новые запросы, дообработать текущие и закрыть соединения. В Spring Boot корректное завершение включено по умолчанию; время задаёт `spring.lifecycle.timeout-per-shutdown-phase`.

</details>

---

## 8. Ресурсы, QoS и автомасштабирование

### 8.1. Requests и limits

`[ ]` **Короткий ответ:** **requests** — сколько ресурсов гарантируется; по ним планировщик выбирает узел. **limits** — потолок. Превышение лимита памяти → контейнер убивается (`OOMKilled`). Превышение лимита CPU → троттлинг: процесс замедляется, но не убивается.

| | CPU | Память |
|---|---|---|
| Единицы | `500m` = 0,5 ядра | `768Mi` |
| Превышение limit | Троттлинг (задержки) | `OOMKilled`, перезапуск |
| Без requests | Под может не получить CPU под нагрузкой | Под первым кандидат на вытеснение |
| Практика | Requests ставить, лимит CPU — по ситуации (троттлинг бьёт по p99) | Limit = request, чтобы не было неожиданного OOM |

### 8.2. Классы QoS

| Класс | Условие | При нехватке памяти на узле |
|---|---|---|
| `Guaranteed` | У каждого контейнера requests = limits для CPU и памяти | Вытесняются последними |
| `Burstable` | Requests заданы, но меньше limits (или не все) | Средний приоритет |
| `BestEffort` | Нет ни requests, ни limits | Вытесняются первыми |

### 8.3. Автомасштабирование

- **HPA** меняет число реплик по метрикам (CPU, память, пользовательские). Utilization считается **от requests**, поэтому без requests HPA по CPU не работает. Масштабирование не мгновенное (сбор метрик, запуск подов, прогрев JVM) — не заменяет ограничение входящей нагрузки.
- **VPA** рекомендует или меняет requests и limits.
- **Cluster Autoscaler / Karpenter** добавляет узлы, когда поды не помещаются.
- **KEDA** — масштабирование по внешним событиям (длина очереди, лаг Kafka), в том числе до нуля.

### 8.4. Изменение ресурсов без пересоздания

`[ ]` **Короткий ответ:** с Kubernetes 1.35 изменение CPU и памяти работающего пода «на месте» (In-Place Pod Resize) — стабильная функция. Запрос делается через подресурс `resize`, часто без перезапуска контейнера ([Kubernetes Blog](https://kubernetes.io/blog/2025/12/19/kubernetes-v1-35-in-place-pod-resize-ga/)).

Ограничения по документации: менять можно только CPU и память; уменьшение лимита памяти допускается, только если текущее потребление ниже нового лимита (проверка без гарантии); не работает со swap и статическими менеджерами CPU и памяти. Для Java важно: JVM не умеет менять память без перезапуска — heap задаётся при старте ([Kubernetes Blog](https://kubernetes.io/blog/2025/12/19/kubernetes-v1-35-in-place-pod-resize-ga/)).

---

## 9. JVM в контейнере

`[ ]` **Короткий ответ:** современная JVM видит лимиты cgroup (`UseContainerSupport` включён по умолчанию). По умолчанию максимальный heap — 25% от лимита памяти контейнера, это обычно мало. Ставят `-XX:MaxRAMPercentage=70..75`, но не 100%: кроме heap JVM тратит память на metaspace, стеки потоков, JIT-код, direct-буферы, GC — и всё это входит в лимит контейнера.

<details>
<summary>Результат запуска на OpenJDK 25</summary>

```text
$ java -XX:+PrintFlagsFinal -version | grep -E 'MaxRAMPercentage|InitialRAMPercentage|UseContainerSupport'
   double InitialRAMPercentage = 1.562500
   double MaxRAMPercentage     = 25.000000
   bool   UseContainerSupport  = true

$ java -XshowSettings:system -version
Operating System Metrics:
    Provider: cgroupv2
    Effective CPU Count: 2
    Memory Limit: 7.65G
```

При лимите 1 ГиБ и настройках по умолчанию heap будет около 256 МиБ.

</details>

| Симптом | Причина | Что делать |
|---|---|---|
| `OOMKilled`, но в логах нет `OutOfMemoryError` | Не heap, а вся память процесса превысила лимит | Уменьшить `MaxRAMPercentage`, проверить Native Memory Tracking (`-XX:NativeMemoryTracking=summary`, `jcmd VM.native_memory`) |
| `java.lang.OutOfMemoryError: Java heap space` | Heap мал или утечка | Увеличить heap или искать утечку (heap dump) |
| Высокий p99 при низком среднем CPU | Троттлинг по CPU limit | Метрика `container_cpu_cfs_throttled_periods_total`, поднять или убрать CPU limit |
| Медленный старт | JIT-прогрев, мало CPU при старте | Startup-проба, AppCDS/AOT-кеш, больше CPU request |

`availableProcessors()` учитывает CPU-лимит — от него зависят размеры пулов (`ForkJoinPool`, GC-потоки). При лимите в 1 ядро JVM выберет Serial GC; явно задать сборщик можно флагом.

---

## 10. Конфигурация и секреты

`[ ]` **Короткий ответ:** ConfigMap — несекретная конфигурация, Secret — секретная. Но Secret по умолчанию только закодирован в Base64 — это не шифрование: любой, кто может читать Secret через API или имеет доступ к etcd, видит значение. Нужно: RBAC на чтение секретов, шифрование etcd at rest, по возможности внешний менеджер секретов (Vault, облачный KMS) через External Secrets Operator или CSI-драйвер.

```bash
$ echo -n 's3cr3t' | base64
czNjcjN0
$ echo czNjcjN0 | base64 -d
s3cr3t
```

- Передавать в под: переменные окружения (`env`, `envFrom`) или файлы (том). Файлы лучше: обновляются без перезапуска (с задержкой), не видны в `kubectl describe` и дампах окружения, не наследуются дочерними процессами.
- Изменение ConfigMap не перезапускает поды. Для применения — перезапуск (`kubectl rollout restart`) или хеш конфигурации в аннотации шаблона.
- Не коммитить секреты в Git; для GitOps — Sealed Secrets, SOPS или External Secrets.

---

## 11. Выкатка и CI/CD

`[ ]` **Короткий ответ:** Deployment по умолчанию делает rolling update: постепенно поднимает новые поды и гасит старые. `maxSurge` — сколько лишних подов можно создать, `maxUnavailable` — сколько можно потерять. Пока идёт выкатка, работают обе версии — код и схема БД должны быть совместимы.

```bash
kubectl rollout status deployment/orders     # следить за выкаткой
kubectl rollout history deployment/orders    # ревизии
kubectl rollout undo deployment/orders       # откат на предыдущую
kubectl rollout restart deployment/orders    # перезапустить поды
```

- `maxUnavailable: 0` + `maxSurge: 1` — без потери мощности, но медленнее и нужно место в кластере.
- Readiness-проба обязательна: без неё поды считаются готовыми сразу и получают трафик до старта.
- Канарейки и blue-green — через Gateway API (веса), Argo Rollouts, Flagger, service mesh.
- **GitOps** (Argo CD, Flux): желаемое состояние кластера в Git, контроллер в кластере синхронизирует. Изменения — через pull request, история и откат — через Git.
- Типичный конвейер: сборка и тесты → сборка образа → сканирование → публикация в реестр с неизменяемым тегом (версия или commit SHA) → обновление манифеста → выкатка → проверка метрик.

---

## 12. Отладка: что сломалось

`[ ]` **Короткий ответ:** порядок: `kubectl get pods` (статус) → `kubectl describe pod` (события внизу) → `kubectl logs` (и `--previous` для упавшего контейнера) → метрики и `kubectl exec` / `kubectl debug`.

| Статус | Значит | Где смотреть |
|---|---|---|
| `Pending` | Под не назначен на узел | `describe` → Events: нехватка ресурсов, taints, нет подходящего PVC |
| `ImagePullBackOff` / `ErrImagePull` | Не скачать образ | Имя и тег, доступ к реестру, `imagePullSecrets` |
| `CrashLoopBackOff` | Контейнер падает сразу после запуска, kubelet перезапускает с растущей паузой | `logs --previous`, код выхода в `describe` |
| `OOMKilled` (код 137) | Превышен лимит памяти | Лимит и настройки JVM, [9](#9-jvm-в-контейнере) |
| `Running`, но `0/1 Ready` | Не проходит readiness | Эндпоинт пробы, зависимости, `describe` |
| `Running`, трафик не доходит | Service не видит поды | `kubectl get endpointslices`, совпадение selector и меток, `targetPort` |

```bash
kubectl get pods -n shop -o wide
kubectl describe pod orders-7d9f8-abc12 -n shop
kubectl logs orders-7d9f8-abc12 -n shop --previous
kubectl get events -n shop --sort-by=.lastTimestamp
kubectl top pod -n shop                                   # нужен metrics-server
kubectl debug -it orders-7d9f8-abc12 --image=busybox --target=app   # временный контейнер для отладки
kubectl port-forward svc/orders 8080:80 -n shop
```

Коды выхода: `137` = 128 + 9 (SIGKILL, часто OOM), `143` = 128 + 15 (SIGTERM), `1` — ошибка приложения.

---

## 13. Пробное собеседование

Сначала ответьте вслух, потом откройте ответ.

<details>
<summary>1. После каждой выкатки пользователи получают ошибки 502 в течение нескольких секунд. Почему?</summary>

Варианты: нет readiness-пробы — новые поды получают трафик до готовности; приложение не обрабатывает SIGTERM (shell-форма запуска) — старые поды обрывают запросы; балансировщик ещё шлёт трафик в завершающийся под — нужна пауза в preStop; `maxUnavailable` слишком велик. Проверить каждое: пробы, `ENTRYPOINT`, корректное завершение в приложении, preStop, параметры стратегии.

</details>

<details>
<summary>2. Под с Java-сервисом раз в несколько часов получает OOMKilled, но heap заполнен на 60%. Что происходит?</summary>

Лимит контейнера превышает вся память процесса, а не heap: metaspace, стеки потоков (особенно если потоков тысячи), direct-буферы (Netty), JIT, GC. Включить Native Memory Tracking и сравнить с лимитом; уменьшить `MaxRAMPercentage`; ограничить `MaxDirectMemorySize`, число потоков; проверить утечки native-памяти.

</details>

<details>
<summary>3. Когда liveness-проба должна проверять базу данных?</summary>

Почти никогда. Если БД недоступна, перезапуск приложения её не починит, а одновременный перезапуск всех подов создаст шквал подключений при восстановлении. Зависимость можно отражать в readiness (если без неё под действительно бесполезен), но и тут осторожно: если все поды станут неготовыми, сервис полностью недоступен, хотя мог бы отдавать деградированные ответы.

</details>

<details>
<summary>4. Как развернуть PostgreSQL в Kubernetes? Хватит ли StatefulSet?</summary>

StatefulSet даёт стабильные имена и свой том на под, но не репликацию, failover, бэкапы и обновления. Для этого — оператор (CloudNativePG, Zalando Postgres Operator) или управляемая БД вне кластера. Нужны также PodDisruptionBudget, anti-affinity по узлам и зонам, проверенное восстановление из бэкапа.

</details>

<details>
<summary>5. Разработчик положил пароль в ENV в Dockerfile, а потом удалил строку. Проблема решена?</summary>

Нет, если образ уже собирался и публиковался: значение есть в слоях и в `docker history` старых образов. Нужно сменить пароль, удалить старые образы из реестра, передавать секреты в рантайме (Secret, менеджер секретов), а при сборке — через `RUN --mount=type=secret`.

</details>

<details>
<summary>6. HPA настроен на 70% CPU, но реплик не добавляется. Почему?</summary>

Проверить: заданы ли CPU requests (utilization считается от них); работает ли metrics-server (`kubectl top`); не достигнут ли `maxReplicas`; помещаются ли новые поды в кластер (Pending); не упирается ли нагрузка в БД, а не в CPU пода — тогда добавление реплик не поможет.

</details>

---

## 14. Источники

- [Kubernetes Blog: Ingress NGINX Retirement](https://kubernetes.io/blog/2025/11/11/ingress-nginx-retirement/)
- [Kubernetes Blog: Kubernetes 1.35 — In-Place Pod Resize Graduates to Stable](https://kubernetes.io/blog/2025/12/19/kubernetes-v1-35-in-place-pod-resize-ga/)
- [Kubernetes: Sidecar Containers](https://kubernetes.io/docs/concepts/workloads/pods/sidecar-containers/)
- [Kubernetes: Pod Lifecycle](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/)
- [Kubernetes: Resource Management for Pods and Containers](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)

---

## Приложение: полный манифест сервиса

Deployment с пробами, ресурсами и корректным завершением, Service, HPA, PodDisruptionBudget и маршрут Gateway API. Проверен на корректность YAML.

<details>
<summary>k8s.yaml</summary>

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: orders
spec:
  replicas: 3
  selector:
    matchLabels: { app: orders }
  strategy:
    type: RollingUpdate
    rollingUpdate: { maxUnavailable: 0, maxSurge: 1 }
  template:
    metadata:
      labels: { app: orders }
    spec:
      terminationGracePeriodSeconds: 30
      securityContext:
        runAsNonRoot: true
      containers:
        - name: app
          image: registry.example.com/orders:1.4.2
          ports:
            - containerPort: 8080
          env:
            - name: JAVA_TOOL_OPTIONS
              value: "-XX:MaxRAMPercentage=75"
          envFrom:
            - configMapRef: { name: orders-config }
            - secretRef: { name: orders-secrets }
          resources:
            requests: { cpu: "500m", memory: "768Mi" }
            limits: { memory: "768Mi" }
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
            periodSeconds: 5
            failureThreshold: 30
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8080 }
            periodSeconds: 5
            failureThreshold: 3
          lifecycle:
            preStop:
              exec: { command: ["sh", "-c", "sleep 5"] }
---
apiVersion: v1
kind: Service
metadata:
  name: orders
spec:
  selector: { app: orders }
  ports:
    - port: 80
      targetPort: 8080
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: orders
spec:
  scaleTargetRef: { apiVersion: apps/v1, kind: Deployment, name: orders }
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target: { type: Utilization, averageUtilization: 70 }
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: orders
spec:
  minAvailable: 2
  selector:
    matchLabels: { app: orders }
---
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: orders
spec:
  parentRefs:
    - name: public-gateway
  hostnames: ["shop.example.com"]
  rules:
    - matches:
        - path: { type: PathPrefix, value: /api/orders }
      backendRefs:
        - name: orders
          port: 80
```

Решения в манифесте:

- `limits` только для памяти: CPU limit не задан, чтобы избежать троттлинга; requests нужны планировщику и HPA.
- Startup-проба даёт до 150 секунд (30 × 5 с) на старт JVM, liveness при этом не мешает.
- `preStop` с паузой 5 секунд — чтобы балансировщики успели исключить под до SIGTERM. Требует `sh` в образе; для образов без оболочки — пауза в самом приложении при завершении.
- `maxUnavailable: 0` и PDB с `minAvailable: 2` — мощность не падает ни при выкатке, ни при обслуживании узлов.

</details>
