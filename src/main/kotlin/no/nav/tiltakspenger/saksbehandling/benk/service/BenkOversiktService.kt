package no.nav.tiltakspenger.saksbehandling.benk.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo

class BenkOversiktService(
    private val benkOversiktRepo: BenkOversiktRepo,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }

    suspend fun hentBenkOversikt(
        command: HentÅpneBehandlingerCommand,
        saksbehandlerToken: String,
        saksbehandler: Saksbehandler,
    ): BenkOversikt {
        val benkOversikt = benkOversiktRepo.hentÅpneBehandlinger(command)

        if (benkOversikt.isEmpty()) return benkOversikt
        val tilganger = tilgangskontrollService.harTilgangTilPersoner(
            fnrs = benkOversikt.fødselsnummere(),
            saksbehandlerToken = saksbehandlerToken,
            saksbehandler = saksbehandler,
        )
        return filtrerIkkeTilgang(benkOversikt, tilganger, command.saksbehandler, logger)
    }
}

fun filtrerIkkeTilgang(
    benkOversikt: BenkOversikt,
    tilganger: Map<Fnr, Boolean>,
    saksbehandler: Saksbehandler,
    logger: KLogger,
): BenkOversikt = benkOversikt.filtrerOversikt {
    val harTilgang = tilganger[it.fnr]
    if (harTilgang == null) {
        logger.debug { "tilgangsstyring: Filtrerte vekk bruker fra benk for saksbehandler $saksbehandler. Kunne ikke avgjøre om hen har tilgang. Se sikkerlogg for mer kontekst." }
        Sikkerlogg.debug { "tilgangsstyring: Filtrerte vekk bruker ${it.fnr.verdi} fra benk for saksbehandler $saksbehandler. Kunne ikke avgjøre om hen har tilgang." }
    }
    if (harTilgang == false) {
        logger.debug { "tilgangsstyring: Filtrerte vekk bruker fra benk for saksbehandler $saksbehandler. Saksbehandler har ikke tilgang. Se sikkerlogg for mer kontekst." }
        Sikkerlogg.debug { "tilgangsstyring: Filtrerte vekk bruker ${it.fnr.verdi} fra benk for saksbehandler $saksbehandler. Saksbehandler har ikke tilgang." }
    }
    harTilgang == true
}
