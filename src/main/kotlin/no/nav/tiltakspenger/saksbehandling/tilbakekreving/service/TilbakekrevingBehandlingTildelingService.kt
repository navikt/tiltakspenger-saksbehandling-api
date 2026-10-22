package no.nav.tiltakspenger.saksbehandling.tilbakekreving.service

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptySetOrThrow
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling.leggTilbake
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling.overta
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling.tildel
import java.time.Clock

class TilbakekrevingBehandlingTildelingService(
    private val sakService: SakService,
    private val tilbakekrevingBehandlingRepo: TilbakekrevingBehandlingRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {

    fun tildelBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, TilbakekrevingBehandling> {
        return tildelBehandlinger(
            TildelTilbakekrevingerKommando(
                saksbehandler = saksbehandler,
                tilbakekrevinger = nonEmptyListOf(
                    TildelTilbakekrevingerKommando.TilbakekrevingMedSakId(
                        tilbakekrevingId = tilbakekrevingId,
                        sakId = sakId,
                    ),
                ),
            ),
        ).single()
    }

    /**
     * Tildeler saksbehandler alle tilbakekrevingene i kommandoen, eller ingen av dem.
     * Alle tildelingene valideres før noe lagres, og lagringen skjer i én transaksjon.
     * Feiler én av dem, rulles hele tildelingen tilbake.
     */
    fun tildelBehandlinger(
        kommando: TildelTilbakekrevingerKommando,
    ): List<Pair<Sak, TilbakekrevingBehandling>> {
        val tildelte = kommando.tilbakekrevinger.map { (tilbakekrevingId, sakId) ->
            val (sak, behandling) = hentSakOgBehandling(sakId, tilbakekrevingId)
            oppdatertSak(sak, behandling.tildel(kommando.saksbehandler, clock))
        }

        sessionFactory.withTransactionContext { tx ->
            tildelte.forEach { (_, oppdatert) -> lagreTildeling(oppdatert, tx) }
        }

        return tildelte
    }

    private fun lagreTildeling(oppdatert: TilbakekrevingBehandling, tx: TransactionContext) {
        val harOppdatert = when (oppdatert.statusIntern) {
            TilbakekrevingBehandlingsstatusIntern.UNDER_FORHÅNDSVARSLING,
            TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING,
            ->
                tilbakekrevingBehandlingRepo.taBehandlingSaksbehandler(oppdatert, tx)

            TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING ->
                tilbakekrevingBehandlingRepo.taBehandlingBeslutter(oppdatert, tx)

            else -> throw IllegalStateException("Uventet status ${oppdatert.statusIntern} etter ta behandling for ${oppdatert.id}")
        }
        require(harOppdatert) {
            "Oppdatering av tilbakekrevingbehandling feilet for ${oppdatert.id}. En annen bruker kan ha endret behandlingen."
        }
    }

    fun overtaBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, TilbakekrevingBehandling> {
        val (sak, behandling) = hentSakOgBehandling(sakId, tilbakekrevingId)
        val oppdatert = behandling.overta(saksbehandler, clock)

        val harOppdatert = when (behandling.statusIntern) {
            TilbakekrevingBehandlingsstatusIntern.UNDER_FORHÅNDSVARSLING,
            TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING,
            ->
                tilbakekrevingBehandlingRepo.overtaSaksbehandler(oppdatert, behandling.saksbehandler!!)

            TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING ->
                tilbakekrevingBehandlingRepo.overtaBeslutter(oppdatert, behandling.beslutter!!)

            else -> throw IllegalStateException("Uventet status ${behandling.statusIntern} ved overta for $tilbakekrevingId")
        }
        require(harOppdatert) {
            "Oppdatering av tilbakekrevingbehandling feilet for $tilbakekrevingId. En annen bruker kan ha endret behandlingen."
        }

        return oppdatertSak(sak, oppdatert)
    }

    fun leggTilbakeBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, TilbakekrevingBehandling> {
        val (sak, behandling) = hentSakOgBehandling(sakId, tilbakekrevingId)
        val oppdatert = behandling.leggTilbake(saksbehandler, clock)

        val harOppdatert = when (behandling.statusIntern) {
            TilbakekrevingBehandlingsstatusIntern.UNDER_FORHÅNDSVARSLING,
            TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING,
            ->
                tilbakekrevingBehandlingRepo.leggTilbakeSaksbehandler(oppdatert, behandling.saksbehandler!!)

            TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING ->
                tilbakekrevingBehandlingRepo.leggTilbakeBeslutter(oppdatert, behandling.beslutter!!)

            else -> throw IllegalStateException("Uventet status ${behandling.statusIntern} ved legg tilbake for $tilbakekrevingId")
        }
        require(harOppdatert) {
            "Oppdatering av tilbakekrevingbehandling feilet for $tilbakekrevingId. En annen bruker kan ha endret behandlingen."
        }

        return oppdatertSak(sak, oppdatert)
    }

    private fun hentSakOgBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
    ): Pair<Sak, TilbakekrevingBehandling> {
        val sak = sakService.hentForSakId(sakId)
        val behandling = sak.tilbakekrevinger.singleOrNull { it.id == tilbakekrevingId }
            ?: throw IllegalArgumentException("Fant ikke tilbakekrevingbehandling med id $tilbakekrevingId for sak $sakId")
        return sak to behandling
    }

    private fun oppdatertSak(sak: Sak, oppdatert: TilbakekrevingBehandling): Pair<Sak, TilbakekrevingBehandling> {
        val oppdatertSak = sak.copy(
            tilbakekrevinger = sak.tilbakekrevinger.map { if (it.id == oppdatert.id) oppdatert else it },
        )
        return oppdatertSak to oppdatert
    }
}

data class TildelTilbakekrevingerKommando(
    val saksbehandler: Saksbehandler,
    val tilbakekrevinger: NonEmptyList<TilbakekrevingMedSakId>,
) {
    fun hentSakIder(): NonEmptySet<SakId> = tilbakekrevinger.map { it.sakId }.toNonEmptySetOrThrow()

    data class TilbakekrevingMedSakId(
        val tilbakekrevingId: TilbakekrevingId,
        val sakId: SakId,
    )
}
