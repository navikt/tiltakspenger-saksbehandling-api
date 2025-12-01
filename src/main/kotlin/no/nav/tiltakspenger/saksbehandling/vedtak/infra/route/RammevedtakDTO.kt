package no.nav.tiltakspenger.saksbehandling.vedtak.infra.route

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.maksAntallDager
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param vedtaksdato Datoen vi bruker i brevet. Lagres samtidig som vi genererer og journalfører brevet. Vil være null fram til dette.
 * @param opprinneligVedtaksperiode Vedtaksperioden da den ble vedtatt. Er ikke sikkert den er gjeldende lenger, hvis den har blitt omgjort. Avslagsvedtak er aldri gjeldende.
 * @param gjeldendeVedtaksperioder Listen over perioder der vedtaket fortsatt er gjeldende for sakens nå-tilstand. Den var opprinnelig en hel periode, men kan ha blitt splittet av en eller flere omgjøringer. Vil alltid være tom for avslag siden de aldri er gjeldende.
 * @param opprinneligInnvilgetPerioder Vil alltid være tom for avslag, stans og rene opphør. For innvilgelser (inkl. omgjøring) og forlengelser vil dette være perioden(e) som opprinnelig ble innvilget i vedtaket.
 * @param gjeldendeInnvilgetPerioder Vil alltid være tom for avslag, stans og rene opphør. For innvilgelser (inkl. omgjøring) og forlengelser vil dette være perioden(e) som fortsatt er innvilget i vedtaket for sakens nå-tilstand.
 */
data class RammevedtakDTO(
    val id: String,
    val behandlingId: String,
    val opprettet: LocalDateTime,
    val vedtaksdato: LocalDate?,
    val resultat: RammebehandlingResultatTypeDTO,
    val opprinneligVedtaksperiode: PeriodeDTO,
    val opprinneligInnvilgetPerioder: List<PeriodeDTO>,
    val gjeldendeVedtaksperioder: List<PeriodeDTO>,
    val gjeldendeInnvilgetPerioder: List<PeriodeDTO>,
    val saksbehandler: String,
    val beslutter: String,
    val antallDagerPerMeldeperiode: Int,
    val barnetillegg: BarnetilleggDTO?,
    val erGjeldende: Boolean,
)

fun Rammevedtak.tilRammevedtakDTO(): RammevedtakDTO {
    val periodeDTO = periode.toDTO()

    return RammevedtakDTO(
        id = id.toString(),
        behandlingId = behandling.id.toString(),
        opprettet = opprettet,
        vedtaksdato = vedtaksdato,
        resultat = resultat.tilRammebehandlingResultatTypeDTO(),
        gjeldendeVedtaksperioder = this.gjeldendePerioder.map { it.toDTO() },
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        // TODO: sett en periodisering istedenfor bare maks
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode.maksAntallDager(),
        barnetillegg = barnetillegg?.toBarnetilleggDTO(),
        opprinneligVedtaksperiode = periodeDTO,
        opprinneligInnvilgetPerioder = listOfNotNull(this.innvilgelsesperiode?.toDTO()),
        gjeldendeInnvilgetPerioder = this.gjeldendeInnvilgelsesperioder.map { it.toDTO() },
        erGjeldende = this.erGjeldende,
    )
}

data class TidslinjeElementDTO(
    val rammevedtak: RammevedtakDTO,
    val periode: PeriodeDTO,
    val tidslinjeResultat: TidslinjeResultat,
)

enum class TidslinjeResultat {
    STANS,
    FORLENGELSE,
    SØKNADSBEHANDLING_INNVILGELSE,
    REVURDERING_INNVILGELSE,
    OMGJØRING_INNVILGELSE,
    OMGJØRING_OPPHØR,
}

data class TidslinjeDTO(
    val elementer: List<TidslinjeElementDTO>,
)

fun Rammevedtak.toTidslinjeElementDto(tidslinjeperiode: Periode): List<TidslinjeElementDTO> {
    return when (this.resultat) {
        is RevurderingResultat.Omgjøring -> {
            val innvilgelseperiode = tidslinjeperiode.overlappendePeriode(this.innvilgelsesperiode!!) ?: return listOf(
                // Dette omgjøringsvedtaket har ingen gjeldende innvilgelser. Hele perioden er et opphør.
                TidslinjeElementDTO(
                    rammevedtak = this.tilRammevedtakDTO().copy(barnetillegg = null),
                    periode = tidslinjeperiode.toDTO(),
                    tidslinjeResultat = TidslinjeResultat.OMGJØRING_OPPHØR,
                ),
            )
            val opphørtePeriode = tidslinjeperiode.trekkFra(innvilgelseperiode)

            val innvilgelsesTidslinjeElement = TidslinjeElementDTO(
                rammevedtak = this.tilRammevedtakDTO().copy(
                    barnetillegg = this.barnetillegg?.tilKrympetBarnetilleggDTO(innvilgelseperiode),
                ),
                periode = innvilgelseperiode.toDTO(),
                tidslinjeResultat = TidslinjeResultat.OMGJØRING_INNVILGELSE,
            )

            val opphørteTidslinjeElementer = opphørtePeriode.map {
                TidslinjeElementDTO(
                    rammevedtak = this.tilRammevedtakDTO().copy(barnetillegg = null),
                    periode = it.toDTO(),
                    tidslinjeResultat = TidslinjeResultat.OMGJØRING_OPPHØR,
                )
            }

            when (opphørtePeriode.size) {
                0 -> listOf(innvilgelsesTidslinjeElement)
                1 -> {
                    val singleOpphørsTidslinjeElement = opphørteTidslinjeElementer.single()
                    if (innvilgelseperiode.fraOgMed > tidslinjeperiode.fraOgMed) {
                        listOf(singleOpphørsTidslinjeElement, innvilgelsesTidslinjeElement)
                    } else {
                        listOf(innvilgelsesTidslinjeElement, singleOpphørsTidslinjeElement)
                    }
                }

                2 -> listOf(
                    opphørteTidslinjeElementer.first(),
                    innvilgelsesTidslinjeElement,
                    opphørteTidslinjeElementer.last(),
                )

                else -> throw IllegalStateException("Uventet antall opphørte perioder ved omgjøring: ${opphørtePeriode.size}")
            }
        }

        is SøknadsbehandlingResultat.Avslag -> throw IllegalStateException("Avslag kan ikke forekomme i tidslinje")
        is SøknadsbehandlingResultat.Innvilgelse,
        is RevurderingResultat.Innvilgelse,
        is RevurderingResultat.Stans,
        ->
            listOf(
                TidslinjeElementDTO(
                    rammevedtak = this.tilRammevedtakDTO().copy(
                        barnetillegg = this.barnetillegg?.tilKrympetBarnetilleggDTO(tidslinjeperiode),
                    ),
                    periode = tidslinjeperiode.toDTO(),
                    tidslinjeResultat = when (this.resultat) {
                        is RevurderingResultat.Omgjøring -> throw IllegalStateException("Omgjøring skal bli håndtert spesielt")

                        is SøknadsbehandlingResultat.Avslag -> throw IllegalStateException("Avslag kan ikke forekomme i tidslinje")

                        is RevurderingResultat.Innvilgelse -> TidslinjeResultat.REVURDERING_INNVILGELSE
                        is SøknadsbehandlingResultat.Innvilgelse -> TidslinjeResultat.SØKNADSBEHANDLING_INNVILGELSE
                        is RevurderingResultat.Stans -> TidslinjeResultat.STANS
                    },
                ),
            )
    }
}

fun Rammevedtaksliste.tilRammevedtakTidslinjeDTO(): TidslinjeDTO {
    return tidslinje.perioderMedVerdi.flatMap { (rammevedtak, tidslinjeperiode) ->
        rammevedtak.toTidslinjeElementDto(tidslinjeperiode)
    }.let { TidslinjeDTO(it) }
}

fun Rammevedtaksliste.tilRammevedtakInnvilgetTidslinjeDTO(): TidslinjeDTO {
    return innvilgetTidslinje.perioderMedVerdi.flatMap { (rammevedtak, tidslinjeperiode) ->
        rammevedtak.toTidslinjeElementDto(tidslinjeperiode)
    }.let { TidslinjeDTO(it) }
}

private fun Barnetillegg.tilKrympetBarnetilleggDTO(periode: Periode): BarnetilleggDTO = BarnetilleggDTO(
    perioder = periodisering.krymp(periode).perioderMedVerdi.map {
        BarnetilleggPeriodeDTO(
            antallBarn = it.verdi.value,
            periode = it.periode.toDTO(),
        )
    },
    begrunnelse = begrunnelse?.verdi,
)
