package no.nav.tiltakspenger.saksbehandling.lokal.klage.infra.route

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.respondOk
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.lokal.infra.route.DEV_ROUTE
import no.nav.tiltakspenger.saksbehandling.lokal.klage.infra.kafka.GenererLokalKabalHendelseCommand
import no.nav.tiltakspenger.saksbehandling.lokal.klage.infra.kafka.LokalHendelseUtfall
import no.nav.tiltakspenger.saksbehandling.lokal.klage.infra.kafka.LokalKabalHendelseService

private const val PATH = "$DEV_ROUTE/klage/hendelse"

private data class NyKlagehendelseBody(
    val klagebehandlingId: String,
    val type: String,
    val utfall: String,
)

fun Route.nyKlagehendelseDevRoute(lokalKabalHendelseService: LokalKabalHendelseService) {
    post(PATH) {
        call.withBody<NyKlagehendelseBody> { body ->
            lokalKabalHendelseService.genererHendelse(
                command = GenererLokalKabalHendelseCommand(
                    type = body.type,
                    utfall = LokalHendelseUtfall.valueOf(body.utfall),
                    klagebehandlingId = KlagebehandlingId.fromString(body.klagebehandlingId),
                ),
            )
            call.respondOk()
        }
    }
}
