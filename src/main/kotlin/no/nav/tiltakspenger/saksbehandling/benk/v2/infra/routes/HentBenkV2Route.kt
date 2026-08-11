package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes

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
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Fane
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.HentBenkV2Kommando
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.tilSortering
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto.BenkKlagebehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto.BenkMeldekortTypeDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto.BenkRevurderingResultatDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto.BenkSøknadsbehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto.BenkSøknadstypeDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto.BenkTilbakekrevingKildeDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto.BenkTilbakekrevingStatusDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto.BenkV2BehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto.tilDomene
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.service.BenkV2Service
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
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
fun Route.hentBenkV2Route(
    benkV2Service: BenkV2Service,
) {
    post(PATH) { søknader(benkV2Service) }
    post("$PATH/soknader") { søknader(benkV2Service) }
    post("$PATH/revurderinger") { revurderinger(benkV2Service) }
    post("$PATH/meldekort") { meldekort(benkV2Service) }
    post("$PATH/klage") { klage(benkV2Service) }
    post("$PATH/tilbakekreving") { tilbakekreving(benkV2Service) }

    // Catch-all for feilskrevne faner i url-en — svarer med søknadsfanen og error satt.
    post("$PATH/{...}") { svarMedSøknader(benkV2Service, HentSøknaderBody(), FEIL_UKJENT_FANE) }
}

private suspend fun RoutingContext.søknader(benkV2Service: BenkV2Service) {
    val (body, error) = call.parseBodyEllerDefault(HentSøknaderBody())
    svarMedSøknader(benkV2Service, body, error)
}

private suspend fun RoutingContext.svarMedSøknader(
    benkV2Service: BenkV2Service,
    body: HentSøknaderBody,
    error: String?,
) {
    logger.debug { "Mottatt post-request på $PATH/soknader" }
    val (saksbehandler, token) = autentiser() ?: return

    val respons = benkV2Service.hentSøknader(
        command = HentBenkV2Kommando(
            filtrering = BenkSøknaderFiltrering(
                status = body.filters.status?.tilDomene(),
                søknadstype = body.filters.søknadstype?.tilDomene(),
                resultat = body.filters.resultat?.tilDomene(),
                saksbehandler = body.filters.saksbehandler,
                skjulPåVent = body.filters.skjulPåVent,
            ),
            sortering = body.sortering.tilSortering(BenkSøknaderKolonne.entries, BenkSøknaderKolonne.KRAVTIDSPUNKT),
            saksbehandler = saksbehandler,
            correlationId = call.correlationId(),
        ),
        saksbehandlerToken = token,
    ).toDTO(BenkV2Fane.SØKNADER, saksbehandler, error)

    call.respondJson(value = respons)
}

private suspend fun RoutingContext.revurderinger(benkV2Service: BenkV2Service) {
    logger.debug { "Mottatt post-request på $PATH/revurderinger" }
    val (saksbehandler, token) = autentiser() ?: return
    val (body, error) = call.parseBodyEllerDefault(HentRevurderingerBody())

    val respons = benkV2Service.hentRevurderinger(
        command = HentBenkV2Kommando(
            filtrering = BenkRevurderingerFiltrering(
                status = body.filters.status?.tilDomene(),
                resultat = body.filters.resultat?.tilDomene(),
                saksbehandler = body.filters.saksbehandler,
                skjulPåVent = body.filters.skjulPåVent,
            ),
            sortering = body.sortering.tilSortering(BenkRevurderingerKolonne.entries, BenkRevurderingerKolonne.STARTET),
            saksbehandler = saksbehandler,
            correlationId = call.correlationId(),
        ),
        saksbehandlerToken = token,
    ).toDTO(BenkV2Fane.REVURDERINGER, saksbehandler, error)

    call.respondJson(value = respons)
}

private suspend fun RoutingContext.meldekort(benkV2Service: BenkV2Service) {
    logger.debug { "Mottatt post-request på $PATH/meldekort" }
    val (saksbehandler, token) = autentiser() ?: return
    val (body, error) = call.parseBodyEllerDefault(HentMeldekortBody())

    val respons = benkV2Service.hentMeldekort(
        command = HentBenkV2Kommando(
            filtrering = BenkMeldekortFiltrering(
                status = body.filters.status?.tilDomene(),
                type = body.filters.type?.tilDomene(),
                saksbehandler = body.filters.saksbehandler,
                skjulPåVent = body.filters.skjulPåVent,
            ),
            sortering = body.sortering.tilSortering(BenkMeldekortKolonne.entries, BenkMeldekortKolonne.PERIODE),
            saksbehandler = saksbehandler,
            correlationId = call.correlationId(),
        ),
        saksbehandlerToken = token,
    ).toDTO(BenkV2Fane.MELDEKORT, saksbehandler, error)

    call.respondJson(value = respons)
}

private suspend fun RoutingContext.klage(benkV2Service: BenkV2Service) {
    logger.debug { "Mottatt post-request på $PATH/klage" }
    val (saksbehandler, token) = autentiser() ?: return
    val (body, error) = call.parseBodyEllerDefault(HentKlageBody())

    val respons = benkV2Service.hentKlager(
        command = HentBenkV2Kommando(
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
    ).toDTO(BenkV2Fane.KLAGE, saksbehandler, error)

    call.respondJson(value = respons)
}

private suspend fun RoutingContext.tilbakekreving(benkV2Service: BenkV2Service) {
    logger.debug { "Mottatt post-request på $PATH/tilbakekreving" }
    val (saksbehandler, token) = autentiser() ?: return
    val (body, error) = call.parseBodyEllerDefault(HentTilbakekrevingBody())

    val respons = benkV2Service.hentTilbakekrevinger(
        command = HentBenkV2Kommando(
            filtrering = BenkTilbakekrevingFiltrering(
                status = body.filters.status?.tilDomene(),
                kilde = body.filters.kilde?.tilDomene(),
                saksbehandler = body.filters.saksbehandler,
                minstebeløp = body.filters.minstebeløp(),
                skjulPåVent = body.filters.skjulPåVent,
            ),
            sortering = body.sortering.tilSortering(BenkTilbakekrevingKolonne.entries, BenkTilbakekrevingKolonne.STARTET),
            saksbehandler = saksbehandler,
            correlationId = call.correlationId(),
        ),
        saksbehandlerToken = token,
    ).toDTO(BenkV2Fane.TILBAKEKREVING, saksbehandler, error)

    call.respondJson(value = respons)
}

private suspend fun RoutingContext.autentiser(): Pair<Saksbehandler, String>? {
    val token = call.principal<TexasPrincipalInternal>()?.token ?: return null
    val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return null
    krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
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
        val status: BenkV2BehandlingsstatusDTO? = null,
        val søknadstype: BenkSøknadstypeDTO? = null,
        val resultat: BenkSøknadsbehandlingResultatDTO? = null,
        val saksbehandler: String? = null,
        val skjulPåVent: Boolean = false,
    )
}

private data class HentRevurderingerBody(
    val sortering: String? = null,
    val filters: Filters = Filters(),
) {
    data class Filters(
        val status: BenkV2BehandlingsstatusDTO? = null,
        val resultat: BenkRevurderingResultatDTO? = null,
        val saksbehandler: String? = null,
        val skjulPåVent: Boolean = false,
    )
}

private data class HentMeldekortBody(
    val sortering: String? = null,
    val filters: Filters = Filters(),
) {
    data class Filters(
        val status: BenkV2BehandlingsstatusDTO? = null,
        val type: BenkMeldekortTypeDTO? = null,
        val saksbehandler: String? = null,
        val skjulPåVent: Boolean = false,
    )
}

private data class HentKlageBody(
    val sortering: String? = null,
    val filters: Filters = Filters(),
) {
    data class Filters(
        val status: BenkV2BehandlingsstatusDTO? = null,
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
    ) {
        fun minstebeløp(): Long = if (kunOverMinstebeløp) {
            TilbakekrevingBehandling.MINSTEBELØP_FOR_TILBAKEKREVING
        } else {
            0
        }
    }
}
