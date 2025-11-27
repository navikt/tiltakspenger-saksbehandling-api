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
 * @param periode Den opprinnelige perioden for vedtaket
 * @param gjeldendePeriode Perioden der vedtaket fortsatt er gjeldende for sakens nå-tilstand
 */
data class RammevedtakDTO(
    val id: String,
    val behandlingId: String,
    val opprettet: LocalDateTime,
    val vedtaksdato: LocalDate?,
    val resultat: RammebehandlingResultatTypeDTO,
    val periode: PeriodeDTO,
    val gjeldendePeriode: PeriodeDTO,
    val saksbehandler: String,
    val beslutter: String,
    val antallDagerPerMeldeperiode: Int,
    val barnetillegg: BarnetilleggDTO?,
)

fun Rammevedtak.tilRammevedtakDTO(): RammevedtakDTO {
    val periodeDTO = periode.toDTO()

    return RammevedtakDTO(
        id = id.toString(),
        behandlingId = behandling.id.toString(),
        opprettet = opprettet,
        vedtaksdato = vedtaksdato,
        resultat = resultat.tilRammebehandlingResultatTypeDTO(),
        periode = periodeDTO,
        gjeldendePeriode = periodeDTO,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        // TODO: sett en periodisering istedenfor bare maks
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode.maksAntallDager(),
        barnetillegg = barnetillegg?.toBarnetilleggDTO(),
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
            val innvilgelseperiode = tidslinjeperiode.overlappendePeriode(this.innvilgelsesperiode!!)
            // TODO - denne if'en burde vi lage en test for.
            if (innvilgelseperiode == null) {
                return listOf(
                    TidslinjeElementDTO(
                        rammevedtak = this.tilRammevedtakDTO().copy(
                            gjeldendePeriode = tidslinjeperiode.toDTO(),
                            barnetillegg = null,
                        ),
                        periode = tidslinjeperiode.toDTO(),
                        tidslinjeResultat = TidslinjeResultat.OMGJØRING_OPPHØR,
                    ),
                )
            }

            val opphørtePeriode = tidslinjeperiode.trekkFra(innvilgelseperiode)

            val innvilgelsesTidslinjeElement = TidslinjeElementDTO(
                rammevedtak = this.tilRammevedtakDTO().copy(
                    gjeldendePeriode = innvilgelseperiode.toDTO(),
                    barnetillegg = this.barnetillegg?.tilKrympetBarnetilleggDTO(innvilgelseperiode),
                ),
                periode = innvilgelseperiode.toDTO(),
                tidslinjeResultat = TidslinjeResultat.OMGJØRING_INNVILGELSE,
            )

            val opphørteTidslinjeElementer = opphørtePeriode.map {
                TidslinjeElementDTO(
                    rammevedtak = this.tilRammevedtakDTO().copy(
                        gjeldendePeriode = it.toDTO(),
                        barnetillegg = null,
                    ),
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
                        gjeldendePeriode = tidslinjeperiode.toDTO(),
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
    }.let {
        TidslinjeDTO(elementer = it)
    }
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
