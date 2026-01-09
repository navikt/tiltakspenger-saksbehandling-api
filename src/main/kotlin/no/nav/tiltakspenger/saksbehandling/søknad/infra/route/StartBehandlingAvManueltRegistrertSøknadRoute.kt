package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSaksnummer
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype
import no.nav.tiltakspenger.saksbehandling.søknad.service.StartBehandlingAvManueltRegistrertSøknadService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo

private val logger = KotlinLogging.logger {}

const val MANUELT_REGISTRERT_SØKNAD_PATH = "sak/{saksnummer}/soknad"

fun Route.startBehandlingAvManueltRegistrertSøknadRoute(
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    startBehandlingAvManueltRegistrertSøknadService: StartBehandlingAvManueltRegistrertSøknadService,
    tiltaksdeltakerRepo: TiltaksdeltakerRepo,
) {
    post(MANUELT_REGISTRERT_SØKNAD_PATH) {
        logger.debug { "Mottatt manuelt registrert søknad på '$MANUELT_REGISTRERT_SØKNAD_PATH'" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSaksnummer { saksnummer ->
            krevSaksbehandlerRolle(saksbehandler)
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, token)

            call.withBody<ManueltRegistrertSøknadBody> { body ->
                val internTiltaksdeltakelsesId = body.svar.tiltak?.eksternDeltakelseId?.let {
                    tiltaksdeltakerRepo.hentEllerLagre(it)
                }
                val (sak, søknad) = startBehandlingAvManueltRegistrertSøknadService.startBehandlingAvManueltRegistrertSøknad(
                    saksnummer = saksnummer,
                    kommando = body.tilKommando(internTiltaksdeltakelsesId),
                    saksbehandler = saksbehandler,
                    correlationId = call.correlationId(),
                )
                auditService.logMedSaksnummer(
                    saksnummer = saksnummer,
                    navIdent = saksbehandler.navIdent,
                    action = AuditLogEvent.Action.CREATE,
                    correlationId = call.correlationId(),
                    contextMessage = "Startet behandling av manuelt registrert søknad med id ${søknad.id} for sak $saksnummer",
                )
                call.respondJson(
                    value = søknad.tilSøknadsbehandlingDTO(
                        utbetalingsstatus = null,
                        beregninger = sak.meldeperiodeBeregninger,
                        rammevedtakId = null,
                    ),
                )
            }
        }
    }
}

data class StartBehandlingAvManueltRegistrertSøknadCommand(
    val personopplysninger: Søknad.Personopplysninger,
    val journalpostId: JournalpostId,
    val manueltSattSøknadsperiode: Periode?,
    val manueltSattTiltak: String?,
    val søknadstiltak: Søknadstiltak?,
    val barnetillegg: List<BarnetilleggFraSøknad>,
    val antallVedlegg: Int,
    val søknadstype: Søknadstype,
    val harSøktPåTiltak: Søknad.JaNeiSpm,
    val harSøktOmBarnetillegg: Søknad.JaNeiSpm,
    val kvp: Søknad.PeriodeSpm,
    val intro: Søknad.PeriodeSpm,
    val institusjon: Søknad.PeriodeSpm,
    val etterlønn: Søknad.JaNeiSpm,
    val gjenlevendepensjon: Søknad.PeriodeSpm,
    val alderspensjon: Søknad.FraOgMedDatoSpm,
    val sykepenger: Søknad.PeriodeSpm,
    val supplerendeStønadAlder: Søknad.PeriodeSpm,
    val supplerendeStønadFlyktning: Søknad.PeriodeSpm,
    val jobbsjansen: Søknad.PeriodeSpm,
    val trygdOgPensjon: Søknad.PeriodeSpm,
)
