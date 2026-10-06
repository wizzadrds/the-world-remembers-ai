# Phase 10 Design

Dream generation is a deterministic simulation layer over existing memories. It does not call an external LLM and does not create persistent facts.

A dream records sleeper UUID, category, source identifier, start tick and bounded duration. Category selection uses remembered importance, fear/stress, positive history and relationship context. Impossible dreams change presentation rules only; their source remains a real memory.
