package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
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
 * Frontenden sender enten [barn] eller [perioder], ikke begge:
 * - [barn]: barna saksbehandler har valgt, se [BarnetilleggV2].
 *   Tom liste betyr ingen barnetillegg.
 * - [perioder]: antall barn per periode, se [BarnetilleggV1].
 *   Sendes av eldre versjoner av frontenden, mens den rulles ut.
 *   Tom liste betyr ingen barnetillegg.
 *   TODO: Fjernes når alle saksbehandlere har frontenden som sender barna.
 */
data class OppdaterBarnetilleggDTO(
    val barn: List<OppdaterBarnMedBarnetilleggDTO>? = null,
    val perioder: List<BarnetilleggPeriodeDTO>? = null,
    val begrunnelse: String?,
) {
    init {
        require((barn == null) != (perioder == null)) { "Barnetillegget må ha enten barn eller perioder" }
    }

    /** @throws UgyldigBarnetilleggException dersom barnetillegget er ugyldig, som gir 400 til frontend. */
    fun tilBarnetillegg(innvilgelsesperioder: NonEmptyList<Periode>): Barnetillegg =
        tilValgtBarnetillegg().tilBarnetillegg(innvilgelsesperioder).getOrElse { throw UgyldigBarnetilleggException(it) }

    fun tilValgtBarnetillegg(): ValgtBarnetillegg {
        val begrunnelse = begrunnelse?.let { Begrunnelse.create(it) }
        return if (barn != null) {
            ValgtBarnetillegg.Barn(barn = barn.map { it.tilDomene() }, begrunnelse = begrunnelse)
        } else {
            ValgtBarnetillegg.PerioderMedAntallBarn(
                perioderMedBarn = perioder!!.map { it.periode.toDomain() to AntallBarn(it.antallBarn) },
                begrunnelse = begrunnelse,
            )
        }
    }
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
