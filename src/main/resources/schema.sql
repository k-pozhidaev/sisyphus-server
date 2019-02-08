CREATE TABLE if not exists file
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  mime_type VARCHAR(255) NOT NULL,
  content_length BIGINT NOT NULL,
  content_offset BIGINT NOT NULL,
  last_uploaded_chunk_number BIGINT NOT NULL DEFAULT 0,
  original_name VARCHAR(500),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX if not exists file_id_uindex ON file (id);