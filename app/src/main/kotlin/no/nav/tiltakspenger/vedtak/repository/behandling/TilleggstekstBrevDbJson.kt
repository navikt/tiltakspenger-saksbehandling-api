package no.nav.tiltakspenger.vedtak.repository.behandling

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.domene.behandling.TilleggstekstBrev
import java.security.InvalidParameterException

data class TilleggstekstBrevDbJson(
    val subsumsjon: SubsumsjonDb,
    val tekst: String,
)

internal fun String.toTilleggstekstBrev(): TilleggstekstBrev =
    try {
        val tilleggstekstBrevJson = deserialize<TilleggstekstBrevDbJson>(this)

        TilleggstekstBrev(
            subsumsjon = tilleggstekstBrevJson.subsumsjon.toDomain(),
            tekst = tilleggstekstBrevJson.tekst,
        )
    } catch (exception: Exception) {
        throw InvalidParameterException("Det oppstod en feil ved parsing av json: " + exception.message)
    }

internal fun TilleggstekstBrev.toDbJson(): String =
    serialize(
        TilleggstekstBrevDbJson(
            subsumsjon = subsumsjon.toDb(),
            tekst = tekst,
        ),
    )
