package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.oppdaterMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService

/**
 * Har ansvar for å ta imot et utfylt meldekort og lagre det.
 */
class OppdaterMeldekortService(
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakService: SakService,
    private val simulerService: SimulerService,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun oppdaterMeldekort(
        kommando: OppdaterMeldekortKommando,
    ): Either<KanIkkeOppdatereMeldekort, Pair<Sak, MeldekortUnderBehandling>> {
        val sak = hentSak(kommando)
        return sak.oppdaterMeldekort(
            kommando = kommando,
            simuler = { behandling ->
                simulerService.simulerMeldekort(
                    behandling = behandling,
                    forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
                    meldeperiodeKjeder = sak.meldeperiodeKjeder,
                    brukersNavkontor = { behandling.navkontor },
                )
            },
        ).map { (sak, meldekort, simulering) ->
            meldekortBehandlingRepo.oppdater(meldekort, simulering)
            logger.info { "Meldekort under behandling med id ${meldekort.id} oppdatert. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            Pair(sak, meldekort)
        }
    }

    // TODO jah: Kopiert til [SendMeldekortTilBeslutterService] - lage noe felles?
    private suspend fun hentSak(
        kommando: OppdaterMeldekortKommando,
    ): Sak {
        krevSaksbehandlerRolle(kommando.saksbehandler)

        // Denne sjekker at saksbehandler har tilgang til personen og at den har en av rollene SAKSBEHANDLER eller BESLUTTER
        val sak = sakService.sjekkTilgangOgHentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId)

        val meldekortbehandling = sak.hentMeldekortBehandling(kommando.meldekortId)!!
        val meldeperiode = meldekortbehandling.meldeperiode
        if (!sak.erSisteVersjonAvMeldeperiode(meldeperiode)) {
            throw IllegalStateException("Kan ikke iverksette meldekortbehandling hvor meldeperioden (${meldeperiode.versjon}) ikke er siste versjon av meldeperioden i saken. sakId: ${sak.id}, meldekortId: ${meldekortbehandling.id}")
        }
        return sak
    }
}
