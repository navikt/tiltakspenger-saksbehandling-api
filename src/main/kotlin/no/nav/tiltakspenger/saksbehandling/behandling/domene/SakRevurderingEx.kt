package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

/**
 * Generell funksjon for å starte revurdering (både innvilgelse og stans)
 */
suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    clock: Clock,
    hentSaksopplysninger: HentSaksopplysninger,
): Pair<Sak, Revurdering> {
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
        copy(rammebehandlinger = rammebehandlinger.leggTilRevurdering(revurdering)),
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
    require(this.rammevedtaksliste.erInnvilgelseSammenhengende) {
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
