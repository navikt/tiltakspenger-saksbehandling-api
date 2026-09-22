package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toTiltakDeltakerstatus
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppgavegrunnlagDb.EndretTiltaksdeltakelse::class, name = "ENDRET_TILTAKSDELTAKELSE"),
)
private sealed interface OppgavegrunnlagDb {
    class EndretTiltaksdeltakelse(
        val hendelseId: String,
        val tiltaksdeltakerId: String,
        val eksternDeltakerId: String,
        val deltakelseFraOgMed: LocalDate?,
        val deltakelseTilOgMed: LocalDate?,
        val dagerPerUke: Float?,
        val deltakelsesprosent: Float?,
        val deltakerstatus: String,
    ) : OppgavegrunnlagDb
}

fun Oppgavegrunnlag.toDbJson(): String = serialize(
    when (this) {
        is Oppgavegrunnlag.EndretTiltaksdeltakelse -> OppgavegrunnlagDb.EndretTiltaksdeltakelse(
            hendelseId = hendelseId.toString(),
            tiltaksdeltakerId = tiltaksdeltakerId.toString(),
            eksternDeltakerId = eksternDeltakerId,
            deltakelseFraOgMed = deltakelseFraOgMed,
            deltakelseTilOgMed = deltakelseTilOgMed,
            dagerPerUke = dagerPerUke,
            deltakelsesprosent = deltakelsesprosent,
            deltakerstatus = deltakerstatus.toDb(),
        )
    },
)

fun String.toOppgavegrunnlag(): Oppgavegrunnlag =
    when (val db = deserialize<OppgavegrunnlagDb>(this)) {
        is OppgavegrunnlagDb.EndretTiltaksdeltakelse -> Oppgavegrunnlag.EndretTiltaksdeltakelse(
            hendelseId = TiltaksdeltakerHendelseId.fromString(db.hendelseId),
            tiltaksdeltakerId = TiltaksdeltakerId.fromString(db.tiltaksdeltakerId),
            eksternDeltakerId = db.eksternDeltakerId,
            deltakelseFraOgMed = db.deltakelseFraOgMed,
            deltakelseTilOgMed = db.deltakelseTilOgMed,
            dagerPerUke = db.dagerPerUke,
            deltakelsesprosent = db.deltakelsesprosent,
            deltakerstatus = db.deltakerstatus.toTiltakDeltakerstatus(),
        )
    }
