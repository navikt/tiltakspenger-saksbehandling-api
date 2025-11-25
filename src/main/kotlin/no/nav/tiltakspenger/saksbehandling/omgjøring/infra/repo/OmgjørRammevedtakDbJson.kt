package no.nav.tiltakspenger.saksbehandling.omgjøring.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak

/**
 * Sier noe om et vedtak omgjør et eller flere vedtak, helt eller delvis.
 * Skal kun brukes i db-laget.
 */
data class OmgjørRammevedtakDbJson(
    val omgjørRammevedtak: List<OmgjøringsperiodeDbJson>,
) {
    fun toDomain() = OmgjørRammevedtak(omgjørRammevedtak.toDomain())
}

fun OmgjørRammevedtak.toDbJson(): String = serialize(OmgjørRammevedtakDbJson(this.omgjøringsperioder.toDbJson()))

fun String?.toOmgjørRammevedtak(): OmgjørRammevedtak {
    if (this == null) return OmgjørRammevedtak.empty
    return deserialize<OmgjørRammevedtakDbJson>(this).toDomain()
}
