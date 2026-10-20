package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil
import no.nav.tiltakspenger.saksbehandling.infra.route.withSaksnummer
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Behandlingsarsak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.KanIkkeStarteBehandlingenAvManueltRegistrertSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype
import no.nav.tiltakspenger.saksbehandling.søknad.service.StartBehandlingAvManueltRegistrertSøknadService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

const val MANUELT_REGISTRERT_SØKNAD_PATH = "sak/{saksnummer}/soknad"

data class StartBehandlingAvManueltRegistrertSøknadCommand(
    val journalpostId: JournalpostId,
    val manueltSattSøknadsperiode: Periode?,
    val manueltSattSøknadMottattDato: LocalDate?,
    val manueltSattTiltak: String?,
    val behandlingsarsak: Behandlingsarsak?,
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

fun Route.startBehandlingAvManueltRegistrertSøknadRoute(
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    startBehandlingAvManueltRegistrertSøknadService: StartBehandlingAvManueltRegistrertSøknadService,
    sakService: SakService,
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
                val internTiltaksdeltakelsesId = body.svar.tiltak?.let {
                    tiltaksdeltakerRepo.hentEllerLagre(
                        eksternId = it.eksternDeltakelseId,
                        tiltakstype = it.typeKode.tilTiltakstype(),
                        sakId = sakService.hentForSaksnummer(saksnummer).id,
                    )
                }
                startBehandlingAvManueltRegistrertSøknadService.startBehandlingAvManueltRegistrertSøknad(
                    saksnummer = saksnummer,
                    kommando = body.tilKommando(internTiltaksdeltakelsesId),
                    saksbehandler = saksbehandler,
                    correlationId = call.correlationId(),
                ).fold(
                    ifLeft = { feil ->
                        call.loggOgSvarFeil(
                            logger,
                            operasjon = "Start behandling av manuelt registrert søknad",
                            feil = feil,
                            statusOgErrorJson = feil.statusOgErrorJson(),
                            kontekst = "saksnummer=$saksnummer",
                        )
                    },
                    ifRight = { (sak, søknadsbehandling) ->
                        auditService.logMedSaksnummer(
                            saksnummer = saksnummer,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.CREATE,
                            correlationId = call.correlationId(),
                            contextMessage = "Startet behandling ${søknadsbehandling.id} av manuelt registrert søknad ${søknadsbehandling.søknad.id} for sak $saksnummer",
                        )
                        call.respondJson(
                            value = søknadsbehandling.tilSøknadsbehandlingDTO(
                                utbetalingsstatus = null,
                                beregninger = sak.meldeperiodeBeregninger,
                                rammevedtakId = null,
                                tilbakekrevingId = null,
                                kallendeSaksbehandler = saksbehandler,
                            ),
                        )
                    },
                )
            }
        }
    }
}

fun KanIkkeStarteBehandlingenAvManueltRegistrertSøknad.statusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KanIkkeStarteBehandlingenAvManueltRegistrertSøknad.EnManuellDatoKrevesNårDetIkkeErMigreringFraArena -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "En manuell registrert søknad må ha en dato når den ikke overføres fra Arena.",
        kode = "mangler_manuell_dato",
    )

    is KanIkkeStarteBehandlingenAvManueltRegistrertSøknad.JournalpostFinnesIkke -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Journalpost $id finnes ikke.",
        kode = "journalpost_finnes_ikke",
    )

    is KanIkkeStarteBehandlingenAvManueltRegistrertSøknad.JournalpostManglerOpprettsdato -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Journalpost $id mangler opprettsdato.",
        kode = "journalpost_mangler_opprettsdato",
    )

    is KanIkkeStarteBehandlingenAvManueltRegistrertSøknad.JournalpostTilhørerEnAnnenBruker -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Journalpost $id tilhører en annen bruker.",
        kode = "journalpost_tilhører_en_annen_bruker",
    )
}
