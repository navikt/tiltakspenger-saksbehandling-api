package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.KanIkkeOppretteBehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.OpprettBehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.OpprettMeldekortbehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.OpprettRevurderingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.OpprettSøknadsbehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.opprettBehandlingFraKlage
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
    ): Either<KanIkkeOppretteBehandlingForKlage, OpprettBehandlingForKlageResultat> {
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
    ): Either<KanIkkeOppretteBehandlingForKlage, OpprettBehandlingForKlageResultat> {
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
            it.tilKanIkkeOppretteBehandlingForKlage()
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

sealed interface OpprettBehandlingForKlageResultat {
    val sak: Sak

    data class RammebehandlingOpprettet(
        override val sak: Sak,
        val rammebehandling: Rammebehandling,
    ) : OpprettBehandlingForKlageResultat

    data class MeldekortbehandlingOpprettet(
        override val sak: Sak,
        val meldekortbehandling: MeldekortUnderBehandling,
        val kjedeId: MeldeperiodeKjedeId,
    ) : OpprettBehandlingForKlageResultat
}

sealed interface KanIkkeOppretteBehandlingForKlage {
    data class KanIkkeOppretteMeldekortbehandling(
        val underliggende: no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortbehandling,
    ) : KanIkkeOppretteBehandlingForKlage

    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String?,
        val faktiskSaksbehandler: String,
    ) : KanIkkeOppretteBehandlingForKlage

    data class FinnesÅpenBehandling(val behandlingId: BehandlingId) : KanIkkeOppretteBehandlingForKlage
}

private fun KanIkkeOppretteBehandlingFraKlage.tilKanIkkeOppretteBehandlingForKlage(): KanIkkeOppretteBehandlingForKlage {
    return when (this) {
        is KanIkkeOppretteBehandlingFraKlage.SaksbehandlerMismatch -> KanIkkeOppretteBehandlingForKlage.SaksbehandlerMismatch(
            forventetSaksbehandler = forventetSaksbehandler,
            faktiskSaksbehandler = faktiskSaksbehandler,
        )

        is KanIkkeOppretteBehandlingFraKlage.FinnesÅpenBehandling -> KanIkkeOppretteBehandlingForKlage.FinnesÅpenBehandling(
            behandlingId = behandlingId,
        )
    }
}
