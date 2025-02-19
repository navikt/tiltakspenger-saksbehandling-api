package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository

import no.nav.tiltakspenger.felles.OppgaveId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
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
        nyFlyt: Boolean = true,
    ): Boolean {
        val sammeFom = deltakelseFraOgMed == tiltaksdeltakelseFraBehandling.deltakelsesperiode.fraOgMed
        val sammeTom = deltakelseTilOgMed == tiltaksdeltakelseFraBehandling.deltakelsesperiode.tilOgMed

        val sammeAntallDagerPerUke = floatIsEqual(dagerPerUke, tiltaksdeltakelseFraBehandling.antallDagerPerUke)
        val sammeDeltakelsesprosent = floatIsEqual(deltakelsesprosent, tiltaksdeltakelseFraBehandling.deltakelseProsent)
        val sammeStatus = deltakerstatus == tiltaksdeltakelseFraBehandling.deltakelseStatus

        // TODO: Fjern denne når vi ikke lenger trenger å støtte gammel flyt! Gammel flyt mangler informasjon om
        // dager pr uke og deltakelsesprosent, så vi kan ikke sammenligne med disse
        if (!nyFlyt) {
            return if (sammeFom && sammeTom) {
                !(sammeStatus || deltakelsenErAvsluttetSomForventet())
            } else {
                true
            }
        }

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
