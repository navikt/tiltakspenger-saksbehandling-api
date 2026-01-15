package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock

/** Generell funksjon for å starte revurdering (stans, forlengelse, omgjøring og opphør) */
suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    clock: Clock,
    hentSaksopplysninger: HentSaksopplysninger,
): Either<KunneIkkeStarteRevurdering, Pair<Sak, Revurdering>> {
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

        RevurderingType.OMGJØRING -> startRevurderingOmgjøring(
            saksbehandler = kommando.saksbehandler,
            hentSaksopplysninger = hentSaksopplysninger,
            correlationId = kommando.correlationId,
            rammevedtakIdSomOmgjøres = kommando.vedtakIdSomOmgjøres!!,
            clock = clock,
        ).getOrElse {
            return KunneIkkeStarteRevurdering.Omgjøring(it).left()
        }
    }
    return Pair(
        this.leggTilRevurdering(revurdering),
        revurdering,
    ).right()
}

sealed interface KunneIkkeStarteRevurdering {
    data class Omgjøring(val årsak: KunneIkkeOppretteOmgjøring) : KunneIkkeStarteRevurdering
}

private suspend fun Sak.startRevurderingStans(
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysninger,
    correlationId: CorrelationId,
    clock: Clock,
): Revurdering {
    return Revurdering.opprettStans(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        saksopplysninger = hentSaksopplysninger(
            fnr,
            correlationId,
            this.tiltaksdeltakelserDetErSøktTiltakspengerFor,
            // TODO jah: På sikt er det mer presist at saksbehandler velger denne når hen starter en stans.
            //  Vi kan begrense denne litt mer ved å fjerne de tiltaksdeltakelsene det ikke er innvilget for, men vi kan utsette det til etter satsingsperioden.
            this.tiltaksdeltakelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.tiltaksdeltakerId }.distinct(),
            false,
        ),

        clock = clock,
    )
}

// TODO forlengelse jah: Konverter til forlengelse.
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
            this.tiltaksdeltakelserDetErSøktTiltakspengerFor,
            // TODO jah: På sikt er det mer presist at saksbehandler velger disse når hen starter en revurdering innvilgelse.
            //  Det er vanskelig å begrense denne så lenge vi ikke vet på forhånd om dette er en revurdering av tidligere innvilget perioder, forlengelse eller en kombinasjon.
            this.tiltaksdeltakelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.tiltaksdeltakerId }.distinct(),
            false,
        ),
        clock = clock,
    )
}

private suspend fun Sak.startRevurderingOmgjøring(
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysninger,
    correlationId: CorrelationId,
    rammevedtakIdSomOmgjøres: VedtakId,
    clock: Clock,
): Either<KunneIkkeOppretteOmgjøring, Revurdering> {
    require(this.erRammevedtakGjeldendeForHeleSinPeriode(rammevedtakIdSomOmgjøres)) {
        "I første versjon, kan man kun omgjøre et (delvis) innvilget rammevedtak som er gjeldende for hele sin periode."
    }
    val gjeldendeRammevedtak: Rammevedtak = this.hentRammevedtakForId(rammevedtakIdSomOmgjøres)
    return Revurdering.opprettOmgjøring(
        saksbehandler = saksbehandler,
        saksopplysninger = hentSaksopplysninger(
            fnr,
            correlationId,
            this.tiltaksdeltakelserDetErSøktTiltakspengerFor,
            // TODO jah: På sikt er det mer presist at saksbehandler velger disse når hen starter en revurdering innvilgelse.
            //  Det er vanskelig å begrense denne så lenge vi ikke vet på forhånd om dette er en revurdering av tidligere innvilget perioder, forlengelse eller en kombinasjon.
            this.tiltaksdeltakelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.tiltaksdeltakerId }.distinct(),
            false,
        ),
        clock = clock,
        omgjørRammevedtak = gjeldendeRammevedtak,
    )
}
