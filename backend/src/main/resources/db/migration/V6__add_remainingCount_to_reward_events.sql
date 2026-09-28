ALTER TABLE reward_events ADD COLUMN quantity_type VARCHAR(20) NOT NULL DEFAULT 'UNLIMITED';
ALTER TABLE reward_events ADD COLUMN remaining_count INTEGER;