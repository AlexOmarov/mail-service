CREATE TABLE mail (
   id uuid NOT NULL,
   mail_status_id uuid NOT NULL,
   mail_channel_id uuid not null,
   client_email varchar(512),
   text text NOT NULL,
   creation_date timestamptz NOT NULL,
   last_update_date timestamptz NOT NULL,
   CONSTRAINT mail_pk PRIMARY KEY (id)
);

CREATE TABLE mail_status (
  id uuid NOT NULL,
  code varchar(512) NOT NULL,
  CONSTRAINT conversion_status_pk PRIMARY KEY (id)
);

CREATE TABLE mail_channel (
  id uuid NOT NULL,
  code varchar(512) NOT NULL,
  CONSTRAINT mail_channel_pk PRIMARY KEY (id)
);

ALTER TABLE mail ADD CONSTRAINT mail_status_fk FOREIGN KEY (mail_status_id)
    REFERENCES mail_status (id) MATCH FULL
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE mail ADD CONSTRAINT mail_channel_fk FOREIGN KEY (mail_channel_id)
    REFERENCES mail_channel (id) MATCH FULL
    ON DELETE RESTRICT ON UPDATE CASCADE;

INSERT INTO mail_channel(id, code) values
('1e6a74e6-baa3-4185-8cbe-b5ce7061db52', 'MOBILE');

INSERT INTO mail_status(id, code) values
('0b35681b-fcc5-4140-adf1-4ff0da2acfac', 'NEW'),
('a453c3a3-394f-452c-bc07-b2dfdb6bc12e', 'SENT');
