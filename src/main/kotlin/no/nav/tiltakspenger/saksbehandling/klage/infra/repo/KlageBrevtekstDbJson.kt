package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst

private data class KlageBrevtekstDbJson(
    val tekster: List<TittelOgTekstDbJson>,
) {
    data class TittelOgTekstDbJson(
        val tittel: String,
        val tekst: String,
    )

    fun toDomain(): Brevtekster = Brevtekster(
        tekster = tekster.map {
            TittelOgTekst(
                tittel = NonBlankString.create(it.tittel),
                tekst = NonBlankString.create(it.tekst),
            )
        },
    )
}

fun String.toKlageBrevtekst(): Brevtekster {
    return deserialize<KlageBrevtekstDbJson>(this).toDomain()
}

fun Brevtekster.toDbJson(): String {
    return serialize(
        KlageBrevtekstDbJson(
            tekster = this.map {
                KlageBrevtekstDbJson.TittelOgTekstDbJson(
                    tittel = it.tittel.value,
                    tekst = it.tekst.value,
                )
            },
        ),
    )
}
