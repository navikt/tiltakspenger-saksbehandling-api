package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortType
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadstype
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKilde
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingStatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Fane
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Filtrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Sortering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2SorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.HentBenkV2Command
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.tilSortering
import no.nav.tiltakspenger.saksbehandling.benk.v2.service.BenkV2Service
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling

private const val PATH = "/benk"

/**
 * Benk v2.
 *
 * Ett kall henter én fane, sammen med antallet i alle fanene.
 * Frontenden trenger begge deler for å tegne siden, og ville ellers måtte gjøre to kall for hvert fanebytte.
 */
fun Route.hentBenkV2Route(
    benkV2Service: BenkV2Service,
) {
    val logger = KotlinLogging.logger {}

    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH for å hente en fane av benken" }

        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

        call.withBody<HentBenkV2Body> { body ->
            val correlationId = call.correlationId()
            val fane = body.fane()

            val respons = when (fane) {
                BenkV2Fane.SØKNADER -> benkV2Service.hentSøknader(
                    command = body.tilCommand(
                        saksbehandler,
                        correlationId,
                        BenkSøknaderFiltrering(
                            status = body.filters.behandlingsstatus(),
                            søknadstype = body.filters.søknadstype.tilEnum(BenkSøknadstype.entries),
                            saksbehandler = body.filters.saksbehandler,
                        ),
                        body.sortering(BenkSøknaderKolonne.entries, BenkSøknaderKolonne.KRAVTIDSPUNKT),
                    ),
                    saksbehandlerToken = token,
                ).toDTO(fane)

                BenkV2Fane.REVURDERINGER -> benkV2Service.hentRevurderinger(
                    command = body.tilCommand(
                        saksbehandler,
                        correlationId,
                        BenkRevurderingerFiltrering(
                            status = body.filters.behandlingsstatus(),
                            resultat = body.filters.resultat.tilEnum(BenkRevurderingResultat.entries),
                            saksbehandler = body.filters.saksbehandler,
                        ),
                        body.sortering(BenkRevurderingerKolonne.entries, BenkRevurderingerKolonne.STARTET),
                    ),
                    saksbehandlerToken = token,
                ).toDTO(fane)

                BenkV2Fane.MELDEKORT -> benkV2Service.hentMeldekort(
                    command = body.tilCommand(
                        saksbehandler,
                        correlationId,
                        BenkMeldekortFiltrering(
                            status = body.filters.behandlingsstatus(),
                            type = body.filters.type.tilEnum(BenkMeldekortType.entries),
                            saksbehandler = body.filters.saksbehandler,
                        ),
                        body.sortering(BenkMeldekortKolonne.entries, BenkMeldekortKolonne.PERIODE),
                    ),
                    saksbehandlerToken = token,
                ).toDTO(fane)

                BenkV2Fane.KLAGE -> benkV2Service.hentKlager(
                    command = body.tilCommand(
                        saksbehandler,
                        correlationId,
                        BenkKlageFiltrering(
                            status = body.filters.behandlingsstatus(),
                            resultat = body.filters.resultat.tilEnum(BenkKlagebehandlingResultat.entries),
                            saksbehandler = body.filters.saksbehandler,
                        ),
                        body.sortering(BenkKlageKolonne.entries, BenkKlageKolonne.KRAVTIDSPUNKT),
                    ),
                    saksbehandlerToken = token,
                ).toDTO(fane)

                BenkV2Fane.TILBAKEKREVING -> benkV2Service.hentTilbakekrevinger(
                    command = body.tilCommand(
                        saksbehandler,
                        correlationId,
                        BenkTilbakekrevingFiltrering(
                            status = body.filters.status.tilEnum(BenkTilbakekrevingStatus.entries),
                            kilde = body.filters.kilde.tilEnum(BenkTilbakekrevingKilde.entries),
                            saksbehandler = body.filters.saksbehandler,
                            minstebeløp = body.filters.minstebeløp(),
                        ),
                        body.sortering(BenkTilbakekrevingKolonne.entries, BenkTilbakekrevingKolonne.STARTET),
                    ),
                    saksbehandlerToken = token,
                ).toDTO(fane)
            }

            call.respondJson(value = respons)
        }
    }
}

/**
 * Requesten fra benken.
 *
 * [filters] er unionen av filtrene fanene tilbyr, og hver fane plukker sine.
 * Ett felles filterobjekt framfor ett per fane holder deserialiseringen enkel, og koster bare at ubrukte felt ignoreres.
 */
private data class HentBenkV2Body(
    val tab: String?,
    val sortering: String?,
    val filters: Filters = Filters(),
) {
    data class Filters(
        val status: String? = null,
        val søknadstype: String? = null,
        val resultat: String? = null,
        val type: String? = null,
        val kilde: String? = null,
        val saksbehandler: String? = null,
        val kunOverMinstebeløp: Boolean = false,
    ) {
        fun behandlingsstatus(): BenkV2Behandlingsstatus? = status.tilEnum(BenkV2Behandlingsstatus.entries)

        fun minstebeløp(): Long = if (kunOverMinstebeløp) {
            TilbakekrevingBehandling.MINSTEBELØP_FOR_TILBAKEKREVING
        } else {
            0
        }
    }

    fun fane(): BenkV2Fane = tab.tilEnum(BenkV2Fane.entries) ?: BenkV2Fane.SØKNADER

    fun <K : BenkV2SorteringKolonne> sortering(kolonner: List<K>, default: K): BenkV2Sortering<K> =
        sortering.tilSortering(kolonner, default)

    fun <F : BenkV2Filtrering, K : BenkV2SorteringKolonne> tilCommand(
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        filtrering: F,
        sortering: BenkV2Sortering<K>,
    ): HentBenkV2Command<F, K> = HentBenkV2Command(
        filtrering = filtrering,
        sortering = sortering,
        saksbehandler = saksbehandler,
        correlationId = correlationId,
    )
}

/**
 * En ukjent verdi tolkes som «ikke filtrert», ikke som en feil.
 * Filtervalgene kommer fra en url brukeren kan redigere, og en skrivefeil der skal gi en uflitrert benk framfor en 400.
 */
private fun <T : Enum<T>> String?.tilEnum(entries: List<T>): T? =
    entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
