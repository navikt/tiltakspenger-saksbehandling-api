package no.nav.tiltakspenger.saksbehandling.vedtak.infra.route

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.tilBarnetilleggPerioderDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperioderDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.OmgjøringsgradDTO.Companion.tilDTO
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param vedtaksdato Datoen vi bruker i brevet.
 * Lagres samtidig som vi genererer og journalfører brevet.
 * Vil være null fram til dette.
 * @param opprinneligVedtaksperiode Vedtaksperioden da den ble vedtatt.
 * Er ikke sikkert den er gjeldende lenger, hvis den har blitt omgjort.
 * Avslagsvedtak er aldri gjeldende.
 * @param gjeldendeVedtaksperioder Listen over perioder der vedtaket fortsatt er gjeldende for sakens nå-tilstand.
 * Den var opprinnelig en hel periode, men kan ha blitt splittet av en eller flere omgjøringer.
 * Vil alltid være tom for avslag siden de aldri er gjeldende.
 * @param opprinneligInnvilgetPerioder Vil alltid være tom for avslag, stans og rene opphør.
 * For innvilgelser (inkl. omgjøring) og forlengelser vil dette være perioden(e) som opprinnelig ble innvilget i vedtaket.
 * @param gjeldendeInnvilgetPerioder Vil alltid være tom for avslag, stans og rene opphør.
 * For innvilgelser (inkl. omgjøring) og forlengelser vil dette være perioden(e) som fortsatt er innvilget i vedtaket for sakens nå-tilstand.
 */
data class RammevedtakDTO(
    val id: String,
    val behandlingId: String,
    val klagebehandlingId: String?,
    val opprettet: LocalDateTime,
    val vedtaksdato: LocalDate?,
    val resultat: RammebehandlingResultatTypeDTO,
    val opprinneligVedtaksperiode: PeriodeDTO,
    val opprinneligInnvilgetPerioder: List<PeriodeDTO>,
    val gjeldendeVedtaksperioder: List<PeriodeDTO>,
    val gjeldendeInnvilgetPerioder: List<PeriodeDTO>,
    val saksbehandler: String,
    val beslutter: String,
    val innvilgelsesperioder: InnvilgelsesperioderDTO?,
    val barnetillegg: BarnetilleggDTO?,
    val gjeldendeBarnetilleggPerioder: List<BarnetilleggPeriodeDTO>,
    val erGjeldende: Boolean,
    val gyldigeKommandoer: Map<RammevedtakKommandoDTO.KommandoType, RammevedtakKommandoDTO>,
    val omgjortGrad: OmgjøringsgradDTO?,
    val skalSendeVedtaksbrev: Boolean,
)

enum class OmgjøringsgradDTO {
    HELT,
    DELVIS,
    ;

    companion object {
        fun Omgjøringsgrad.tilDTO(): OmgjøringsgradDTO {
            return when (this) {
                Omgjøringsgrad.HELT -> HELT
                Omgjøringsgrad.DELVIS -> DELVIS
            }
        }
    }
}

fun Rammevedtak.tilRammevedtakDTO(): RammevedtakDTO {
    return RammevedtakDTO(
        id = id.toString(),
        behandlingId = rammebehandling.id.toString(),
        klagebehandlingId = rammebehandling.klagebehandling?.id?.toString(),
        opprettet = opprettet,
        vedtaksdato = vedtaksdato,
        resultat = rammebehandlingsresultat.tilRammebehandlingResultatTypeDTO(),
        gjeldendeVedtaksperioder = this.gjeldendePerioder.map { it.toDTO() },
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        innvilgelsesperioder = innvilgelsesperioder?.tilDTO(),
        barnetillegg = barnetillegg?.toBarnetilleggDTO(),
        gjeldendeBarnetilleggPerioder = gjeldendeBarnetillegg.tilBarnetilleggPerioderDTO(),
        opprinneligVedtaksperiode = periode.toDTO(),
        opprinneligInnvilgetPerioder = this.opprinneligInnvilgetPerioder.map { it.toDTO() },
        gjeldendeInnvilgetPerioder = this.gjeldendeInnvilgetPerioder.map { it.toDTO() },
        erGjeldende = this.erGjeldende,
        gyldigeKommandoer = this.gyldigeKommandoer.toDTO(),
        omgjortGrad = this.omgjortGrad?.tilDTO(),
        skalSendeVedtaksbrev = this.skalSendeVedtaksbrev,
    )
}

data class TidslinjeElementDTO(
    val rammevedtakId: String,
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
    return when (this.rammebehandlingsresultat) {
        is OmgjøringInnvilgelse -> {
            val innvilgedePerioder = tidslinjeperiode.overlappendePerioder(this.innvilgelsesperioder!!.perioder)

            // Det som ikke er innvilget i denne tidslinjeperioden, er opphørt av omgjøringen.
            val opphørtePerioder = tidslinjeperiode.trekkFra(innvilgedePerioder)

            innvilgedePerioder.map { it to TidslinjeResultat.OMGJØRING_INNVILGELSE }
                .plus(opphørtePerioder.map { it to TidslinjeResultat.OMGJØRING_OPPHØR })
                .sortedBy { (periode, _) -> periode.fraOgMed }
                .map { (periode, tidslinjeResultat) ->
                    TidslinjeElementDTO(
                        rammevedtakId = this.id.toString(),
                        periode = periode.toDTO(),
                        tidslinjeResultat = tidslinjeResultat,
                    )
                }
        }

        is Omgjøringsresultat.OmgjøringOpphør -> listOf(
            TidslinjeElementDTO(
                rammevedtakId = this.id.toString(),
                periode = tidslinjeperiode.toDTO(),
                tidslinjeResultat = TidslinjeResultat.OMGJØRING_OPPHØR,
            ),
        )

        is Søknadsbehandlingsresultat.Innvilgelse,
        is Revurderingsresultat.Innvilgelse,
        is Revurderingsresultat.Stans,
        ->
            listOf(
                TidslinjeElementDTO(
                    rammevedtakId = this.id.toString(),
                    periode = tidslinjeperiode.toDTO(),
                    tidslinjeResultat = when (this.rammebehandlingsresultat) {
                        is Omgjøringsresultat -> throw IllegalStateException("Omgjøring skal bli håndtert spesielt")
                        is Søknadsbehandlingsresultat.Avslag -> throw IllegalStateException("Avslag kan ikke forekomme i tidslinje")
                        is Revurderingsresultat.Innvilgelse -> TidslinjeResultat.REVURDERING_INNVILGELSE
                        is Søknadsbehandlingsresultat.Innvilgelse -> TidslinjeResultat.SØKNADSBEHANDLING_INNVILGELSE
                        is Revurderingsresultat.Stans -> TidslinjeResultat.STANS
                    },
                ),
            )

        is Søknadsbehandlingsresultat.Avslag,
        is Omgjøringsresultat.OmgjøringIkkeValgt,
        -> throw IllegalStateException("${this.rammebehandlingsresultat} kan ikke forekomme i tidslinje")
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
