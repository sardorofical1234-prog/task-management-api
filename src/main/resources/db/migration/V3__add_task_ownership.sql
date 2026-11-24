-- Existing tasks are retained with a NULL owner so historical data is not silently
-- assigned to an arbitrary account. ADMIN users can still inspect and manage these rows.
ALTER TABLE tasks
    ADD COLUMN owner_id BIGINT;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_owner
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE RESTRICT;

CREATE INDEX idx_tasks_owner_created_at
    ON tasks (owner_id, created_at DESC);
