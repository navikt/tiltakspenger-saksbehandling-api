package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveGrunnlag
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveløsning
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgavetype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId

enum class InternOppgavetypeDb {
    ENDRET_TILTAKSDELTAKELSE,
}

fun InternOppgavetype.toDb(): InternOppgavetypeDb = when (this) {
    InternOppgavetype.ENDRET_TILTAKSDELTAKELSE -> InternOppgavetypeDb.ENDRET_TILTAKSDELTAKELSE
}

private class EndretTiltaksdeltakelseDbJson(
    val hendelseId: String,
    val beskrivelse: String,
)

fun InternOppgaveGrunnlag.toDbJson(): String = when (this) {
    is InternOppgaveGrunnlag.EndretTiltaksdeltakelse -> serialize(
        EndretTiltaksdeltakelseDbJson(hendelseId.toString(), beskrivelse),
    )
}

fun InternOppgavetypeDb.tilGrunnlag(nøkkel: String, json: String): InternOppgaveGrunnlag = when (this) {
    InternOppgavetypeDb.ENDRET_TILTAKSDELTAKELSE -> deserialize<EndretTiltaksdeltakelseDbJson>(json).let {
        InternOppgaveGrunnlag.EndretTiltaksdeltakelse(
            tiltaksdeltakerId = TiltaksdeltakerId.fromString(nøkkel),
            hendelseId = TiltaksdeltakerHendelseId.fromString(it.hendelseId),
            beskrivelse = it.beskrivelse,
        )
    }
}

enum class InternOppgaveløsningDb {
    FORKASTET,
    STANS,
    FORLENGELSE,
    OMGJORING,
    ;

    fun tilDomene(behandlingId: String?): InternOppgaveløsning = when (this) {
        FORKASTET -> InternOppgaveløsning.Forkastet
        STANS -> revurdering(InternOppgaveløsning.Revurderingstype.STANS, behandlingId)
        FORLENGELSE -> revurdering(InternOppgaveløsning.Revurderingstype.FORLENGELSE, behandlingId)
        OMGJORING -> revurdering(InternOppgaveløsning.Revurderingstype.OMGJØRING, behandlingId)
    }
}

private fun revurdering(type: InternOppgaveløsning.Revurderingstype, behandlingId: String?) =
    InternOppgaveløsning.Revurdering(
        type = type,
        // intern_oppgave_losning krever behandling_id for disse løsningene.
        behandlingId = RammebehandlingId.fromString(behandlingId!!),
    )

fun InternOppgaveløsning.toDb(): InternOppgaveløsningDb = when (this) {
    InternOppgaveløsning.Forkastet -> InternOppgaveløsningDb.FORKASTET

    is InternOppgaveløsning.Revurdering -> when (type) {
        InternOppgaveløsning.Revurderingstype.STANS -> InternOppgaveløsningDb.STANS
        InternOppgaveløsning.Revurderingstype.FORLENGELSE -> InternOppgaveløsningDb.FORLENGELSE
        InternOppgaveløsning.Revurderingstype.OMGJØRING -> InternOppgaveløsningDb.OMGJORING
    }
}
