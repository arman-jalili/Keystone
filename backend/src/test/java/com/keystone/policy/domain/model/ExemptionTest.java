package com.keystone.policy.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExemptionTest {

    @Test
    void isValid_shouldReturnTrueForActiveNonExpiredExemption() {
        var exemption = new Exemption(
                UUID.randomUUID(), UUID.randomUUID(), "change-1",
                "admin", "Approved for migration",
                Instant.now().plus(7, ChronoUnit.DAYS),
                Instant.now(), null, true);

        assertThat(exemption.isValid()).isTrue();
    }

    @Test
    void isValid_shouldReturnFalseForExpiredExemption() {
        var exemption = new Exemption(
                UUID.randomUUID(), UUID.randomUUID(), "change-1",
                "admin", "Approved",
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now(), null, true);

        assertThat(exemption.isValid()).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForRevokedExemption() {
        var exemption = new Exemption(
                UUID.randomUUID(), UUID.randomUUID(), "change-1",
                "admin", "Approved",
                Instant.now().plus(7, ChronoUnit.DAYS),
                Instant.now(), Instant.now(), true);

        assertThat(exemption.isValid()).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForInactiveExemption() {
        var exemption = new Exemption(
                UUID.randomUUID(), UUID.randomUUID(), "change-1",
                "admin", "Approved",
                Instant.now().plus(7, ChronoUnit.DAYS),
                Instant.now(), null, false);

        assertThat(exemption.isValid()).isFalse();
    }

    @Test
    void isExpired_shouldReturnTrueForPastExpiry() {
        var exemption = new Exemption(
                UUID.randomUUID(), UUID.randomUUID(), "change-1",
                "admin", "Approved",
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now(), null, true);

        assertThat(exemption.isExpired()).isTrue();
    }

    @Test
    void isExpired_shouldReturnFalseForFutureExpiry() {
        var exemption = new Exemption(
                UUID.randomUUID(), UUID.randomUUID(), "change-1",
                "admin", "Approved",
                Instant.now().plus(1, ChronoUnit.HOURS),
                Instant.now(), null, true);

        assertThat(exemption.isExpired()).isFalse();
    }
}
