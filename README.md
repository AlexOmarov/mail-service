# Mail Service

[![Java version](https://img.shields.io/static/v1?label=Java&message=21&color=blue)](https://sonarcloud.io/project/overview?id=AlexOmarov_mail-service)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=AlexOmarov_mail-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=AlexOmarov_mail-service)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=AlexOmarov_mail-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=AlexOmarov_mail-service)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=AlexOmarov_mail-service&metric=bugs)](https://sonarcloud.io/summary/new_code?id=AlexOmarov_mail-service)

Web-сервис, содержащий в себе простой бизнес-сценарий и примеры использования различных технологических инструментов, подходов к проектированию

## Содержание
- [Введение](#Введение)
- [Используемые инструменты](#Используемые-инструменты)
- [Техническое наполнение](#Техническое-наполнение)
- [Развертка](#Развертка)
- [Использование code-quality инструментов](#Использование-code-quality-инструментов)
- [Публикация](#Публикация)

### Введение
Проект содержит исходный код и docker-compose файл для развертки сервиса и окружения.

Основная функция сервиса - принимать из различных источников обьект с полями email/text, заполнять по нему шаблон email-письма и по планировщику отправлять на smtp сервер. Сервис имеет 2 операции, которые могут быть инициированы клиентами - создание письма и получение письма. Отправка письма на smtp сервер осуществляется асинхронно, по планировщику.

Процесс импорта проекта в IntellijIDEA не требует дополнительных настроек локальной среды, проект может быть импортирован в IDE с помощью меню `File->New->Project from existing sources->Gradle`.

### Используемые инструменты

Сервис построен на фреймворках, поддерживающих [Reactive Streams](https://www.reactive-streams.org/) спецификацию:
- [WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html) - основной фреймворк сервиса, позволяющий запускать web-приложение в рамках веб-сервера Netty, который поддерживает реактивный способ обработки http-запросов. Также используются несколько дополнительных фрейворков экосистемы Spring, такие как Security, r2dbc-starter и т.д.
- [Kotlin coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - библиотека расширения синтаксиса kotlin-языка, позволяющая использовать те же реактивные подходы, что и Project Reactor фреймворк (на котором построен WebFlux), при этом используя последовательный, императивный стиль программирования, не используя функциональный подход типов Mono/Flux.
- [Reactor Kafka](https://github.com/reactor/reactor-kafka) - драйвер для работы с брокером сообщений Kafka, использующий реактивный подход к обработке потребления и отправке событий. Позволяет не блокировать потоки в пуле, отвечающем за потребление и отправку сообщений до конца выполнения операции.
- [Reactive Redis](https://developer.redis.com/develop/java/spring/rate-limiting/fixed-window/reactive/) - реактивный драйвер для работы с распределенным кэш-хранилищем Redis.
- [Reactive Postgres](https://github.com/pgjdbc/r2dbc-postgresql) - реактивный драйвер для работы с СУБД Postgres.
- [Spring Grpc](https://github.com/grpc-ecosystem/grpc-spring) - фреймворк, который при использовании библиотеки [Grpc Kotlin](https://github.com/grpc/grpc-kotlin) позволяет получить базовый функционал grpc-сервера с "нативной" поддержкой реактивного стэка через suspend функции.

Также, для отправки email-сообщений используется фреймворк [Spring Mail](https://docs.spring.io/spring-framework/reference/integration/email.html), который основан на Jakarta Mail библиотеке и не поддерживает реактивность изначально. Для работы с этим фреймворком в неблокирующей среде используется динамический пул потоков Dispatchers.IO, который выделен библиотекой Kotlin Coroutines специально для подобных случаев. В перспективе этот пул можно будет заменить на виртуальные потоки из [Project Loom](https://wiki.openjdk.org/display/loom/Main).

Полный список используемых библиотек можно найти в файле [libs.versions.toml](gradle/libs.versions.toml)
### Техническое наполнение

Сервис имеет следующие технические возможности:
1. Обе операции, поддерживаемые сервисом представлены в виде 3 типов API - rsocket, http и grpc
2. Поддерживается создание письма с помощью отправки сообщения в соответствующую очередь в Kafka, также по изменению статуса сообщения (при создании или отправке) сервис отправляет сообщение в другую очередь.
3. Для rsocket/http настроена basic авторизация с использованием Spring Security фреймворка (grpc пока реактивную авторизацию не поддерживает)
4. При взаимодействии по RSocket протоколу используется бинарный формат данных [Hessian](http://hessian.caucho.com/). Это позволяет уменьшить размер передаваемых по сети данных, но накладывает на сервис дополнительную ответственность по десериализации обьектов не из сырых строк (будь то xml или json), а из специального формата. В перспективе неплохо заменяется [CBOR](https://cbor.io/)
5. Настроен стэк мониторинга приложения - генерация метрик, логов и трейсов. Метрики доступны по http-эндпоинту, трейсы и логи периодически отправляются самим сервисом в системы мониторинга. Далее про это описано более подробно.
6. Настроено jmx - подключение внутри docker-контейнера.
7. Реализовано взаимодействие с персистентным хранилищем (Postgres), работа со схемой базы происходит с помощью Flyway-миграций.
8. Реализовано использование распределенного кэша Redis для хранения созданных сообщений.
9. Реализован распределенный планировщик, поддерживающий горизонтальное масштабирование с помощью библиотеки [ShedLock](https://github.com/lukas-krecan/ShedLock)
10. Для имитации работы с сервисом добавлен планировщик [LoadScheduler](mail-service-app/src/main/kotlin/ru/somarov/mail/application/scheduler/LoadScheduler.kt), который периодически отправляет запросы на API сервиса и сообщения в прослушиваемую очередь.  
11. Для описания http-спецификаций добавлена библиотека openapi
При наличии гейтвея и достаточного количества партиций в kafka-очередях сервис поддерживает горизонтальное масштабирование и может использоваться в рамках kubernetes-кластера.

Кроме того, есть несколько особенностей github-репозитория:
1. Настроено автоматическое обновление версии сервиса в gradle.properties файле по созданию релиза
2. Настроено автоматическое прохождение проверок качества (синтаксический анализ кода, тесты, более обширный статический sonar-анализ)
3. Настроена автоматическая сборка и выкладка docker-образа по созданию нового релиза в github.
   Результаты тестов и анализа кода доступны в [Sonarcloud](https://sonarcloud.io/project/overview?id=AlexOmarov_mail-service), docker-образ доступен в [Docker registry](https://hub.docker.com/repository/docker/decentboat/mail-service/general)

### Развертка
Наиболее удобно развернуть сервис и всё необходимое для демонстрации технических возможностей окружение используя Docker.
#### Развёртка с использованием Docker
В корне проекта находится [compose](docker-compose.yml) файл, который при наличии на локально машине [Docker](https://www.docker.com/) можно запустить либо используя IDE (к примеру Docker плагин для IJIdea), либо из консоли следующей командой
```bash
docker-compose up -d
```

Команду необходимо исполнять находясь в папке с файлом docker-compose.yml.

Compose файл включает в себя следующие инструменты, каждый из которых доступен для подключения по localhost:
1. Postgres - localhost:7000
2. PGAdmin - localhost:4002
3. Zookeeper - localhost:2181
4. Kafka - localhost:29092
5. [Kafka-UI](https://github.com/provectus/kafka-ui) - localhost:9001
6. [Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html) (будет использоваться в будущем)  - localhost:6005
7. [ELK Stack](https://www.elastic.co/elastic-stack) (без Beats) - localhost:9200(elastic), localhost:5601(kibana), localhost:25826(logstash), localhost:5046(logstash)
8. Grafana - localhost:3000
9. Prometheus - localhost:9090
10. Zipkin - localhost:9411
11. Redis - localhost:6379
12. [MailSlurper](https://www.mailslurper.com/) (smtp сервер, на который отправляются сообщения) - localhost:2500/8085/8083
13. Mock server (http сервер с заглушкой по адресу GET /wheather, на текущий момент не используется) - localhost:5001
14. Initializer - системный контейнер, который инициализирует data view внутри Kibana и завершает свое выполнение
15. Сам сервис - localhost:8080, localhost:9091, localhost:7001, localhost:9010

**Важно!** Запуск потребует скачивания недостающих образов для запуска всех контейнеров и 8 ГБ оперативной памяти в процессе работы развёртки.
Кроме того, вышеперечисленные порты должны быть свободны для корректной развёртки всего окружения. Перечислим их ещё раз одним списком:
```
7000, 4002, 2181, 29092, 9001, 6005, 9200, 5601, 25826, 5046, 3000, 9090, 9411, 6379, 2500, 8085, 8083, 5001, 8080, 9091, 7001, 9010
```
#### Локальная развертка
Сервис может разворачиваться и локально, с использованием собранного исходного кода вместо docker-образа.
Для этого необходимо изначально провести развертку с использованием Docker compose файла, после этого удалить контейнер с сервисом (service), собрать проект и запустить его либо через IDE, либо внеся изменения в docker compose файл - раскомментировать строки build/context контейнера service и убрать настройку image. Для запуска через IDE (например для проведения debug-а) необходимо прописать системную переменную `contour.database.port=7000`

#### Использование развёрнутых систем

https://github.com/qishibo/AnotherRedisDesktopManager/releases
`Liveness` и `readiness` API доступны по `actuator/health/liveness` и `actuator/health/readiness` путям.  
Метрики для Prometheus доступны по адресу `actuator/prometheus`.
![smtp.jpg](doc%2Fimg%2Fsmtp.jpg)
![kibana.jpg](doc%2Fimg%2Fkibana.jpg)
![redis.jpg](doc%2Fimg%2Fredis.jpg)
### Использование code-quality инструментов
Когда проект собирается с использованием `build` задачи gradle detekt и ktlint проверки проходят автоматически. Detekt xml отчет формируется по путям `./build/app/reports/detekt`, `./build/api/reports/detekt`.  
Также есть возможность запускать проверки вручную командой
```bash  
.\gradlew detekt
```  

Тестирование и измерение покрытия также проходят автоматически при вызове команды `build`. Покрытие измеряется инструментом kover, который в свою очередь использует движок JaCoCo.  
Отчеты по покрытию (xml для sonar-инструментов и html для локальной разработки)  
формируются по путям `./build/app/reports/kover/report.xml`, `./build/app/reports/kover/html/index.html`.  
Также есть возможность вызвать тестирование и измерение покрытия вручную, вызвав команду
```bash  
.\gradlew test koverPrintCoverage
```  
Процент покрытия также можно смотреть и в самой IDE.  
Для этого достаточно правой кнопкой мыши кликнуть на папку test и запустить задачу Run with Coverage.  
**Важно!** Тесты не пройдут, если одновременно в докере развернут compose!  

Прохождение Quality Gate реализовано с использованием gradle плагина sonarqube. Вызвать прохождение можно командами:
```bash  
.\gradlew build
.\gradlew sonar -D"sonar.host.url"="<SONAR_HOST>" -D"sonar.token"="YOUR_TOKEN" -D"sonar.projectKey"="KEY" -D"sonar.organization"="ORG"
```  

При вызове sonar с помощью gradle задачи сгенерированный detekt отчет и kover отчет будут добавлен к анализу.

### Публикация
Для тестирования будет полезна возможность локальной публикации пакетов в mavenLocal.  
Для этого используется команда publishToMavenLocal.

```bash  
.\gradlew publishToMavenLocal
```  
В результате выполнения команды в .m2 папке пользователя появится артефакт mail-service-api, который будет содержать все  
необходимые dto-классы, proto-файлы и другие нужные для взаимодействия с сервисом интерфейсы.  