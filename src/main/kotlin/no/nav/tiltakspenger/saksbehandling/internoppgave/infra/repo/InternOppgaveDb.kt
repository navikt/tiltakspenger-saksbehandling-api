package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.Dialoginnlegg
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveGrunnlag
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveløsning
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgavetype
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.Løsningsbegrunnelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import java.time.LocalDateTime

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

    fun tilUtfall(behandlingId: String?): InternOppgaveløsning.Utfall = when (this) {
        FORKASTET -> InternOppgaveløsning.Utfall.Forkastet
        STANS -> revurdering(InternOppgaveløsning.Revurderingstype.STANS, behandlingId)
        FORLENGELSE -> revurdering(InternOppgaveløsning.Revurderingstype.FORLENGELSE, behandlingId)
        OMGJORING -> revurdering(InternOppgaveløsning.Revurderingstype.OMGJØRING, behandlingId)
    }
}

private fun revurdering(type: InternOppgaveløsning.Revurderingstype, behandlingId: String?) =
    InternOppgaveløsning.Utfall.Revurdering(
        type = type,
        // intern_oppgave_løsning krever behandling_id for disse løsningene.
        behandlingId = RammebehandlingId.fromString(behandlingId!!),
    )

fun InternOppgaveløsning.Utfall.toDb(): InternOppgaveløsningDb = when (this) {
    InternOppgaveløsning.Utfall.Forkastet -> InternOppgaveløsningDb.FORKASTET

    is InternOppgaveløsning.Utfall.Revurdering -> when (type) {
        InternOppgaveløsning.Revurderingstype.STANS -> InternOppgaveløsningDb.STANS
        InternOppgaveløsning.Revurderingstype.FORLENGELSE -> InternOppgaveløsningDb.FORLENGELSE
        InternOppgaveløsning.Revurderingstype.OMGJØRING -> InternOppgaveløsningDb.OMGJORING
    }
}

enum class LøsningsbegrunnelseDb {
    ENDRINGEN_ER_ALLEREDE_HANDTERT,
    ENDRINGEN_PAVIRKER_IKKE_RETTEN,
    ;

    fun tilDomene(): Løsningsbegrunnelse.Årsak = when (this) {
        ENDRINGEN_ER_ALLEREDE_HANDTERT -> Løsningsbegrunnelse.Årsak.ENDRINGEN_ER_ALLEREDE_HÅNDTERT
        ENDRINGEN_PAVIRKER_IKKE_RETTEN -> Løsningsbegrunnelse.Årsak.ENDRINGEN_PÅVIRKER_IKKE_RETTEN
    }
}

fun Løsningsbegrunnelse.Årsak.toDb(): LøsningsbegrunnelseDb = when (this) {
    Løsningsbegrunnelse.Årsak.ENDRINGEN_ER_ALLEREDE_HÅNDTERT -> LøsningsbegrunnelseDb.ENDRINGEN_ER_ALLEREDE_HANDTERT
    Løsningsbegrunnelse.Årsak.ENDRINGEN_PÅVIRKER_IKKE_RETTEN -> LøsningsbegrunnelseDb.ENDRINGEN_PAVIRKER_IKKE_RETTEN
}

/** Leser kolonnene `begrunnelse` og `begrunnelse_fritekst`, der databasen sikrer at nøyaktig én er satt på en løst oppgave. */
fun tilLøsningsbegrunnelse(årsak: String?, fritekst: String?): Løsningsbegrunnelse = when {
    årsak != null -> Løsningsbegrunnelse.Forhåndsdefinert(LøsningsbegrunnelseDb.valueOf(årsak).tilDomene())
    else -> Løsningsbegrunnelse.Fritekst(NonBlankString.create(requireNotNull(fritekst) { "En løst oppgave mangler begrunnelse" }))
}

private class DialoginnleggDbJson(
    val saksbehandler: String,
    val tidspunkt: LocalDateTime,
    val tekst: String,
)

private fun Dialoginnlegg.toDb() = DialoginnleggDbJson(saksbehandler, tidspunkt, tekst.value)

fun List<Dialoginnlegg>.toDbJson(): String = serialize(map { it.toDb() })

fun String.tilDialog(): List<Dialoginnlegg> = deserialize<List<DialoginnleggDbJson>>(this).map {
    Dialoginnlegg(it.saksbehandler, it.tidspunkt, NonBlankString.create(it.tekst))
}
