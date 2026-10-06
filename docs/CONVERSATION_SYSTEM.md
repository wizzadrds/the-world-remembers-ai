# Conversation System

## Goal
Turn persistent knowledge and relationships into NPC-to-NPC social behavior while keeping world facts grounded.

## Conversation record
A conversation stores speaker, listener, topic/event identity, tick and whether knowledge was transferred. It is persistent history, not transient chat text.

## Eligibility
NPCs must be alive, nearby and socially eligible. Personality, relationship and stress influence whether they initiate, listen or share information.

## Knowledge transfer
A speaker can transfer only knowledge it possesses. The listener receives REPORTED knowledge from that speaker. If the source itself only knows a rumor, the listener receives RUMORED knowledge with reduced confidence.

## Grounding
The system must never synthesize a witness, location, death, item, relationship or other world fact that does not already exist in simulation state or persistent memory.
