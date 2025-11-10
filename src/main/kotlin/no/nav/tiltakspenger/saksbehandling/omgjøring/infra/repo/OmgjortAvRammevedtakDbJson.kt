package no.nav.tiltakspenger.saksbehandling.omgjøring.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjortAvRammevedtak

/**
 * Sier noe om dette vedtaket er omgjort av et eller flere vedtak, helt eller delvis.
 * Skal kun brukes i db-laget.
 */
data class OmgjortAvRammevedtakDbJson(
    val omgjortAvRammevedtak: List<OmgjøringsperiodeDbJson>,
) {
    fun toDomain(): OmgjortAvRammevedtak = OmgjortAvRammevedtak(omgjortAvRammevedtak.toDomain())
}

fun OmgjortAvRammevedtak.toDbJson(): String = serialize(OmgjortAvRammevedtakDbJson(this.omgjøringsperioder.toDbJson()))

fun String?.toOmgjortAvRammevedtak(): OmgjortAvRammevedtak {
    if (this == null) return OmgjortAvRammevedtak.empty
    return deserialize<OmgjortAvRammevedtakDbJson>(this).toDomain()
}
