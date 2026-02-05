package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.Tilgangsnektårsak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

class TilgangskontrollService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val sakService: SakService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
        saksbehandler: Saksbehandler,
    ) {
        try {
            val vurdering = tilgangsmaskinClient.harTilgangTilPerson(fnr, saksbehandlerToken)

            when (vurdering) {
                is Tilgangsvurdering.Avvist -> throw TilgangException(
                    vurdering.årsak.toTilgangsnektårsak(),
                    "Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person: ${vurdering.begrunnelse}",
                )

                Tilgangsvurdering.Godkjent -> Unit

                Tilgangsvurdering.GenerellFeilMotTilgangsmaskin -> throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent} - Generell feil mot tilgangsmaskinen")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun harTilgangTilPersonForSakId(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        try {
            val fnr = sakService.hentFnrForSakId(sakId)
            harTilgangTilPerson(fnr, saksbehandlerToken, saksbehandler)
        } catch (tilgangException: TilgangException) {
            throw tilgangException
        } catch (e: Exception) {
            log.error { "Noe gikk galt ved sjekk av tilgang for person for sakId $sakId: ${e.message}" }
            throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent}: ${e.message}}")
        }
    }

    suspend fun harTilgangTilPersonForSaksnummer(
        saksnummer: Saksnummer,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        try {
            val fnr = sakService.hentFnrForSaksnummer(saksnummer)
            harTilgangTilPerson(fnr, saksbehandlerToken, saksbehandler)
        } catch (tilgangException: TilgangException) {
            throw tilgangException
        } catch (e: Exception) {
            log.error { "Noe gikk galt ved sjekk av tilgang for person for saksnummer $saksnummer: ${e.message}" }
            throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent}: ${e.message}}")
        }
    }

    suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
        saksbehandler: Saksbehandler,
    ): Map<Fnr, Boolean> {
        try {
            val respons = tilgangsmaskinClient.harTilgangTilPersoner(fnrs, saksbehandlerToken)
            return respons.resultater.associate {
                // TODO jah: Trenger litt debug-logger på denne for å feilsøke tilgangsproblemer. Fjern når vi har bedre oversikt.
                if (!it.harTilgangTilPerson()) {
                    Sikkerlogg.debug { "Benk - saksbehandler har ikke tilgang til person. fnr={${it.brukerId}}. ansattId=${respons.ansattId}. status=${it.status}. detaljer=${it.detaljer}." }
                }
                Fnr.fromString(it.brukerId) to it.harTilgangTilPerson()
            }
        } catch (e: Exception) {
            log.error { "Noe gikk galt ved sjekk av tilgang for flere personer: ${e.message}" }
            throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler (flere brukere) ${saksbehandler.navIdent}: ${e.message}}")
        }
    }
}
