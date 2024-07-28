  CREATE TABLE mail_service.queue (
    id String,
    status String
  ) ENGINE = Kafka('persistence-kafka.persistence:9092', 'mail_service_mail_broadcast', 'clickhouse', 'JSONEachRow');

CREATE TABLE mail_service.queue_dest (
  created_timestamp Nullable(DateTime),
  topic String,
  partition String,
  id String,
  status String
) ENGINE = MergeTree ORDER BY (status);

CREATE MATERIALIZED VIEW mail_service.queue_mv TO mail_service.queue_dest AS
SELECT *, _topic as topic, _partition as partition, _timestamp  as created_timestamp
FROM mail_service.queue;