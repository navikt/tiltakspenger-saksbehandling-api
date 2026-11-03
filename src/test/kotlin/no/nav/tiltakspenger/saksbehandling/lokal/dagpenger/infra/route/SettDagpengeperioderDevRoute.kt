package no.nav.tiltakspenger.saksbehandling.lokal.dagpenger.infra.route

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toDto
import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode
import no.nav.tiltakspenger.saksbehandling.lokal.dagpenger.infra.http.DagpengerFakeLokalKlient
import no.nav.tiltakspenger.saksbehandling.lokal.infra.route.DEV_ROUTE
import java.time.LocalDate

private const val PATH = "$DEV_ROUTE/dagpenger"

private data class SettDagpengeperioderBody(
    val sakId: String,
    val perioder: List<DagpengeperiodeBody>,
)

private data class DagpengeperiodeBody(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val ytelsetype: Dagpengeperiode.Ytelsetype?,
    val kilde: Dagpengeperiode.Kilde?,
) {
    fun toDomain() = Dagpengeperiode(
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        ytelsetype = ytelsetype ?: Dagpengeperiode.Ytelsetype.ORDINÆRE_DAGPENGER,
        kilde = kilde ?: Dagpengeperiode.Kilde.ARENA,
    )
}

/**
 * Setter dagpengeperiodene dp-datadeling svarer med lokalt for personen på saken, og erstatter de som var satt fra før.
 * En tom liste fjerner dem.
 * `tilOgMed` kan utelates for en løpende periode, `ytelsetype` er som standard `ORDINÆRE_DAGPENGER` og `kilde` `ARENA`.
 * Periodene lagres i `lokal.dagpengeperioder` og overlever omstart.
 * De blir først synlige i en behandling når saksopplysningene hentes på nytt.
 */
fun Route.settDagpengeperioderDevRoute(
    sakRepo: SakRepo,
    dagpengerKlient: DagpengerFakeLokalKlient,
) {
    post(PATH) {
        call.withBody<SettDagpengeperioderBody> { body ->
            val sakId = Either.catch { SakId.fromString(body.sakId.trim()) }.getOrNull()
                ?: return@withBody call.respondText("Ugyldig sakId: ${body.sakId}", status = HttpStatusCode.BadRequest)
            val fnr = sakRepo.hentFnrForSakId(sakId)
                ?: return@withBody call.respondText("Fant ingen sak med id $sakId", status = HttpStatusCode.NotFound)
            val ugyldig = body.perioder.firstOrNull { it.tilOgMed != null && it.tilOgMed < it.fraOgMed }
            if (ugyldig != null) {
                return@withBody call.respondText(
                    "tilOgMed ${ugyldig.tilOgMed} er før fraOgMed ${ugyldig.fraOgMed}",
                    status = HttpStatusCode.BadRequest,
                )
            }
            val perioder = body.perioder.map { it.toDomain() }
            dagpengerKlient.settDagpengeperioder(fnr = fnr, perioder = perioder)
            call.respondJson(value = perioder.map { it.toDto() })
        }
    }
}
