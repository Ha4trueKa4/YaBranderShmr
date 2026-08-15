CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE vector_examples
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    description TEXT NOT NULL,
    embedding   vector(256) NOT NULL,
    svg_data    TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_vector_examples_embedding ON vector_examples USING hnsw (embedding vector_cosine_ops);