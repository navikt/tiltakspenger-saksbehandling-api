package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import java.time.LocalDate

/**
 * Endringen lagres som en liste av enkeltendringer, slik den ble lagret før endringene ble tolket til ett utfall.
 * Formatet er beholdt for å kunne lese rader som allerede er lagret.
 * Eldre rader kan inneholde feltet `hendelseId`, som ignoreres ved lesing.
 */
private data class AutomatiskOpprettetRevurderingGrunnDbJson(
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
    AVSLUTTET_SOM_FORVENTET,
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
            endringer = endring.tilEndringerDbJson(),
        ),
    )
}

fun String.toAutomatiskOpprettetRevurderingGrunn(): AutomatiskOpprettetRevurderingGrunn {
    val dbJson = deserialize<AutomatiskOpprettetRevurderingGrunnDbJson>(this)
    return AutomatiskOpprettetRevurderingGrunn(
        endring = dbJson.endringer.toDomain(),
    )
}

/**
 * Endringen som en liste av enkeltendringer, i samme format som i [AutomatiskOpprettetRevurderingGrunn].
 * Brukes for sporbarhet i `tiltaksdeltaker_endring`, der den kun skrives.
 */
fun TiltaksdeltakerEndring.toDbJson(): String = serialize(tilEndringerDbJson())

private fun TiltaksdeltakerEndring.tilEndringerDbJson(): List<TiltaksdeltakerEndringDbJson> = when (this) {
    is TiltaksdeltakerEndring.AvsluttetSomForventet -> listOf(TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.AVSLUTTET_SOM_FORVENTET))

    is TiltaksdeltakerEndring.AvbruttDeltakelse -> listOf(TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.AVBRUTT_DELTAKELSE))

    is TiltaksdeltakerEndring.IkkeAktuellDeltakelse -> listOf(TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.IKKE_AKTUELL_DELTAKELSE))

    is TiltaksdeltakerEndring.Forlengelse -> listOfNotNull(
        endretDeltakelsesmengde?.toDbJson(),
        TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.FORLENGELSE, nySluttdato = nySluttdato.toString()),
    )

    is TiltaksdeltakerEndring.AndreEndringer -> endringer.map { it.toDbJson() }
}

private fun TiltaksdeltakerEndring.Endringsdetalj.toDbJson(): TiltaksdeltakerEndringDbJson = when (this) {
    is TiltaksdeltakerEndring.EndretDeltakelsesmengde -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_DELTAKELSESMENGDE, nyDeltakelsesprosent = nyDeltakelsesprosent, nyDagerPerUke = nyDagerPerUke)
    is TiltaksdeltakerEndring.EndretStartdato -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_STARTDATO, nyStartdato = nyStartdato?.toString())
    is TiltaksdeltakerEndring.EndretSluttdato -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_SLUTTDATO, nySluttdato = nySluttdato?.toString())
    is TiltaksdeltakerEndring.EndretStatus -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_STATUS, nyStatus = nyStatus.name)
}

/**
 * Utfallene prioriteres i samme rekkefølge som når endringene tolkes, slik at også eldre rader med flere enkeltendringer gir ett utfall.
 */
private fun List<TiltaksdeltakerEndringDbJson>.toDomain(): TiltaksdeltakerEndring {
    fun finn(type: TiltaksdeltakerEndringTypeDb) = firstOrNull { it.type == type }

    val endretDeltakelsesmengde = finn(TiltaksdeltakerEndringTypeDb.ENDRET_DELTAKELSESMENGDE)?.let {
        TiltaksdeltakerEndring.EndretDeltakelsesmengde(nyDeltakelsesprosent = it.nyDeltakelsesprosent, nyDagerPerUke = it.nyDagerPerUke)
    }
    val forlengelse = finn(TiltaksdeltakerEndringTypeDb.FORLENGELSE)

    return when {
        finn(TiltaksdeltakerEndringTypeDb.AVSLUTTET_SOM_FORVENTET) != null -> TiltaksdeltakerEndring.AvsluttetSomForventet

        finn(TiltaksdeltakerEndringTypeDb.AVBRUTT_DELTAKELSE) != null -> TiltaksdeltakerEndring.AvbruttDeltakelse

        finn(TiltaksdeltakerEndringTypeDb.IKKE_AKTUELL_DELTAKELSE) != null -> TiltaksdeltakerEndring.IkkeAktuellDeltakelse

        forlengelse != null -> TiltaksdeltakerEndring.Forlengelse(
            nySluttdato = LocalDate.parse(forlengelse.nySluttdato!!),
            endretDeltakelsesmengde = endretDeltakelsesmengde,
        )

        else -> TiltaksdeltakerEndring.AndreEndringer(
            endretDeltakelsesmengde = endretDeltakelsesmengde,
            endretStartdato = finn(TiltaksdeltakerEndringTypeDb.ENDRET_STARTDATO)?.let {
                TiltaksdeltakerEndring.EndretStartdato(nyStartdato = it.nyStartdato?.let(LocalDate::parse))
            },
            endretSluttdato = finn(TiltaksdeltakerEndringTypeDb.ENDRET_SLUTTDATO)?.let {
                TiltaksdeltakerEndring.EndretSluttdato(nySluttdato = it.nySluttdato?.let(LocalDate::parse))
            },
            endretStatus = finn(TiltaksdeltakerEndringTypeDb.ENDRET_STATUS)?.let {
                TiltaksdeltakerEndring.EndretStatus(nyStatus = TiltakDeltakerstatus.valueOf(it.nyStatus!!))
            },
        )
    }
}
