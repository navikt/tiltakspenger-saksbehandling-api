package no.nav.tiltakspenger.saksbehandling.benk.service

import arrow.core.getOrElse
import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevSaksbehandlerEllerBeslutterRolle

class BenkOversiktService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val benkOversiktRepo: BenkOversiktRepo,
) {
    val logger = KotlinLogging.logger { }

    suspend fun hentBenkOversikt(
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): BenkOversikt {
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
        val benkOversikt = benkOversiktRepo.hentÅpneBehandlinger()

        if (benkOversikt.isEmpty()) return benkOversikt
        val tilganger = tilgangsstyringService.harTilgangTilPersoner(
            fnrListe = benkOversikt.fødselsnummere().toNonEmptyListOrNull()!!,
            roller = saksbehandler.roller,
            correlationId = correlationId,
        ).getOrElse { throw IllegalStateException("Feil ved henting av tilganger") }
        return filtrerIkkeTilgang(benkOversikt, tilganger, saksbehandler, logger)
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
