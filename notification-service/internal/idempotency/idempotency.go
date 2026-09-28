package idempotency

import (
	"sync"
	"time"
)

type Store interface {
	SeenBefore(eventID string) bool
}

type inMemoryStore struct {
	mu   sync.Mutex
	seen map[string]time.Time
	ttl  time.Duration
}

func NewInMemoryStore(ttl time.Duration) Store {
	s := &inMemoryStore{
		seen: make(map[string]time.Time),
		ttl:  ttl,
	}
	go s.gcLoop()
	return s
}

func (s *inMemoryStore) SeenBefore(eventID string) bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	if ts, ok := s.seen[eventID]; ok && time.Since(ts) < s.ttl {
		return true
	}
	s.seen[eventID] = time.Now()
	return false
}

func (s *inMemoryStore) gcLoop() {
	ticker := time.NewTicker(s.ttl)
	defer ticker.Stop()
	for range ticker.C {
		s.mu.Lock()
		for id, ts := range s.seen {
			if time.Since(ts) > s.ttl {
				delete(s.seen, id)
			}
		}
		s.mu.Unlock()
	}
}