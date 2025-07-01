package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelHarIkkeRettighet
import java.time.LocalDate

/**
 * TODO jah: Splitt denne opp i en kommando per [BehandlingResultatType] som støttes. Dette er rotete nå.
 *
 * @param avslagsgrunner Brukes kun ved søknadsbehandling avslag
 * @param valgteHjemler Brukes kun ved revurdering til stans
 * @param stansDato Brukes kun ved revurdering til stans
 * @param virkningsperiode Brukes ved avslag og innvilgelse (søknadsbehandling+revurdering). Brukes kun hvis den ikke er satt på behandlingen.
 * @param barnetillegg Brukes ved innvilgelse (søknadsbehandling+revurdering). Kan inneholde hull, men kan ikke være tom. Må valideres basert på innsendt virkningsperiode eller virkningsperioden på behandlingen.
 */
data class ForhåndsvisVedtaksbrevKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev,
    val resultat: BehandlingResultatType,
    val valgteHjemler: List<ValgtHjemmelHarIkkeRettighet>?,
    val virkningsperiode: Periode?,
    val barnetillegg: IkkeTomPeriodisering<AntallBarn>?,
    val stansDato: LocalDate?,
    val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>?,
) {
    init {
        when (resultat) {
            RevurderingType.STANS -> {
                requireNotNull(stansDato)
                requireNotNull(valgteHjemler)
                require(virkningsperiode == null) { "Kan ikke sende inn virkningsperiode ved stans" }
                require(avslagsgrunner == null) { "Kan ikke sende inn avslagsgrunner ved stans" }
                require(barnetillegg == null) { "Kan ikke sende inn barnetillegg ved stans" }
            }
            RevurderingType.INNVILGELSE, SøknadsbehandlingType.INNVILGELSE -> {
                requireNotNull(virkningsperiode)
                require(avslagsgrunner == null) { "Kan ikke sende inn avslagsgrunner ved innvilgelse" }
                require(stansDato == null) { "Kan ikke sende inn stansDato ved innvilgelse" }
                require(valgteHjemler == null) { "Kan ikke sende inn valgteHjemler ved innvilgelse" }
                // Barnetillegg er null hvis det ikke innvilges barn (uavhengig om de har søkt)
            }
            SøknadsbehandlingType.AVSLAG -> {
                requireNotNull(avslagsgrunner)
                requireNotNull(virkningsperiode)
                require(valgteHjemler == null) { "Kan ikke sende inn valgteHjemler ved avslag" }
                require(barnetillegg == null) { "Kan ikke sende inn barnetillegg ved avslag" }
                require(stansDato == null) { "Kan ikke sende inn stansDato ved avslag" }
            }
        }
    }
}
