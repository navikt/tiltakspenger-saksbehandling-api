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
    val valgteHjemler: List<ValgtHjemmelHarIkkeRettighet>,
    val virkningsperiode: Periode?,
    val barnetillegg: IkkeTomPeriodisering<AntallBarn>?,
    val stansDato: LocalDate?,
    val resultat: BehandlingResultatType,
    val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>?,
) {
    init {
        if (resultat == SøknadsbehandlingType.AVSLAG || avslagsgrunner != null) {
            require(resultat == SøknadsbehandlingType.AVSLAG) { "Behandlingsresultat må være AVSLAG når det er valgt avslagsgrunner" }
            require(avslagsgrunner != null) { "Det må være valgt avslagsgrunner når behandlingsresultat er AVSLAG" }
        }
        if (virkningsperiode != null && barnetillegg != null) {
            require(virkningsperiode.inneholderHele(barnetillegg.totalPeriode)) { "Når man sender inn virkningsperiode og barnetillegg, kan ikke barnetilleggsperioden være på utsiden av virkningsperioden" }
        }
        if (resultat == RevurderingType.STANS || stansDato != null) {
            require(resultat == RevurderingType.STANS) { "Behandlingsresultat må være STANS når det er valgt stansDato" }
            require(stansDato != null) { "Det må være valgst stansDato når behandlingsresultatet er STANS" }
        }
        if (barnetillegg != null) {
            require(barnetillegg.isNotEmpty()) { "Man kan ikke sende en tom barnetilleggs liste. Enten må man sende null eller minst ett element." }
        }
    }
}
