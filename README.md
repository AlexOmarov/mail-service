# Mail Service

[![Java version](https://img.shields.io/static/v1?label=Java&message=21&color=blue)](https://sonarcloud.io/project/overview?id=AlexOmarov_mail-service)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=AlexOmarov_mail-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=AlexOmarov_mail-service)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=AlexOmarov_mail-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=AlexOmarov_mail-service)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=AlexOmarov_mail-service&metric=bugs)](https://sonarcloud.io/summary/new_code?id=AlexOmarov_mail-service)

Сервис предназначен для формирования, хранения и обработки email сообщений.

## Содержание
- [Введение](#Введение)
- [Развертка](#Развертка)
- [Публикация](#Публикация)
- [Особенности](#Особенности)

### Введение
Проект представляет исходный код сервиса отправки email-сообщений и docker-compose файлы для развертки 
сервиса и окружения.

Процесс импорта проекта в IntellijIDEA не требует дополнительных настроек локальной среды,
проект может быть импортирован в IDE с помощью меню `File->New->Project from existing sources`

### Публикация
Для тестирования будет полезна возможность локальной публикации пакетов в mavenLocal. 
Для этого используется команда publishToMavenLocal.

```bash
.\gradlew publishToMavenLocal
```
В результате выполнения команды в .m2 папке пользователя появится артефакт mail-service-api, который будет содержать все
необходимые dto-классы, proto-файлы и другие нужные для взаимодействия с сервисом интерфейсы.

### Развертка
Сервис может разворачиваться как в докере, так и локально. Для развертки необходимо использовать инструменты 
указанных версий:
* [Java 20 SDK](https://openjdk.org/projects/jdk/20/)
* [Gradle >= 8.3](https://gradle.org/install/)

Системные переменные, необходимые для корректной работы микросервиса перечислены в разделе [Особенности](#Особенности).

В стандартной конфигурации сервис держит открытым для http соединений порт 8080 и порт 9090 для grpc соединений.
Настройку можно изменить, добавив `contour.grpc-port` и `contour.http.port` системные переменные.

Сервис поддерживает развёртку нескольких экземпляров с одной и той же базой данных и кэшем.

`Liveness` и `readiness` API доступны по `actuator/health/liveness` и `actuator/health/readiness` путям.
Уровень логирования можно менять через системную переменную `logging.level.root` (info, debug, etc.).
Если нет необходимости смотреть ВСЕ debug логи, можно ограничить debug уровень конкретными пакетами,
установив переменную `logging.level.<PACKAGE_TO_LOG>`.

Метрики для Prometheus доступны по адресу `actuator/prometheus`.

#### Локальная развертка
Необходимо установить системную переменную spring.profiles.active=dev.
Можно разворачивать либо конфигурацией из IntellijIDEA, либо вручную командой в терминале
```bash
.\gradlew bootRun --args='--spring.profiles.active=dev'
```

#### Локальный Docker
Необходимо использовать `docker-compose-local.yml` чтобы собрать образ и стартовать контейнер.
В стандартной конфигурации сервис будет использовать `application-dev.yml` файл свойств.
Following ports are served on host machine

8080 - service (http)
9091 - service (grpc)
7001 - service (rsocket)

7000 - postgres (odbc) (нужно добавить в dbeaver)
4002 - pgadmin (http) (нужно ввести пароль)

2181 - zookeeper (tcp)
29092 - kafka cluster (tcp) - had to 2 frst because clash with kafka advertised listeners
6005 - schema registry (tcp/http)
9001 - kafka ui (http)

9411 - zipkin (http/tcp)
3000 - grafana (http/tcp)
9090 - prometheus (http/tcp)

25826, 5046 - logstash (tcp)
9200 - elastic (tcp/http)
5601 - kibana (http)

5001 - mockserver (http)

6379 - redis (tcp) (can be seen via this https://github.com/qishibo/AnotherRedisDesktopManager/releases)
2500 - slurper (tcp)
8085 - slurper (tcp)
8083 - slurper (http)

### Использование code-quality инструментов
Когда проект собирается с использованием `build` задачи gradle detekt и ktlint проверки проходят автоматически,
и detekt xml отчет формируется по путям `./build/app/reports/detekt`, `./build/api/reports/detekt`.
Также есть возможность запускать проверки вручную командой
```bash
.\gradlew detekt
```

Тестирование и измерение покрытия также проходят автоматически при вызове команды `build`. Покрытие измеряется
инструментом kover, который в свою очередь использует движок JaCoCo.
Отчеты по покрытию (xml для sonar-инструментов и html для локальной разработки)
формируются по путям `./build/app/reports/kover/report.xml`, `./build/app/reports/kover/html/index.html`.
Также есть возможность вызвать тестирование и измерение покрытия вручную, вызвав команду
```bash
.\gradlew test
```
Процент покрытия также можно смотреть и в самой IDE.
Для этого достаточно правой кнопкой мыши кликнуть на папку test и запустить задачу Run with Coverage.

Прохождение Quality Gate реализовано с использованием gradle плагина sonarqube. Вызвать прохождение можно командами:
```bash
.\gradlew build
.\gradlew sonar -D"sonar.host.url"="<SONAR_HOST>" -D"sonar.token"="YOUR_TOKEN" -D"sonar.projectKey"="KEY" -D"sonar.organization"="ORG"
```

Пример:
```bash
.\gradlew build
.\gradlew sonar -D"sonar.host.url"="http://localhost:4001" -D"sonar.token"="sqp_<REST_OF_THE_TOKEN>" -D"sonar.projectKey"="appeal-service" -D"sonar.organization"="ORG"
```

При вызове sonar с помощью gradle задачи сгенерированный detekt отчет и kover отчет будут добавлен к анализу.
В идеале sonar задача должна вызываться на каждый коммит в master, и использоваться должен не sonarcloud или локально
поднятый в докере на машине разработчика sonarqube, а общий для всей среды разработки. Но такого пока нет.

### Особенности
* Постоянное подключение к базе Postgres
* Конфигурация через системные переменные, [application-файлы](mail-service-app/src/main/resources)
* Публикация api модуля отдельным пакетом в maven repository (внутренний)

Сервис использует в своей работе несколько сторонних систем:
- Postgres база (v. 15 и выше). Стандартные конфиги (с именами системных переменных):
  - host: ${contour.database.host:localhost}
  - port: ${contour.database.port:5432}
  - name: ${contour.database.name:mail_service}
  - schema: ${contour.database.schema:mail_service}
  - user: ${contour.database.user:mail_service}
  - password: ${contour.database.password:mail_service}

Также имеется возможность настроить логирование:
- LOGSTASH_HOST (true/false) - включает json appender, который используется для импорта логов в elk стэк