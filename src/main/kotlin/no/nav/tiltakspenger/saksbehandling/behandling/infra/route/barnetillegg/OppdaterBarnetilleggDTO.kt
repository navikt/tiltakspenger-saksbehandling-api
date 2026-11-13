package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
import no.nav.tiltakspenger.saksbehandling.barnetillegg.UgyldigBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.UgyldigBarnetilleggException
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.ValgtBarnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import java.time.LocalDate

/**
 * Barnetillegget slik frontenden sender det inn.
 * Inngående DTO-er har rå verdier, siden sladding kun gjelder det vi sender ut.
 *
 * @param barn Barna saksbehandler har valgt, se [BarnetilleggV2].
 * Tom liste betyr ingen barnetillegg.
 */
data class OppdaterBarnetilleggDTO(
    val barn: List<OppdaterBarnMedBarnetilleggDTO>,
    val begrunnelse: String?,
) {
    /** @throws UgyldigBarnetilleggException dersom barnetillegget er ugyldig, som gir 400 til frontend. */
    fun tilBarnetillegg(innvilgelsesperioder: NonEmptyList<Periode>): BarnetilleggV2 =
        tilValgtBarnetillegg().tilBarnetillegg(innvilgelsesperioder).getOrElse { throw UgyldigBarnetilleggException(it) }

    fun tilValgtBarnetillegg(): ValgtBarnetillegg = ValgtBarnetillegg(
        barn = barn.map { it.tilDomene() },
        begrunnelse = begrunnelse?.let { Begrunnelse.create(it) },
    )
}

/**
 * @param søknadId Påkrevd når [kilde] er [BarnKildeDTO.SØKNAD], og skal ellers være null.
 * @param perioder Null betyr hele innvilgelsesperioden.
 * Er periodene valgt, må det være minst én.
 */
data class OppdaterBarnMedBarnetilleggDTO(
    val fnr: String?,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val fødselsdato: LocalDate,
    val kilde: BarnKildeDTO,
    val søknadId: String?,
    val perioder: List<PeriodeDTO>?,
) {
    fun tilDomene(): BarnMedBarnetillegg = BarnMedBarnetillegg(
        fnr = fnr?.takeIf { it.isNotBlank() }?.let {
            Fnr.tryFromString(it.trim()) ?: throw UgyldigBarnetilleggException(UgyldigBarnetillegg.UgyldigFnr(fødselsdato))
        },
        fornavn = fornavn?.trim()?.ifBlank { null },
        mellomnavn = mellomnavn?.trim()?.ifBlank { null },
        etternavn = etternavn?.trim()?.ifBlank { null },
        fødselsdato = fødselsdato,
        kilde = when (kilde) {
            BarnKildeDTO.SØKNAD ->
                søknadId
                    ?.let { runCatching { SøknadId.fromString(it) }.getOrNull() }
                    ?.let { BarnMedBarnetillegg.Kilde.Søknad(it) }

            BarnKildeDTO.SAKSBEHANDLER -> BarnMedBarnetillegg.Kilde.Saksbehandler.takeIf { søknadId == null }
        } ?: throw UgyldigBarnetilleggException(UgyldigBarnetillegg.UgyldigKilde(fødselsdato)),
        perioder = perioder?.map { it.toDomain() }?.toNonEmptyListOrNull(),
    ).also {
        if (perioder?.isEmpty() == true) throw UgyldigBarnetilleggException(UgyldigBarnetillegg.ManglerPerioder(it))
    }
}
