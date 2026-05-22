package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.KanIkkeOppretteRammebehandlingFraKlage
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
        kommando: OpprettBehandlingForKlageKommando,
    ): Either<KanIkkeOppretteBehandlingForKlage, OpprettBehandlingForKlageResultat> {
        return when (kommando) {
            is OpprettSøknadsbehandlingForKlageKommando -> opprettRammebehandling(
                kommando = OpprettSøknadsbehandlingFraKlageKommando(
                    sakId = kommando.sakId,
                    saksbehandler = kommando.saksbehandler,
                    klagebehandlingId = kommando.klagebehandlingId,
                    søknadId = kommando.søknadId,
                    correlationId = kommando.correlationId,
                ),
            )

            is OpprettRevurderingForKlageKommando -> opprettRammebehandling(
                kommando = OpprettRevurderingFraKlageKommando(
                    sakId = kommando.sakId,
                    saksbehandler = kommando.saksbehandler,
                    klagebehandlingId = kommando.klagebehandlingId,
                    type = when (kommando.type) {
                        OpprettRevurderingForKlageKommando.Type.INNVILGELSE -> OpprettRevurderingFraKlageKommando.Type.INNVILGELSE
                        OpprettRevurderingForKlageKommando.Type.OMGJØRING -> OpprettRevurderingFraKlageKommando.Type.OMGJØRING
                    },
                    correlationId = kommando.correlationId,
                    vedtakIdSomOmgjøres = kommando.vedtakIdSomOmgjøres,
                ),
            )

            is OpprettMeldekortbehandlingForKlageKommando -> opprettMeldekortbehandling(kommando)
        }
    }

    private suspend fun opprettRammebehandling(
        kommando: no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.OpprettRammebehandlingFraKlageKommando,
    ): Either<KanIkkeOppretteBehandlingForKlage, OpprettBehandlingForKlageResultat.RammebehandlingOpprettet> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.opprettBehandlingFraKlage(
            kommando = kommando,
            // Har ansvar for å lagre rammebehandling og endringer på klagen + sideeffekter som statistikk og metrikker.
            opprettSøknadsbehandling = behandleSøknadPåNyttService::startSøknadsbehandlingPåNytt,
            // Har ansvar for å lagre revurdering og endringer på klagen + sideeffekter som statistikk og metrikker.
            opprettRevurdering = startRevurderingService::startRevurdering,
        ).fold(
            ifLeft = { it.tilKanIkkeOppretteBehandlingForKlage().left() },
            ifRight = { (oppdatertSak, rammebehandling) ->
                OpprettBehandlingForKlageResultat.RammebehandlingOpprettet(
                    sak = oppdatertSak,
                    rammebehandling = rammebehandling,
                ).right()
            },
        )
    }

    private suspend fun opprettMeldekortbehandling(
        kommando: OpprettMeldekortbehandlingForKlageKommando,
    ): Either<KanIkkeOppretteBehandlingForKlage, OpprettBehandlingForKlageResultat.MeldekortbehandlingOpprettet> {
        val (sakMedMeldekortbehandling, meldekortbehandling) = opprettMeldekortbehandlingService.opprettBehandling(
            OpprettMeldekortbehandlingService.OpprettMeldekortbehandlingKommando(
                sakId = kommando.sakId,
                kjedeId = kommando.kjedeId,
                saksbehandler = kommando.saksbehandler,
                klagebehandlingId = kommando.klagebehandlingId,
                correlationId = kommando.correlationId,
            ),
        ).fold(
            ifLeft = { return KanIkkeOppretteBehandlingForKlage.KanIkkeOppretteMeldekortbehandling(it).left() },
            ifRight = { it },
        )

        return OpprettBehandlingForKlageResultat.MeldekortbehandlingOpprettet(
            sak = sakMedMeldekortbehandling,
            meldekortbehandling = meldekortbehandling as MeldekortUnderBehandling,
            kjedeId = kommando.kjedeId,
        ).right()
    }
}

sealed interface OpprettBehandlingForKlageKommando {
    val sakId: SakId
    val saksbehandler: Saksbehandler
    val klagebehandlingId: KlagebehandlingId
    val correlationId: CorrelationId
}

data class OpprettSøknadsbehandlingForKlageKommando(
    override val sakId: SakId,
    override val saksbehandler: Saksbehandler,
    override val klagebehandlingId: KlagebehandlingId,
    val søknadId: SøknadId,
    override val correlationId: CorrelationId,
) : OpprettBehandlingForKlageKommando

data class OpprettRevurderingForKlageKommando(
    override val sakId: SakId,
    override val saksbehandler: Saksbehandler,
    override val klagebehandlingId: KlagebehandlingId,
    val type: Type,
    override val correlationId: CorrelationId,
    val vedtakIdSomOmgjøres: VedtakId?,
) : OpprettBehandlingForKlageKommando {
    enum class Type {
        INNVILGELSE,
        OMGJØRING,
    }
}

data class OpprettMeldekortbehandlingForKlageKommando(
    override val sakId: SakId,
    override val saksbehandler: Saksbehandler,
    override val klagebehandlingId: KlagebehandlingId,
    val kjedeId: MeldeperiodeKjedeId,
    override val correlationId: CorrelationId,
) : OpprettBehandlingForKlageKommando

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

private fun KanIkkeOppretteRammebehandlingFraKlage.tilKanIkkeOppretteBehandlingForKlage(): KanIkkeOppretteBehandlingForKlage {
    return when (this) {
        is KanIkkeOppretteRammebehandlingFraKlage.SaksbehandlerMismatch -> KanIkkeOppretteBehandlingForKlage.SaksbehandlerMismatch(
            forventetSaksbehandler = forventetSaksbehandler,
            faktiskSaksbehandler = faktiskSaksbehandler,
        )

        is KanIkkeOppretteRammebehandlingFraKlage.FinnesÅpenBehandling -> KanIkkeOppretteBehandlingForKlage.FinnesÅpenBehandling(
            behandlingId = behandlingId,
        )
    }
}
