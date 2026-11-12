package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.UgyldigInnvilgelsesperiode

sealed interface KanIkkeOppdatereBehandling {
    /** Inntil videre støtter vi ikke vedtak over allerede utbetalte perioder (gjelder søknadsbehandling) */
    data object InnvilgelsesperiodenOverlapperMedUtbetaltPeriode : KanIkkeOppdatereBehandling

    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeOppdatereBehandling

    data object MåVæreUnderBehandling : KanIkkeOppdatereBehandling

    data object ErPaVent : KanIkkeOppdatereBehandling

    /** Oppdateringen gjelder en annen behandlingstype eller et resultat behandlingen ikke kan bytte til. */
    data object FeilResultattype : KanIkkeOppdatereBehandling

    data object KanIkkeOpphøre : KanIkkeOppdatereBehandling

    data class UgyldigPeriodeForStans(val årsak: String) : KanIkkeOppdatereBehandling

    data object UgyldigeMeldeperioderHelg : KanIkkeOppdatereBehandling

    data object InnvilgelsesperiodeHarIngenOverlappMedTiltaksdeltakelserFraSaksopplysninger : KanIkkeOppdatereBehandling

    data class InnvilgelsenPasserIkkeMedTiltaksdeltakelsene(val ugyldigePerioder: NonEmptyList<UgyldigInnvilgelsesperiode>) : KanIkkeOppdatereBehandling

    /** Summen av antall dager per meldeperiode for tiltaksdeltakelsene i [perioder] er mer enn det er dager i en meldeperiode. */
    data class ForMangeDagerPerMeldeperiode(val perioder: NonEmptyList<Periode>) : KanIkkeOppdatereBehandling
}

sealed interface KanIkkeOppdatereOmgjøring : KanIkkeOppdatereBehandling {
    data object MåOmgjøreMinstEttVedtak : KanIkkeOppdatereOmgjøring

    data object VedtaksperiodeMåInneholdeInnvilgelsesperiodene : KanIkkeOppdatereOmgjøring

    data class UgyldigPeriodeForOpphør(val årsak: String) : KanIkkeOppdatereOmgjøring

    data class MåHaFritekstForValgteHjemler(val hjemler: NonEmptySet<HjemmelForOpphør>) : KanIkkeOppdatereOmgjøring
}
