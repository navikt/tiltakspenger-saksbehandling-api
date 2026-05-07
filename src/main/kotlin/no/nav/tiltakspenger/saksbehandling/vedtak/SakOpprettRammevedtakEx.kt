package no.nav.tiltakspenger.saksbehandling.vedtak

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjortAvRammevedtak
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.genererStønadsstatistikkForRammevedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.Satstype
import java.time.Clock

sealed interface OpprettRammevedtakFeil {
    data class RammebehandlingIkkeVedtatt(
        val rammebehandlingId: RammebehandlingId,
        val status: Rammebehandlingsstatus,
    ) : OpprettRammevedtakFeil {
        override fun toString(): String =
            "Krever behandlingsstatus VEDTATT når vi skal opprette et vedtak. " +
                "rammebehandlingId=$rammebehandlingId, status=$status"
    }

    data class UgyldigKlagebehandlingStatus(
        val klagebehandlingId: KlagebehandlingId,
        val status: Klagebehandlingsstatus,
    ) : OpprettRammevedtakFeil {
        override fun toString(): String =
            "Krever at klagebehandling har status VEDTATT/FERDIGSTILT når vi skal opprette et vedtak. " +
                "klagebehandlingId=$klagebehandlingId, status=$status"
    }

    data class UgyldigOmgjøring(
        val feil: RammevedtakValideringFeil,
    ) : OpprettRammevedtakFeil {
        override fun toString(): String = feil.toString()
    }
}

fun Sak.opprettRammevedtak(
    rammebehandling: Rammebehandling,
    clock: Clock,
): Either<OpprettRammevedtakFeil, Triple<Sak, Rammevedtak, Statistikkhendelser>> {
    if (rammebehandling.status != Rammebehandlingsstatus.VEDTATT) {
        return OpprettRammevedtakFeil.RammebehandlingIkkeVedtatt(
            rammebehandlingId = rammebehandling.id,
            status = rammebehandling.status,
        ).left()
    }

    val klagebehandling: Klagebehandling? = rammebehandling.klagebehandling

    if (klagebehandling != null &&
        klagebehandling.status != Klagebehandlingsstatus.VEDTATT &&
        klagebehandling.status != Klagebehandlingsstatus.FERDIGSTILT
    ) {
        return OpprettRammevedtakFeil.UgyldigKlagebehandlingStatus(
            klagebehandlingId = klagebehandling.id,
            status = klagebehandling.status,
        ).left()
    }

    val vedtakId = VedtakId.random()
    val opprettet = nå(clock)

    val utbetaling: VedtattUtbetaling? = rammebehandling.utbetaling?.let {
        VedtattUtbetaling(
            id = UtbetalingId.random(),
            vedtakId = vedtakId,
            sakId = this.id,
            saksnummer = this.saksnummer,
            fnr = this.fnr,
            brukerNavkontor = it.navkontor,
            opprettet = opprettet,
            saksbehandler = rammebehandling.saksbehandler!!,
            beslutter = rammebehandling.beslutter!!,
            beregning = it.beregning,
            forrigeUtbetalingId = this.utbetalinger.lastOrNull()?.id,
            statusMetadata = Forsøkshistorikk.opprett(clock = clock),
            satstype = Satstype.DAGLIG,
            sendtTilUtbetaling = null,
            status = null,
        )
    }

    val vedtak = Rammevedtak(
        id = vedtakId,
        opprettet = opprettet,
        sakId = this.id,
        rammebehandling = rammebehandling,
        periode = rammebehandling.vedtaksperiode!!,
        omgjortAvRammevedtak = OmgjortAvRammevedtak.empty,
        utbetaling = utbetaling,
        vedtaksdato = null,
        journalpostId = null,
        journalføringstidspunkt = null,
        distribusjonId = null,
        distribusjonstidspunkt = null,
        sendtTilDatadeling = null,
        brevJson = null,
    )

    this.rammevedtaksliste.validerOmgjøringerVedNyttVedtak(vedtak).onLeft {
        return OpprettRammevedtakFeil.UgyldigOmgjøring(it).left()
    }

    val oppdatertSak = this.leggTilRammevedtak(vedtak).oppdaterRammebehandling(rammebehandling)
    val statistikkhendelser = Statistikkhendelser(
        listOfNotNull(
            vedtak.genererSaksstatistikk(),
            if (vedtak.rammebehandlingsresultat is Søknadsbehandlingsresultat.Avslag) {
                null
            } else {
                genererStønadsstatistikkForRammevedtak(vedtak)
            },
        ),
    )
    return Triple(oppdatertSak, vedtak, statistikkhendelser).right()
}
