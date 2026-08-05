CREATE TABLE IF NOT EXISTS volunteer_tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS volunteer_signups (
    id BIGSERIAL PRIMARY KEY,
    volunteer_task_id BIGINT NOT NULL REFERENCES volunteer_tasks(id) ON DELETE CASCADE,
    family_member_id BIGINT NOT NULL REFERENCES family_members(id) ON DELETE CASCADE,
    UNIQUE (volunteer_task_id, family_member_id)
);
