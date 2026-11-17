package com.example.habits.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Per-user preferences; shares the user's id (one-to-one, PK = FK to users).
 */
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 1=Monday .. 7=Sunday (ISO day-of-week). SMALLINT column -> short. */
    @Column(name = "week_starts_on", nullable = false)
    private short weekStartsOn = 1;

    @Column(name = "reminders_enabled", nullable = false)
    private boolean remindersEnabled = false;

    public UserSettings() {
    }

    public UserSettings(User user) {
        // userId stays null; @MapsId derives it from the user association on
        // persist. Pre-assigning it would make Spring Data treat the row as
        // detached and issue an UPDATE instead of an INSERT.
        this.user = user;
    }

    public UUID getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (userId == null) {
            this.userId = user.getId();
        }
    }

    public short getWeekStartsOn() {
        return weekStartsOn;
    }

    public void setWeekStartsOn(short weekStartsOn) {
        this.weekStartsOn = weekStartsOn;
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }
}
