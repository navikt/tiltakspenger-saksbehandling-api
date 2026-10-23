package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater

import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

/**
 * Forventede feil når saksbehandler oppdaterer en forlengelse.
 */
sealed interface KanIkkeForlenge : KanIkkeOppdatereBehandling {

    /**
     * Et annet vedtak er iverksatt etter at forlengelsen ble opprettet, og vedtaket kan ikke lenger forlenges fra samme dag.
     * Saksbehandler må avbryte forlengelsen og starte en ny.
     */
    data object VedtaketKanIkkeLengerForlenges : KanIkkeForlenge

    data class TilOgMedMåVæreEtterVedtaket(val sisteInnvilgedeDag: LocalDate) : KanIkkeForlenge

    data class OverlapperAndreVedtak(val forlengelsesperiode: Periode) : KanIkkeForlenge

    /** Tiltaksdeltakelsen i saksopplysningene dekker ikke hele forlengelsen, eller finnes ikke lenger. */
    data class TiltaksdeltakelsenDekkerIkkeForlengelsen(val forlengelsesperiode: Periode) : KanIkkeForlenge

    data class BarnetilleggetGårUtoverForlengelsen(val forlengelsesperiode: Periode) : KanIkkeForlenge
}
