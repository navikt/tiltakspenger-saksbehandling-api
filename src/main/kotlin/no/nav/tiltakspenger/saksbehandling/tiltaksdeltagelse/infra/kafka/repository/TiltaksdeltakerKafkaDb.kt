package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.jobb.TiltaksdeltakerEndring
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
) {
    fun tiltaksdeltakelseErEndret(
        tiltaksdeltakelseFraBehandling: Tiltaksdeltagelse,
    ): List<TiltaksdeltakerEndring> {
        val endringer = mutableListOf<TiltaksdeltakerEndring>()
        val sammeFom = deltakelseFraOgMed == tiltaksdeltakelseFraBehandling.deltagelseFraOgMed
        val sammeTom = deltakelseTilOgMed == tiltaksdeltakelseFraBehandling.deltagelseTilOgMed

        val sammeAntallDagerPerUke = floatIsEqual(dagerPerUke, tiltaksdeltakelseFraBehandling.antallDagerPerUke)
        val sammeDeltakelsesprosent = floatIsEqual(deltakelsesprosent, tiltaksdeltakelseFraBehandling.deltakelseProsent)
        val sammeStatus = deltakerstatus == tiltaksdeltakelseFraBehandling.deltakelseStatus

        if (sammeFom &&
            sammeTom &&
            sammeAntallDagerPerUke &&
            sammeDeltakelsesprosent &&
            (sammeStatus || deltakelsenErAvsluttetSomForventet())
        ) {
            return emptyList()
        }

        if (!sammeDeltakelsesprosent || !sammeAntallDagerPerUke) {
            endringer.add(TiltaksdeltakerEndring.ENDRET_DELTAKELSESMENGDE)
        }

        if (erForlengelse(sammeFom, tiltaksdeltakelseFraBehandling)) {
            endringer.add(TiltaksdeltakerEndring.FORLENGELSE)
            return endringer
        }

        if (erAvbruttDeltakelse(sammeStatus = sammeStatus, sammeTom = sammeTom, tiltaksdeltakelseFraBehandling)) {
            endringer.add(TiltaksdeltakerEndring.AVBRUTT_DELTAKELSE)
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

    private fun erForlengelse(sammeFom: Boolean, tiltaksdeltakelseFraBehandling: Tiltaksdeltagelse): Boolean =
        sammeFom && deltakelseTilOgMed?.isAfter(tiltaksdeltakelseFraBehandling.deltagelseTilOgMed) == true

    private fun erAvbruttDeltakelse(
        sammeStatus: Boolean,
        sammeTom: Boolean,
        tiltaksdeltakelseFraBehandling: Tiltaksdeltagelse,
    ): Boolean {
        if (!sammeStatus && deltakerstatus == TiltakDeltakerstatus.Avbrutt) {
            return true
        }
        if (!sammeTom &&
            deltakelseTilOgMed != null &&
            deltakelseTilOgMed.isBefore(tiltaksdeltakelseFraBehandling.deltagelseTilOgMed) &&
            !deltakelseTilOgMed.isAfter(LocalDate.now())
        ) {
            return true
        }
        return false
    }

    private fun deltakelsenErAvsluttetSomForventet(): Boolean {
        return (
            (deltakerstatus == TiltakDeltakerstatus.HarSluttet || deltakerstatus == TiltakDeltakerstatus.Fullført) &&
                deltakelseTilOgMed != null &&
                !deltakelseTilOgMed.isAfter(LocalDate.now())
            )
    }

    private fun floatIsEqual(a: Float?, b: Float?): Boolean {
        return if (a == null && b == 0F) {
            true
        } else if (b == null && a == 0F) {
            true
        } else {
            return compareValues(a, b) == 0
        }
    }
}
