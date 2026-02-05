package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock
import java.time.LocalDateTime

/** Generell funksjon for å starte revurdering (stans, forlengelse, omgjøring og opphør) */
suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    clock: Clock,
    hentSaksopplysninger: HentSaksopplysninger,
): Either<KunneIkkeStarteRevurdering, Pair<Sak, Revurdering>> {
    val nå = nå(clock)
    val klagebehandling: Klagebehandling? = kommando.klagebehandlingId?.let {
        hentKlagebehandling(it).oppdaterRammebehandlingId(
            rammebehandlingId = kommando.revurderingId,
            saksbehandler = kommando.saksbehandler,
            sistEndret = nå,
        )
    }
    val revurdering = when (kommando.revurderingType) {
        StartRevurderingType.STANS -> startRevurderingStans(
            revurderingId = kommando.revurderingId,
            saksbehandler = kommando.saksbehandler,
            hentSaksopplysninger = hentSaksopplysninger,
            correlationId = kommando.correlationId,
            opprettet = nå,
        )

        StartRevurderingType.INNVILGELSE -> startRevurderingInnvilgelse(
            revurderingId = kommando.revurderingId,
            saksbehandler = kommando.saksbehandler,
            hentSaksopplysninger = hentSaksopplysninger,
            correlationId = kommando.correlationId,
            opprettet = nå,
            klagebehandling = klagebehandling,
        )

        StartRevurderingType.OMGJØRING -> startRevurderingOmgjøring(
            revurderingId = kommando.revurderingId,
            saksbehandler = kommando.saksbehandler,
            hentSaksopplysninger = hentSaksopplysninger,
            correlationId = kommando.correlationId,
            rammevedtakIdSomOmgjøres = kommando.vedtakIdSomOmgjøres!!,
            opprettet = nå,
            klagebehandling = klagebehandling,
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
    revurderingId: BehandlingId = BehandlingId.random(),
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysninger,
    correlationId: CorrelationId,
    opprettet: LocalDateTime,
): Revurdering {
    return Revurdering.opprettStans(
        revurderingId = revurderingId,
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

        opprettet = opprettet,
    )
}

// TODO forlengelse jah: Konverter til forlengelse.
private suspend fun Sak.startRevurderingInnvilgelse(
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysninger,
    correlationId: CorrelationId,
    opprettet: LocalDateTime,
    klagebehandling: Klagebehandling?,
    revurderingId: BehandlingId = BehandlingId.random(),
): Revurdering {
    require(harFørstegangsvedtak) {
        "Må ha en tidligere vedtatt innvilgelse for å kunne revurdere"
    }
    return Revurdering.opprettInnvilgelse(
        revurderingId = revurderingId,
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
        opprettet = opprettet,
        klagebehandling = klagebehandling,
    )
}

private suspend fun Sak.startRevurderingOmgjøring(
    saksbehandler: Saksbehandler,
    hentSaksopplysninger: HentSaksopplysninger,
    correlationId: CorrelationId,
    rammevedtakIdSomOmgjøres: VedtakId,
    klagebehandling: Klagebehandling?,
    opprettet: LocalDateTime,
    revurderingId: BehandlingId = BehandlingId.random(),
): Either<KunneIkkeOppretteOmgjøring, Revurdering> {
    val gjeldendeRammevedtak: Rammevedtak = this.hentRammevedtakForId(rammevedtakIdSomOmgjøres)

    return Revurdering.opprettOmgjøring(
        revurderingId = revurderingId,
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
        opprettet = opprettet,
        omgjørRammevedtak = gjeldendeRammevedtak,
        klagebehandling = klagebehandling,
    )
}
