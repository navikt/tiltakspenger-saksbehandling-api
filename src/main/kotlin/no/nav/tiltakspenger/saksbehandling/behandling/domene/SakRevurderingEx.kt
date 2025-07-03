package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.beregning.RevurderingIkkeBeregnet
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDate

private typealias HentSaksopplysninger = suspend (Periode) -> Saksopplysninger

suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    clock: Clock,
    hentSaksopplysninger: suspend (fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> Saksopplysninger,
): Pair<Sak, Revurdering> {
    val saksbehandler = kommando.saksbehandler
    krevSaksbehandlerRolle(saksbehandler)

    val hentSaksopplysninger: HentSaksopplysninger = { periode: Periode ->
        hentSaksopplysninger(
            this.fnr,
            kommando.correlationId,
            periode,
        )
    }

    val revurdering = when (kommando.revurderingType) {
        RevurderingType.STANS -> startStans(saksbehandler, hentSaksopplysninger, clock)
        RevurderingType.INNVILGELSE -> startInnvilgelse(saksbehandler, hentSaksopplysninger, clock)
    }

    return Pair(
        copy(behandlinger = behandlinger.leggTilRevurdering(revurdering)),
        revurdering,
    )
}

private suspend fun Sak.startStans(
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysninger,
    clock: Clock,
): Revurdering {
    // TODO abn: hva (om noe) må vi fikse for å kunne fjerne denne restriksjonen?
    //  Svar jah: Vi bør heller sjekke at man ikke kan stanse over hull. Dvs. man kan kunne stanse siste sammenhengende innvilgelsesperiode som ikke er utbetalt. For det første gjetter jeg på vi ikke kommer i den situasjonen og for det andre støtter ikke brevet vårt et slikt scenarie.
    require(this.vedtaksliste.erInnvilgelseSammenhengende) {
        "Kan ikke opprette stans-revurdering dersom vi har hull i vedtaksperiodene. sakId=${this.id}"
    }

    // TODO jah: Denne må endres sammen med sjekken over.
    val saksopplysningsperiode = this.vedtaksliste.innvilgetTidslinje.totalPeriode

    return Revurdering.opprettStans(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        saksopplysninger = hentSaksopplysninger(saksopplysningsperiode),
        clock = clock,
    )
}

private suspend fun Sak.startInnvilgelse(
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysninger,
    clock: Clock,
): Revurdering {
    val sisteBehandling = hentSisteInnvilgetBehandling()

    requireNotNull(sisteBehandling) {
        "Må ha en tidligere vedtatt innvilgelse for å kunne revurdere innvilgelse"
    }

    return Revurdering.opprettInnvilgelse(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        saksopplysninger = hentSaksopplysninger(sisteBehandling.saksopplysningsperiode),
        clock = clock,
    )
}

fun Sak.sendRevurderingTilBeslutning(
    kommando: RevurderingTilBeslutningKommando,
    clock: Clock,
): Either<KanIkkeSendeTilBeslutter, Revurdering> {
    krevSaksbehandlerRolle(kommando.saksbehandler)

    val behandling = this.hentBehandling(kommando.behandlingId)

    require(behandling is Revurdering) {
        "Behandlingen må være en revurdering, men er: ${behandling?.behandlingstype}"
    }

    return when (kommando) {
        is RevurderingInnvilgelseTilBeslutningKommando -> {
            val beregning = beregnRevurderingInnvilgelse(kommando).getOrElse {
                when (it) {
                    is RevurderingIkkeBeregnet.IngenEndring -> null
                    is RevurderingIkkeBeregnet.IngenTidligereBeregninger -> null
                    is RevurderingIkkeBeregnet.Tilbakekreving ->
                        return KanIkkeSendeTilBeslutter.StøtterIkkeTilbakekreving.left()
                }
            }

            behandling.tilBeslutning(
                kommando = kommando,
                beregning = beregning,
                clock = clock,
            )
        }

        is RevurderingStansTilBeslutningKommando -> {
            validerStansDato(kommando.stansFraOgMed)

            behandling.stansTilBeslutning(
                kommando = kommando.copy(sisteDagSomGirRett = sisteDagSomGirRett),
                clock = clock,
            )
        }
    }
}

fun Sak.validerStansDato(stansDato: LocalDate?) {
    if (stansDato == null) {
        throw IllegalArgumentException("Stansdato er ikke satt")
    }

    this.førsteLovligeStansdato()?.also {
        if (stansDato.isBefore(it)) {
            throw IllegalArgumentException("Angitt stansdato $stansDato er før første lovlige stansdato $it")
        }
    }

    if (stansDato.isBefore(this.førsteDagSomGirRett)) {
        throw IllegalArgumentException("Kan ikke starte revurdering ($stansDato) før første innvilgetdato (${this.førsteDagSomGirRett})")
    }

    if (stansDato.isAfter(this.sisteDagSomGirRett)) {
        throw IllegalArgumentException("Kan ikke starte revurdering med stansdato ($stansDato) etter siste innvilgetdato (${this.sisteDagSomGirRett})")
    }
}
