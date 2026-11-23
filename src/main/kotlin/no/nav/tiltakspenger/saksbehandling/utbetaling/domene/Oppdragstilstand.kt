package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import java.time.Duration
import java.time.LocalDateTime

/** Versjonen en sak går gjennom hos Hel Ved, og hvor langt den har kommet i en migrering. */
data class Oppdragstilstand(
    val versjon: Oppdragsversjon,
    val migrering: Oppdragsmigrering?,
    val migrertHosHelVedTidspunkt: LocalDateTime? = null,
) {
    val sperrerUtbetalinger: Boolean get() = migrering?.sperrerUtbetalinger == true

    /** Hel Ved har brukt lengre tid enn forventet på å ta opp de migrerte kjedene, og noen bør se på saken. */
    fun venterForLengePåHelVed(nå: LocalDateTime): Boolean {
        return migrertHosHelVedTidspunkt != null && nå >= migrertHosHelVedTidspunkt.plus(FORVENTET_TID_HOS_HEL_VED)
    }

    /** En utbetaling som er bygget for [versjon], kan bare sendes når saken fortsatt står på den og ikke er sperret. */
    fun kanSende(versjon: Oppdragsversjon): Boolean {
        return this.versjon == versjon && !sperrerUtbetalinger
    }

    companion object {
        val FORVENTET_TID_HOS_HEL_VED: Duration = Duration.ofHours(1)
    }
}
