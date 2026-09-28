CREATE TABLE reward_history (
    id BIGSERIAL PRIMARY KEY,
    
    user_id BIGINT NOT NULL,
    reward_event_id BIGINT NOT NULL,

    points BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL,

    idempotency_key UUID NOT NULL UNIQUE,

    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);