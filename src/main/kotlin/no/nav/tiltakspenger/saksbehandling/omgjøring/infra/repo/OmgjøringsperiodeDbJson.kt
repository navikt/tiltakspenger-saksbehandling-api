package no.nav.tiltakspenger.saksbehandling.omgjøring.infra.repo

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperioder

/**
 * Sier noe om en periode et vedtak omgjør eller er omgjort, helt eller delvis.
 * Skal kun brukes i db-laget.
 */
data class OmgjøringsperiodeDbJson(
    val vedtakId: String,
    val periode: PeriodeDbJson,
    val omgjøringsgrad: OmgjøringsgradDbJson,
) {
    fun toDomain(): Omgjøringsperiode {
        return Omgjøringsperiode(
            rammevedtakId = VedtakId.fromString(vedtakId),
            periode = periode.toDomain(),
            omgjøringsgrad = omgjøringsgrad.toDomain(),
        )
    }
}

fun List<OmgjøringsperiodeDbJson>.toDomain(): Omgjøringsperioder {
    return Omgjøringsperioder(this.map { it.toDomain() })
}

fun Omgjøringsperioder.toDbJson(): List<OmgjøringsperiodeDbJson> {
    return this.map { it.toDbJson() }
}

fun Omgjøringsperiode.toDbJson(): OmgjøringsperiodeDbJson {
    return OmgjøringsperiodeDbJson(
        vedtakId = this.rammevedtakId.toString(),
        periode = this.periode.toDbJson(),
        omgjøringsgrad = this.omgjøringsgrad.toDbJson(),
    )
}
