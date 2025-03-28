package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
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
    ): Boolean {
        val sammeFom = deltakelseFraOgMed == tiltaksdeltakelseFraBehandling.deltagelseFraOgMed
        val sammeTom = deltakelseTilOgMed == tiltaksdeltakelseFraBehandling.deltagelseTilOgMed

        val sammeAntallDagerPerUke = floatIsEqual(dagerPerUke, tiltaksdeltakelseFraBehandling.antallDagerPerUke)
        val sammeDeltakelsesprosent = floatIsEqual(deltakelsesprosent, tiltaksdeltakelseFraBehandling.deltakelseProsent)
        val sammeStatus = deltakerstatus == tiltaksdeltakelseFraBehandling.deltakelseStatus

        return if (sammeFom && sammeTom && sammeAntallDagerPerUke && sammeDeltakelsesprosent) {
            !(sammeStatus || deltakelsenErAvsluttetSomForventet())
        } else {
            true
        }
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
