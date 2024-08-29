  CREATE TABLE queue (
    id String,
    status String
  ) ENGINE = Kafka('persistence-kafka.persistence:9092', 'mail_service_mail_broadcast', 'clickhouse', 'JSONEachRow');

CREATE TABLE queue_dest (
  created_timestamp Nullable(DateTime),
  topic String,
  partition String,
  id String,
  status String
) ENGINE = MergeTree ORDER BY (status);

CREATE MATERIALIZED VIEW queue_mv TO queue_dest AS
SELECT *, _topic as topic, _partition as partition, _timestamp  as created_timestamp
FROM queue;