package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import java.time.LocalDate

/**
 * TODO jah: Splitt denne opp i en kommando per [BehandlingResultatType] som støttes. Dette er rotete nå.
 *
 * @param avslagsgrunner Brukes kun ved søknadsbehandling avslag
 * @param valgteHjemler Brukes kun ved revurdering til stans
 * @param stansFraOgMed Brukes kun ved revurdering til stans
 * @param stansTilOgMed Brukes kun ved revurdering til stans
 * @param vedtaksperiode Brukes ved avslag og innvilgelse (søknadsbehandling+revurdering). Brukes kun hvis den ikke er satt på behandlingen.
 * @param barnetillegg Brukes ved innvilgelse (søknadsbehandling+revurdering). Kan inneholde hull, men kan ikke være tom. Må valideres basert på innsendt [vedtaksperiode] eller vedtaksperiodeen på behandlingen.
 */
data class ForhåndsvisVedtaksbrevKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val resultat: BehandlingResultatType,
    val valgteHjemler: List<ValgtHjemmelForStans>?,
    val vedtaksperiode: Periode?,
    val barnetillegg: IkkeTomPeriodisering<AntallBarn>?,
    val stansFraOgMed: LocalDate?,
    val stansTilOgMed: LocalDate?,
    val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>?,
    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode>?,
) {
    fun hentStansperiode(førsteDagSomGirRett: LocalDate, sisteDagSomGirRett: LocalDate): Periode {
        if (stansFraOgMed != null) {
            require(stansFraOgMed >= førsteDagSomGirRett) {
                "Stans fra og med $stansFraOgMed kan ikke være før første dag som gir rett $førsteDagSomGirRett"
            }
        }
        if (stansTilOgMed != null) {
            require(stansTilOgMed <= sisteDagSomGirRett) {
                "Stans til og med $stansTilOgMed kan ikke være etter siste dag som gir rett $sisteDagSomGirRett"
            }
        }
        return Periode(
            fraOgMed = hentStansFraOgMed(førsteDagSomGirRett),
            tilOgMed = hentStansTilOgMed(sisteDagSomGirRett),
        )
    }

    private fun hentStansFraOgMed(førsteDagSomGirRett: LocalDate): LocalDate {
        return stansFraOgMed ?: førsteDagSomGirRett
    }

    private fun hentStansTilOgMed(sisteDagSomGirRett: LocalDate): LocalDate {
        return stansTilOgMed ?: sisteDagSomGirRett
    }

    init {
        when (resultat) {
            RevurderingType.STANS -> {
                requireNotNull(valgteHjemler)
                require(vedtaksperiode == null) { "Kan ikke sende inn vedtaksperiode ved stans" }
                require(avslagsgrunner == null) { "Kan ikke sende inn avslagsgrunner ved stans" }
                require(barnetillegg == null) { "Kan ikke sende inn barnetillegg ved stans" }
            }

            RevurderingType.INNVILGELSE, SøknadsbehandlingType.INNVILGELSE, RevurderingType.OMGJØRING -> {
                requireNotNull(vedtaksperiode)
                require(avslagsgrunner == null) { "Kan ikke sende inn avslagsgrunner ved innvilgelse" }
                require(stansFraOgMed == null && stansTilOgMed == null) { "Kan ikke sende inn stansFraOgMed/stansTilOgMed ved innvilgelse" }
                require(valgteHjemler == null) { "Kan ikke sende inn valgteHjemler ved innvilgelse" }
                // Barnetillegg er null hvis det ikke innvilges barn (uavhengig om de har søkt)
            }

            SøknadsbehandlingType.AVSLAG -> {
                requireNotNull(avslagsgrunner)
                // Kan enable denne når frontend er oppdatert til ikke å sende den
//                require(vedtaksperiode == null) { "Kan ikke sende inn vedtaksperiode ved avslag" }
                require(valgteHjemler == null) { "Kan ikke sende inn valgteHjemler ved avslag" }
                require(barnetillegg == null) { "Kan ikke sende inn barnetillegg ved avslag" }
                require(stansFraOgMed == null && stansTilOgMed == null) { "Kan ikke sende inn stansFraOgMed/stansTilOgMed ved avslag" }
            }
        }
    }
}
