package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkFane
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentBenkKommando
import no.nav.tiltakspenger.saksbehandling.benk.domene.tilSortering
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto.BenkBehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto.BenkKlagebehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto.BenkMeldekortTypeDTO
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto.BenkRevurderingResultatDTO
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto.BenkSøknadsbehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto.BenkSøknadstypeDTO
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto.BenkTilbakekrevingKildeDTO
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto.BenkTilbakekrevingStatusDTO
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto.tilDomene
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.benk.service.BenkService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling

private const val PATH = "/benk"

private const val FEIL_UGYLDIGE_FILTERVERDIER = "Noen av filterverdiene kunne ikke tolkes, så standardvisningen brukes"
private const val FEIL_UKJENT_FANE = "Fanen finnes ikke, så søknadsfanen vises"

private val logger = KotlinLogging.logger {}

/**
 * Benk v2.
 *
 * Én rute per fane, og hvert kall svarer med fanen pluss antallet i alle fanene.
 * Frontenden trenger begge deler for å tegne siden, og ville ellers måtte gjøre to kall for hvert fanebytte.
 *
 * Fanen ligger i url-en, og filterverdiene er typede enums i bodyen.
 * Begge kommer fra en url brukeren kan redigere, så en skrivefeil skal gi en standardvisning med `error` satt i responsen — ikke en 400 og en tom side.
 */
fun Route.hentBenkRoute(
    benkService: BenkService,
) {
    post(PATH) { søknader(benkService) }
    post("$PATH/soknader") { søknader(benkService) }
    post("$PATH/revurderinger") { revurderinger(benkService) }
    post("$PATH/meldekort") { meldekort(benkService) }
    post("$PATH/klage") { klage(benkService) }
    post("$PATH/tilbakekreving") { tilbakekreving(benkService) }

    // Catch-all for feilskrevne faner i url-en — svarer med søknadsfanen og error satt.
    post("$PATH/{...}") { svarMedSøknader(benkService, HentSøknaderBody(), FEIL_UKJENT_FANE) }
}

private suspend fun RoutingContext.søknader(benkService: BenkService) {
    val (body, error) = call.parseBodyEllerDefault(HentSøknaderBody())
    svarMedSøknader(benkService, body, error)
}

private suspend fun RoutingContext.svarMedSøknader(
    benkService: BenkService,
    body: HentSøknaderBody,
    error: String?,
) {
    logger.debug { "Mottatt post-request på $PATH/soknader" }
    val (saksbehandler, token) = autentiser() ?: return

    val respons = benkService.hentSøknader(
        command = HentBenkKommando(
            filtrering = BenkSøknaderFiltrering(
                status = body.filters.status?.tilDomene(),
                søknadstype = body.filters.søknadstype?.tilDomene(),
                resultat = body.filters.resultat?.tilDomene(),
                saksbehandler = body.filters.saksbehandler,
                skjulPåVent = body.filters.skjulPåVent,
                skjulVenterPåAnnenSaksbehandler = body.filters.skjulEgneTilBeslutning,
            ),
            sortering = body.sortering.tilSortering(BenkSøknaderKolonne.entries, BenkSøknaderKolonne.KRAVTIDSPUNKT),
            saksbehandler = saksbehandler,
            correlationId = call.correlationId(),
        ),
        saksbehandlerToken = token,
    ).toDTO(BenkFane.SØKNADER, saksbehandler, error)

    call.respondJson(value = respons)
}

private suspend fun RoutingContext.revurderinger(benkService: BenkService) {
    logger.debug { "Mottatt post-request på $PATH/revurderinger" }
    val (saksbehandler, token) = autentiser() ?: return
    val (body, error) = call.parseBodyEllerDefault(HentRevurderingerBody())

    val respons = benkService.hentRevurderinger(
        command = HentBenkKommando(
            filtrering = BenkRevurderingerFiltrering(
                status = body.filters.status?.tilDomene(),
                resultat = body.filters.resultat?.tilDomene(),
                saksbehandler = body.filters.saksbehandler,
                skjulPåVent = body.filters.skjulPåVent,
                skjulVenterPåAnnenSaksbehandler = body.filters.skjulEgneTilBeslutning,
            ),
            sortering = body.sortering.tilSortering(BenkRevurderingerKolonne.entries, BenkRevurderingerKolonne.STARTET),
            saksbehandler = saksbehandler,
            correlationId = call.correlationId(),
        ),
        saksbehandlerToken = token,
    ).toDTO(BenkFane.REVURDERINGER, saksbehandler, error)

    call.respondJson(value = respons)
}

private suspend fun RoutingContext.meldekort(benkService: BenkService) {
    logger.debug { "Mottatt post-request på $PATH/meldekort" }
    val (saksbehandler, token) = autentiser() ?: return
    val (body, error) = call.parseBodyEllerDefault(HentMeldekortBody())

    val respons = benkService.hentMeldekort(
        command = HentBenkKommando(
            filtrering = BenkMeldekortFiltrering(
                status = body.filters.status?.tilDomene(),
                type = body.filters.type?.tilDomene(),
                saksbehandler = body.filters.saksbehandler,
                skjulPåVent = body.filters.skjulPåVent,
                skjulVenterPåAnnenSaksbehandler = body.filters.skjulEgneTilBeslutning,
            ),
            sortering = body.sortering.tilSortering(BenkMeldekortKolonne.entries, BenkMeldekortKolonne.PERIODE),
            saksbehandler = saksbehandler,
            correlationId = call.correlationId(),
        ),
        saksbehandlerToken = token,
    ).toDTO(BenkFane.MELDEKORT, saksbehandler, error)

    call.respondJson(value = respons)
}

private suspend fun RoutingContext.klage(benkService: BenkService) {
    logger.debug { "Mottatt post-request på $PATH/klage" }
    val (saksbehandler, token) = autentiser() ?: return
    val (body, error) = call.parseBodyEllerDefault(HentKlageBody())

    val respons = benkService.hentKlager(
        command = HentBenkKommando(
            filtrering = BenkKlageFiltrering(
                status = body.filters.status?.tilDomene(),
                resultat = body.filters.resultat?.tilDomene(),
                saksbehandler = body.filters.saksbehandler,
                skjulPåVent = body.filters.skjulPåVent,
            ),
            sortering = body.sortering.tilSortering(BenkKlageKolonne.entries, BenkKlageKolonne.KRAVTIDSPUNKT),
            saksbehandler = saksbehandler,
            correlationId = call.correlationId(),
        ),
        saksbehandlerToken = token,
    ).toDTO(BenkFane.KLAGE, saksbehandler, error)

    call.respondJson(value = respons)
}

private suspend fun RoutingContext.tilbakekreving(benkService: BenkService) {
    logger.debug { "Mottatt post-request på $PATH/tilbakekreving" }
    val (saksbehandler, token) = autentiser() ?: return
    val (body, error) = call.parseBodyEllerDefault(HentTilbakekrevingBody())

    val respons = benkService.hentTilbakekrevinger(
        command = HentBenkKommando(
            filtrering = BenkTilbakekrevingFiltrering(
                status = body.filters.status?.tilDomene(),
                kilde = body.filters.kilde?.tilDomene(),
                saksbehandler = body.filters.saksbehandler,
                minstebeløp = body.filters.minstebeløp(),
                skjulPåVent = body.filters.skjulPåVent,
                skjulVenterPåAnnenSaksbehandler = body.filters.skjulEgneTilBeslutning,
            ),
            sortering = body.sortering.tilSortering(BenkTilbakekrevingKolonne.entries, BenkTilbakekrevingKolonne.STARTET),
            saksbehandler = saksbehandler,
            correlationId = call.correlationId(),
        ),
        saksbehandlerToken = token,
    ).toDTO(BenkFane.TILBAKEKREVING, saksbehandler, error)

    call.respondJson(value = respons)
}

private suspend fun RoutingContext.autentiser(): Pair<Saksbehandler, String>? {
    val token = call.principal<TexasPrincipalInternal>()?.token ?: return null
    val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return null
    return saksbehandler to token
}

/**
 * Tolker bodyen, men faller tilbake på [default] framfor å svare 400 når den ikke lar seg deserialisere.
 * Paret sier hvilken body som ble brukt, og feilmeldingen som skal vises i frontenden når fallbacken ble brukt.
 */
private suspend inline fun <reified T> ApplicationCall.parseBodyEllerDefault(default: T): Pair<T, String?> {
    val bodyText = receiveText()
    if (bodyText.isBlank()) return default to null
    return Either.catch { deserialize<T>(bodyText) }.fold(
        ifLeft = {
            logger.debug { "Feil ved deserialisering av benk-request. Se sikkerlogg for mer kontekst." }
            Sikkerlogg.debug(it) { "Feil ved deserialisering av benk-request. Body: $bodyText" }
            default to FEIL_UGYLDIGE_FILTERVERDIER
        },
        ifRight = { it to null },
    )
}

/**
 * Requesten til søknadsfanen.
 * En ukjent enumverdi feiler deserialiseringen, og ruten svarer med standardvisningen og `error` satt.
 */
private data class HentSøknaderBody(
    val sortering: String? = null,
    val filters: Filters = Filters(),
) {
    data class Filters(
        val status: BenkBehandlingsstatusDTO? = null,
        val søknadstype: BenkSøknadstypeDTO? = null,
        val resultat: BenkSøknadsbehandlingResultatDTO? = null,
        val saksbehandler: String? = null,
        val skjulPåVent: Boolean = false,
        val skjulEgneTilBeslutning: Boolean = false,
    )
}

private data class HentRevurderingerBody(
    val sortering: String? = null,
    val filters: Filters = Filters(),
) {
    data class Filters(
        val status: BenkBehandlingsstatusDTO? = null,
        val resultat: BenkRevurderingResultatDTO? = null,
        val saksbehandler: String? = null,
        val skjulPåVent: Boolean = false,
        val skjulEgneTilBeslutning: Boolean = false,
    )
}

private data class HentMeldekortBody(
    val sortering: String? = null,
    val filters: Filters = Filters(),
) {
    data class Filters(
        val status: BenkBehandlingsstatusDTO? = null,
        val type: BenkMeldekortTypeDTO? = null,
        val saksbehandler: String? = null,
        val skjulPåVent: Boolean = false,
        val skjulEgneTilBeslutning: Boolean = false,
    )
}

private data class HentKlageBody(
    val sortering: String? = null,
    val filters: Filters = Filters(),
) {
    data class Filters(
        val status: BenkBehandlingsstatusDTO? = null,
        val resultat: BenkKlagebehandlingResultatDTO? = null,
        val saksbehandler: String? = null,
        val skjulPåVent: Boolean = false,
    )
}

private data class HentTilbakekrevingBody(
    val sortering: String? = null,
    val filters: Filters = Filters(),
) {
    data class Filters(
        val status: BenkTilbakekrevingStatusDTO? = null,
        val kilde: BenkTilbakekrevingKildeDTO? = null,
        val saksbehandler: String? = null,
        val kunOverMinstebeløp: Boolean = false,
        val skjulPåVent: Boolean = false,
        val skjulEgneTilBeslutning: Boolean = false,
    ) {
        fun minstebeløp(): Long = if (kunOverMinstebeløp) {
            TilbakekrevingBehandling.MINSTEBELØP_FOR_TILBAKEKREVING
        } else {
            0
        }
    }
}
