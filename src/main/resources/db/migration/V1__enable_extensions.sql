-- pg_trgm provides trigram similarity: splitting text into three-character
-- chunks and measuring overlap. This is what makes the blocking stage possible,
-- letting Postgres narrow 5,000 reference packages to ~25 candidates before any
-- comparator runs.
CREATE EXTENSION IF NOT EXISTS pg_trgm;