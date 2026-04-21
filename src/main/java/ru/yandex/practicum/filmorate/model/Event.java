package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class Event {
    private Long eventId;
    private Long userId;
    private Long entityId;
    private String eventType;
    private String operation;
    private Long timestamp;
}
