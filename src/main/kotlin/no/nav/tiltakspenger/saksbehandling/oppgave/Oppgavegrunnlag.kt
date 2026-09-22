package no.nav.tiltakspenger.saksbehandling.oppgave

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import java.time.LocalDate

sealed interface Oppgavegrunnlag {
    data class EndretTiltaksdeltakelse(
        val kilde: Kilde,
        val tiltaksdeltakerId: TiltaksdeltakerId,
        val eksternDeltakerId: String,
        val deltakelseFraOgMed: LocalDate?,
        val deltakelseTilOgMed: LocalDate?,
        val dagerPerUke: Float?,
        val deltakelsesprosent: Float?,
        val deltakerstatus: TiltakDeltakerstatus,
    ) : Oppgavegrunnlag {
        /** Kildespesifikke referanser holdes adskilt fra øyeblikksbildet av deltakelsen. */
        sealed interface Kilde {
            data class Kafka(val hendelseId: TiltaksdeltakerHendelseId) : Kilde
        }
    }
}
