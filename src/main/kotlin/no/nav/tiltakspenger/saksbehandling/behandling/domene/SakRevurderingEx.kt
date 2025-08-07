package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Innvilgelse.Utbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.beregning.RevurderingIkkeBeregnet
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDate

private typealias HentSaksopplysningerForPeriode = suspend (Periode) -> Saksopplysninger
private typealias HentNavkontor = suspend (Fnr) -> Navkontor

/**
 * Generell funksjon for å starte revurdering (både innvilgelse og stans)
 */
suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    clock: Clock,
    hentSaksopplysninger: HentSaksopplysninger,
): Pair<Sak, Revurdering> {
    val saksbehandler = kommando.saksbehandler
    krevSaksbehandlerRolle(saksbehandler)

    val hentSaksopplysninger: HentSaksopplysningerForPeriode = { periode: Periode ->
        hentSaksopplysninger(
            this.fnr,
            kommando.correlationId,
            periode,
        )
    }

    val revurdering = when (kommando.revurderingType) {
        RevurderingType.STANS -> startRevurderingStans(saksbehandler, hentSaksopplysninger, clock)
        RevurderingType.INNVILGELSE -> startRevurderingInnvilgelse(saksbehandler, hentSaksopplysninger, clock)
    }

    return Pair(
        copy(behandlinger = behandlinger.leggTilRevurdering(revurdering)),
        revurdering,
    )
}

private suspend fun Sak.startRevurderingStans(
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysningerForPeriode,
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

private suspend fun Sak.startRevurderingInnvilgelse(
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysningerForPeriode,
    clock: Clock,
): Revurdering {
    require(harFørstegangsvedtak) {
        "Må ha en tidligere vedtatt innvilgelse for å kunne revurdere"
    }
    val saksopplysningsperiode = this.saksopplysningsperiode.let { perioder ->
        Periode(
            perioder.minOf { it.fraOgMed },
            // Legger på et år for å matche søknadsbehandlingen enn så lenge. Dette vil kun være OK for forlengelse frem i tid, ikke forlengelse tilbake i tid eller omgjøring tilbake i tid.
            perioder.maxOf { it.tilOgMed }.plusYears(1),
        )
    }
    return Revurdering.opprettInnvilgelse(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        // TODO jah: På sikt vil vi dele innhenting i 1) tiltaksdeltagelser 2) velger revurderingsperiode 3) bruker revurderingsperiode som innhentingsperiode for resten av saksopplysnignene
        saksopplysninger = hentSaksopplysninger(saksopplysningsperiode),
        clock = clock,
    )
}

suspend fun Sak.sendRevurderingTilBeslutning(
    kommando: OppdaterRevurderingKommando,
    hentNavkontor: HentNavkontor,
    clock: Clock,
): Either<KanIkkeSendeTilBeslutter, Revurdering> {
    krevSaksbehandlerRolle(kommando.saksbehandler)

    val behandling = this.hentBehandling(kommando.behandlingId)

    require(behandling is Revurdering) {
        "Behandlingen må være en revurdering, men er: ${behandling?.behandlingstype}"
    }

    return when (kommando) {
        is OppdaterRevurderingKommando.Innvilgelse -> {
            val utbetaling = beregnRevurderingInnvilgelse(kommando).fold(
                ifLeft = {
                    when (it) {
                        is RevurderingIkkeBeregnet.IngenEndring -> null
                        is RevurderingIkkeBeregnet.StøtterIkkeTilbakekreving ->
                            return KanIkkeSendeTilBeslutter.StøtterIkkeTilbakekreving.left()
                    }
                },

                ifRight = {
                    Utbetaling(
                        beregning = it,
                        navkontor = hentNavkontor(this.fnr),
                    )
                },
            )

            behandling.innvilgelseTilBeslutning(
                kommando = kommando,
                utbetaling = utbetaling,
                clock = clock,
            )
        }

        is OppdaterRevurderingKommando.Stans -> {
            validerStansDato(kommando.stansFraOgMed)

            behandling.stansTilBeslutning(
                kommando = kommando,
                sisteDagSomGirRett = sisteDagSomGirRett!!,
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
