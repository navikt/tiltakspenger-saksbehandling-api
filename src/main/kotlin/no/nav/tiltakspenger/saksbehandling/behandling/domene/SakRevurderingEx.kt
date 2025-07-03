package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Innvilgelse.Utbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.beregning.RevurderingIkkeBeregnet
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDate

private typealias HentNavkontor = suspend (Fnr) -> Navkontor

/**
 * Generell funksjon for å starte revurdering (både innvilgelse og stans)
 */
suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    clock: Clock,
    hentSaksopplysninger: HentSaksopplysninger,
): Pair<Sak, Revurdering> {
    krevSaksbehandlerRolle(kommando.saksbehandler)
    val revurdering = when (kommando.revurderingType) {
        RevurderingType.STANS -> startRevurderingStans(
            saksbehandler = kommando.saksbehandler,
            hentSaksopplysninger = hentSaksopplysninger,
            correlationId = kommando.correlationId,
            clock = clock,
        )

        RevurderingType.INNVILGELSE -> startRevurderingInnvilgelse(
            saksbehandler = kommando.saksbehandler,
            hentSaksopplysninger = hentSaksopplysninger,
            correlationId = kommando.correlationId,
            clock = clock,
        )
    }
    return Pair(
        copy(behandlinger = behandlinger.leggTilRevurdering(revurdering)),
        revurdering,
    )
}

private suspend fun Sak.startRevurderingStans(
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysninger,
    correlationId: CorrelationId,
    clock: Clock,
): Revurdering {
    // TODO abn: hva (om noe) må vi fikse for å kunne fjerne denne restriksjonen?
    //  Svar jah: Vi bør heller sjekke at man ikke kan stanse over hull. Dvs. man kan kunne stanse siste sammenhengende innvilgelsesperiode som ikke er utbetalt. For det første gjetter jeg på vi ikke kommer i den situasjonen og for det andre støtter ikke brevet vårt et slikt scenarie.
    require(this.vedtaksliste.erInnvilgelseSammenhengende) {
        "Kan ikke opprette stans-revurdering dersom vi har hull i vedtaksperiodene. sakId=${this.id}"
    }
    return Revurdering.opprettStans(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        saksopplysninger = hentSaksopplysninger(
            fnr,
            correlationId,
            this.tiltaksdeltagelserDetErSøktTiltakspengerFor,
            // TODO jah: På sikt er det mer presist at saksbehandler velger denne når hen starter en stans.
            //  Vi kan begrense denne litt mer ved å fjerne de tiltaksdeltagelsene det ikke er innvilget for, men vi kan utsette det til etter satsingsperioden.
            this.tiltaksdeltagelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.id }.distinct(),
            false,
        ),
        clock = clock,
    )
}

private suspend fun Sak.startRevurderingInnvilgelse(
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysninger,
    correlationId: CorrelationId,
    clock: Clock,
): Revurdering {
    require(harFørstegangsvedtak) {
        "Må ha en tidligere vedtatt innvilgelse for å kunne revurdere"
    }
    return Revurdering.opprettInnvilgelse(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        saksopplysninger = hentSaksopplysninger(
            fnr,
            correlationId,
            this.tiltaksdeltagelserDetErSøktTiltakspengerFor,
            // TODO jah: På sikt er det mer presist at saksbehandler velger disse når hen starter en revurdering innvilgelse.
            //  Det er vanskelig å begrense denne så lenge vi ikke vet på forhånd om dette er en revurdering av tidligere innvilget perioder, forlengelse eller en kombinasjon.
            this.tiltaksdeltagelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.id }.distinct(),
            false,
        ),
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
