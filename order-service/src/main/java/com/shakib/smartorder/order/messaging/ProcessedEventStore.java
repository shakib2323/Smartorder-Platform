package com.shakib.smartorder.order.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/*
  Remembers which events were already handled, so a message delivered twice is applied once.
  Call it inside the same transaction as the work: the "processed" mark and the business
  change then commit together or roll back together.
 */
@Component
public class ProcessedEventStore {

    private final JdbcTemplate jdbc;

    public ProcessedEventStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /*
      Returns true if this call claimed the event, false if it was already processed.
      The primary key on event_id decides: a second insert of the same id changes nothing,
      and a concurrent duplicate waits for the first transaction to finish.
     */
    public boolean markProcessed(String eventId) {
        return jdbc.update("INSERT IGNORE INTO processed_events (event_id) VALUES (?)", eventId) == 1;
    }
}
