CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    status VARCHAR(40) NOT NULL,
    due_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_tasks_user_id ON tasks (user_id);
CREATE INDEX idx_tasks_user_id_status ON tasks (user_id, status);
