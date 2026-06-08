package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.KanIkkeOppretteBehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.OpprettBehandlingForKlageResultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.OpprettBehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.OpprettMeldekortbehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.OpprettRevurderingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.OpprettSøknadsbehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.opprettBehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class OpprettBehandlingForKlageService(
    private val sakService: SakService,
    private val behandleSøknadPåNyttService: BehandleSøknadPåNyttService,
    private val startRevurderingService: StartRevurderingService,
    private val opprettMeldekortbehandlingService: OpprettMeldekortbehandlingService,
) {
    suspend fun opprett(
        kommando: OpprettBehandlingFraKlageKommando,
    ): Either<KanIkkeOppretteBehandlingFraKlage, OpprettBehandlingForKlageResultat> {
        return when (kommando) {
            is OpprettSøknadsbehandlingFraKlageKommando -> opprettBehandling(
                kommando = OpprettSøknadsbehandlingFraKlageKommando(
                    sakId = kommando.sakId,
                    saksbehandler = kommando.saksbehandler,
                    klagebehandlingId = kommando.klagebehandlingId,
                    søknadId = kommando.søknadId,
                    correlationId = kommando.correlationId,
                ),
            )

            is OpprettRevurderingFraKlageKommando -> opprettBehandling(
                kommando = OpprettRevurderingFraKlageKommando(
                    sakId = kommando.sakId,
                    saksbehandler = kommando.saksbehandler,
                    klagebehandlingId = kommando.klagebehandlingId,
                    type = when (kommando.type) {
                        OpprettRevurderingFraKlageKommando.Type.INNVILGELSE -> OpprettRevurderingFraKlageKommando.Type.INNVILGELSE
                        OpprettRevurderingFraKlageKommando.Type.OMGJØRING -> OpprettRevurderingFraKlageKommando.Type.OMGJØRING
                    },
                    correlationId = kommando.correlationId,
                    vedtakIdSomOmgjøres = kommando.vedtakIdSomOmgjøres,
                ),
            )

            is OpprettMeldekortbehandlingFraKlageKommando -> opprettBehandling(
                kommando = OpprettMeldekortbehandlingFraKlageKommando(
                    sakId = kommando.sakId,
                    saksbehandler = kommando.saksbehandler,
                    klagebehandlingId = kommando.klagebehandlingId,
                    correlationId = kommando.correlationId,
                    kjedeId = kommando.kjedeId,
                ),
            )
        }
    }

    private suspend fun opprettBehandling(
        kommando: OpprettBehandlingFraKlageKommando,
    ): Either<KanIkkeOppretteBehandlingFraKlage, OpprettBehandlingForKlageResultat> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.opprettBehandlingFraKlage(
            kommando = kommando,
            // Har ansvar for å lagre rammebehandling og endringer på klagen + sideeffekter som statistikk og metrikker.
            opprettSøknadsbehandling = behandleSøknadPåNyttService::startSøknadsbehandlingPåNytt,
            // Har ansvar for å lagre revurdering og endringer på klagen + sideeffekter som statistikk og metrikker.
            opprettRevurdering = startRevurderingService::startRevurdering,
            // Har ansvar for å lagre meldekortbehandling og endringer på klagen + sideeffekter som statistikk og metrikker.
            opprettMeldekortbehandling = { kommando, _ ->
                opprettMeldekortbehandlingService.opprettBehandling(kommando).getOrThrow()
            },
        ).mapLeft {
            return it.left()
        }.map { (oppdatertSak, behandling) ->
            when (behandling) {
                is Rammebehandling -> OpprettBehandlingForKlageResultat.RammebehandlingOpprettet(
                    sak = oppdatertSak,
                    rammebehandling = behandling,
                )

                is MeldekortUnderBehandling -> OpprettBehandlingForKlageResultat.MeldekortbehandlingOpprettet(
                    sak = oppdatertSak,
                    meldekortbehandling = behandling,
                    kjedeId = (kommando as OpprettMeldekortbehandlingFraKlageKommando).kjedeId,
                )

                else -> throw IllegalStateException("Ukjent behandlingstype ${behandling::class} for å opprette behandling fra klage")
            }
        }
    }
}
