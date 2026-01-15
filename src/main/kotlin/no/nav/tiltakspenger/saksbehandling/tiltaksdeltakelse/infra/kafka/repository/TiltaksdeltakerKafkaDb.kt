package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndring
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class TiltaksdeltakerKafkaDb(
    val id: String,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val deltakerstatus: TiltakDeltakerstatus,
    val sakId: SakId,
    val oppgaveId: OppgaveId?,
    val oppgaveSistSjekket: LocalDateTime?,
    // kan gjøres påkrevd når ingen innslag i databasen mangler denne
    val tiltaksdeltakerId: TiltaksdeltakerId?,
) {
    fun tiltaksdeltakelseErEndret(
        tiltaksdeltakelseFraBehandling: Tiltaksdeltakelse,
        clock: Clock,
    ): List<TiltaksdeltakerEndring> {
        val endringer = mutableListOf<TiltaksdeltakerEndring>()
        val sammeFom = deltakelseFraOgMed == tiltaksdeltakelseFraBehandling.deltakelseFraOgMed
        val sammeTom = deltakelseTilOgMed == tiltaksdeltakelseFraBehandling.deltakelseTilOgMed

        val sammeAntallDagerPerUke = floatIsEqual(dagerPerUke, tiltaksdeltakelseFraBehandling.antallDagerPerUke)
        val sammeDeltakelsesprosent = floatIsEqual(deltakelsesprosent, tiltaksdeltakelseFraBehandling.deltakelseProsent)
        val sammeStatus = deltakerstatus == tiltaksdeltakelseFraBehandling.deltakelseStatus

        if (sammeFom &&
            sammeTom &&
            sammeAntallDagerPerUke &&
            sammeDeltakelsesprosent &&
            (sammeStatus || deltakelsenErAvsluttetSomForventet(clock = clock))
        ) {
            return emptyList()
        }

        if (erAvbruttDeltakelse(sammeStatus = sammeStatus, sammeTom = sammeTom, tiltaksdeltakelseFraBehandling, clock = clock)) {
            endringer.add(TiltaksdeltakerEndring.AVBRUTT_DELTAKELSE)
            return endringer
        }

        if (erIkkeAktuellDeltakelse(sammeStatus)) {
            endringer.add(TiltaksdeltakerEndring.IKKE_AKTUELL_DELTAKELSE)
            return endringer
        }

        if (!sammeDeltakelsesprosent || !sammeAntallDagerPerUke) {
            endringer.add(TiltaksdeltakerEndring.ENDRET_DELTAKELSESMENGDE)
        }

        if (erForlengelse(sammeFom, tiltaksdeltakelseFraBehandling)) {
            endringer.add(TiltaksdeltakerEndring.FORLENGELSE)
            return endringer
        }

        if (!sammeFom) {
            endringer.add(TiltaksdeltakerEndring.ENDRET_STARTDATO)
        }
        if (!sammeTom) {
            endringer.add(TiltaksdeltakerEndring.ENDRET_SLUTTDATO)
        }
        if (!sammeStatus) {
            endringer.add(TiltaksdeltakerEndring.ENDRET_STATUS)
        }

        return endringer
    }

    private fun erForlengelse(sammeFom: Boolean, tiltaksdeltakelseFraBehandling: Tiltaksdeltakelse): Boolean =
        sammeFom && deltakelseTilOgMed?.isAfter(tiltaksdeltakelseFraBehandling.deltakelseTilOgMed) == true

    private fun erAvbruttDeltakelse(
        sammeStatus: Boolean,
        sammeTom: Boolean,
        tiltaksdeltakelseFraBehandling: Tiltaksdeltakelse,
        clock: Clock,
    ): Boolean {
        if (!sammeStatus && deltakerstatus == TiltakDeltakerstatus.Avbrutt) {
            return true
        }
        if (!sammeTom &&
            deltakelseTilOgMed != null &&
            deltakelseTilOgMed.isBefore(tiltaksdeltakelseFraBehandling.deltakelseTilOgMed) &&
            !deltakelseTilOgMed.isAfter(LocalDate.now(clock))
        ) {
            return true
        }
        return false
    }

    private fun erIkkeAktuellDeltakelse(
        sammeStatus: Boolean,
    ): Boolean {
        return !sammeStatus && deltakerstatus == TiltakDeltakerstatus.IkkeAktuell
    }

    private fun deltakelsenErAvsluttetSomForventet(clock: Clock): Boolean {
        return (
            (deltakerstatus == TiltakDeltakerstatus.HarSluttet || deltakerstatus == TiltakDeltakerstatus.Fullført) &&
                deltakelseTilOgMed != null &&
                !deltakelseTilOgMed.isAfter(LocalDate.now(clock))
            )
    }

    private fun floatIsEqual(a: Float?, b: Float?): Boolean {
        return if (a == null && b == 0F) {
            true
        } else if (b == null && a == 0F) {
            true
        } else {
            compareValues(a, b) == 0
        }
    }
}
