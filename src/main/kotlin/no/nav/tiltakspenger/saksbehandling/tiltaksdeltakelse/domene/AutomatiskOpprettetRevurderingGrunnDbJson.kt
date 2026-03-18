package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndringer
import java.time.LocalDate

private data class AutomatiskOpprettetRevurderingGrunnDbJson(
    val hendelseId: String,
    val endringer: List<TiltaksdeltakerEndringDbJson>,
)

private data class TiltaksdeltakerEndringDbJson(
    val type: TiltaksdeltakerEndringTypeDb,
    val nySluttdato: String? = null,
    val nyStartdato: String? = null,
    val nyDeltakelsesprosent: Float? = null,
    val nyDagerPerUke: Float? = null,
    val nyStatus: String? = null,
)

private enum class TiltaksdeltakerEndringTypeDb {
    AVBRUTT_DELTAKELSE,
    IKKE_AKTUELL_DELTAKELSE,
    FORLENGELSE,
    ENDRET_SLUTTDATO,
    ENDRET_STARTDATO,
    ENDRET_DELTAKELSESMENGDE,
    ENDRET_STATUS,
}

fun AutomatiskOpprettetRevurderingGrunn.toDbJson(): String {
    return serialize(
        AutomatiskOpprettetRevurderingGrunnDbJson(
            hendelseId = hendelseId,
            endringer = endringer.map { it.toDbJson() },
        ),
    )
}

fun String.toAutomatiskOpprettetRevurderingGrunn(): AutomatiskOpprettetRevurderingGrunn {
    val dbJson = deserialize<AutomatiskOpprettetRevurderingGrunnDbJson>(this)
    return AutomatiskOpprettetRevurderingGrunn(
        hendelseId = dbJson.hendelseId,
        endringer = TiltaksdeltakerEndringer(
            dbJson.endringer.map { it.toDomain() }.toNonEmptyListOrNull()!!,
        ),
    )
}

private fun TiltaksdeltakerEndring.toDbJson(): TiltaksdeltakerEndringDbJson = when (this) {
    is TiltaksdeltakerEndring.AvbruttDeltakelse -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.AVBRUTT_DELTAKELSE)
    is TiltaksdeltakerEndring.IkkeAktuellDeltakelse -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.IKKE_AKTUELL_DELTAKELSE)
    is TiltaksdeltakerEndring.Forlengelse -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.FORLENGELSE, nySluttdato = nySluttdato.toString())
    is TiltaksdeltakerEndring.EndretSluttdato -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_SLUTTDATO, nySluttdato = nySluttdato?.toString())
    is TiltaksdeltakerEndring.EndretStartdato -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_STARTDATO, nyStartdato = nyStartdato?.toString())
    is TiltaksdeltakerEndring.EndretDeltakelsesmengde -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_DELTAKELSESMENGDE, nyDeltakelsesprosent = nyDeltakelsesprosent, nyDagerPerUke = nyDagerPerUke)
    is TiltaksdeltakerEndring.EndretStatus -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_STATUS, nyStatus = nyStatus.name)
}

private fun TiltaksdeltakerEndringDbJson.toDomain(): TiltaksdeltakerEndring = when (type) {
    TiltaksdeltakerEndringTypeDb.AVBRUTT_DELTAKELSE -> TiltaksdeltakerEndring.AvbruttDeltakelse
    TiltaksdeltakerEndringTypeDb.IKKE_AKTUELL_DELTAKELSE -> TiltaksdeltakerEndring.IkkeAktuellDeltakelse
    TiltaksdeltakerEndringTypeDb.FORLENGELSE -> TiltaksdeltakerEndring.Forlengelse(nySluttdato = LocalDate.parse(nySluttdato!!))
    TiltaksdeltakerEndringTypeDb.ENDRET_SLUTTDATO -> TiltaksdeltakerEndring.EndretSluttdato(nySluttdato = nySluttdato?.let { LocalDate.parse(it) })
    TiltaksdeltakerEndringTypeDb.ENDRET_STARTDATO -> TiltaksdeltakerEndring.EndretStartdato(nyStartdato = nyStartdato?.let { LocalDate.parse(it) })
    TiltaksdeltakerEndringTypeDb.ENDRET_DELTAKELSESMENGDE -> TiltaksdeltakerEndring.EndretDeltakelsesmengde(nyDeltakelsesprosent = nyDeltakelsesprosent, nyDagerPerUke = nyDagerPerUke)
    TiltaksdeltakerEndringTypeDb.ENDRET_STATUS -> TiltaksdeltakerEndring.EndretStatus(nyStatus = TiltakDeltakerstatus.valueOf(nyStatus!!))
}
