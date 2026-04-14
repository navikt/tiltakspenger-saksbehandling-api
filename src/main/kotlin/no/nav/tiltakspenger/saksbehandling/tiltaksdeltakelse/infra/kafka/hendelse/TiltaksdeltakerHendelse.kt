package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndringer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndringer.Companion.tilEndringer
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 *  [id] Vår interne id for hendelsen
 *  [deltakerId] Id for deltakelsen fra arena/tiltak/komet
 *  [behandlingId] Er satt dersom endringen førte til at det ble automatisk opprettet en revurdering
 * */
data class TiltaksdeltakerHendelse(
    val id: TiltaksdeltakerHendelseId,
    val deltakerId: String,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val deltakerstatus: TiltakDeltakerstatus,
    val sakId: SakId,
    val oppgaveId: OppgaveId?,
    val oppgaveSistSjekket: LocalDateTime?,
    val tiltaksdeltakerId: TiltaksdeltakerId,
    val behandlingId: BehandlingId?,
) {

    fun finnEndringer(
        tiltaksdeltakelseFraBehandling: Tiltaksdeltakelse,
        clock: Clock,
    ): TiltaksdeltakerEndringer? {
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
            return null
        }

        if (erAvbruttDeltakelse(sammeStatus = sammeStatus, sammeTom = sammeTom, tiltaksdeltakelseFraBehandling, clock = clock)) {
            endringer.add(TiltaksdeltakerEndring.AvbruttDeltakelse)
            return endringer.tilEndringer()
        }

        if (erIkkeAktuellDeltakelse(sammeStatus)) {
            endringer.add(TiltaksdeltakerEndring.IkkeAktuellDeltakelse)
            return endringer.tilEndringer()
        }

        if (!sammeDeltakelsesprosent || !sammeAntallDagerPerUke) {
            endringer.add(TiltaksdeltakerEndring.EndretDeltakelsesmengde(deltakelsesprosent, dagerPerUke))
        }

        if (erForlengelse(sammeFom, tiltaksdeltakelseFraBehandling)) {
            endringer.add(TiltaksdeltakerEndring.Forlengelse(deltakelseTilOgMed!!))
            return endringer.tilEndringer()
        }

        if (!sammeFom) {
            endringer.add(TiltaksdeltakerEndring.EndretStartdato(deltakelseFraOgMed))
        }
        if (!sammeTom) {
            endringer.add(TiltaksdeltakerEndring.EndretSluttdato(deltakelseTilOgMed))
        }
        if (!sammeStatus) {
            endringer.add(TiltaksdeltakerEndring.EndretStatus(deltakerstatus))
        }

        return endringer.tilEndringer()
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
