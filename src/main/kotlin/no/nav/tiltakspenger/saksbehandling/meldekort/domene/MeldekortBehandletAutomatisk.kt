package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.beregning.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.beregn
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.KunneIkkeOvertaMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

data class MeldekortBehandletAutomatisk(
    override val id: MeldekortId,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val opprettet: LocalDateTime,
    override val dager: MeldekortDager,
    override val beregning: MeldekortBeregning,
    override val simulering: Simulering?,
    override val meldeperiode: Meldeperiode,
    override val brukersMeldekort: BrukersMeldekort,
    override val navkontor: Navkontor,
    override val type: MeldekortBehandlingType,
    override val status: MeldekortBehandlingStatus,
) : MeldekortBehandling.Behandlet {
    // Automatiske behandlinger iverksettes umiddelbart
    override val iverksattTidspunkt = opprettet
    override val sendtTilBeslutning = opprettet

    override val saksbehandler = AUTOMATISK_SAKSBEHANDLER_ID
    override val beslutter = AUTOMATISK_SAKSBEHANDLER_ID

    override val begrunnelse = null
    override val ikkeRettTilTiltakspengerTidspunkt = null

    override val attesteringer = Attesteringer.empty()
    override val avbrutt: Avbrutt? = null

    init {
        require(type == MeldekortBehandlingType.FØRSTE_BEHANDLING) {
            "Vi støtter ikke automatisk behandling av korrigering fra bruker"
        }
        require(status === MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET) {
            "Ugyldig status for automatisk behandling: $status"
        }
    }

    override fun overta(saksbehandler: Saksbehandler): Either<KunneIkkeOvertaMeldekortBehandling, MeldekortBehandling> {
        return KunneIkkeOvertaMeldekortBehandling.KanIkkeOvertaAutomatiskBehandling.left()
    }

    override fun taMeldekortBehandling(saksbehandler: Saksbehandler): MeldekortBehandling {
        throw IllegalStateException("Kan ikke tildele automatisk behandlet meldekort")
    }

    override fun leggTilbakeMeldekortBehandling(saksbehandler: Saksbehandler): MeldekortBehandling {
        throw IllegalStateException("Kan ikke legge tilbake automatisk behandlet meldekort")
    }
}

suspend fun Sak.opprettAutomatiskMeldekortBehandling(
    brukersMeldekort: BrukersMeldekort,
    navkontor: Navkontor,
    clock: Clock,
    simuler: suspend (behandling: MeldekortBehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>,
): Either<BrukersMeldekortBehandletAutomatiskStatus, Pair<MeldekortBehandletAutomatisk, SimuleringMedMetadata?>> {
    val meldekortId = brukersMeldekort.id
    val kjedeId = brukersMeldekort.kjedeId

    validerOpprettMeldekortBehandling(kjedeId)

    val sisteMeldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    val behandlingerKnyttetTilKjede = this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(kjedeId)

    if (!brukersMeldekort.behandlesAutomatisk) {
        logger.error { "Brukers meldekort $meldekortId skal ikke behandles automatisk" }
        return BrukersMeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK.left()
    }
    if (behandlingerKnyttetTilKjede.isNotEmpty()) {
        logger.error { "Meldeperiodekjeden $kjedeId har allerede minst en behandling. Vi støtter ikke automatisk korrigering fra bruker (meldekort id $meldekortId)" }
        return BrukersMeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET.left()
    }
    if (brukersMeldekort.meldeperiode != sisteMeldeperiode) {
        logger.error { "Meldeperioden for brukers meldekort må være like siste meldeperiode på kjeden for å kunne behandles (meldekort id $meldekortId)" }
        return BrukersMeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE.left()
    }
    if (brukersMeldekort.antallDagerRegistrert > sisteMeldeperiode.maksAntallDagerForMeldeperiode) {
        logger.error { "Brukers meldekort $meldekortId har for mange dager registret" }
        return BrukersMeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT.left()
    }

    val meldekortBehandlingId = MeldekortId.random()

    val beregninger = this.beregn(
        meldekortIdSomBeregnes = meldekortBehandlingId,
        meldeperiodeSomBeregnes = brukersMeldekort.tilMeldekortDager(),
    )
    val meldekortBehandletAutomatisk = MeldekortBehandletAutomatisk(
        id = meldekortBehandlingId,
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = nå(clock),
        navkontor = navkontor,
        brukersMeldekort = brukersMeldekort,
        meldeperiode = sisteMeldeperiode,
        dager = brukersMeldekort.tilMeldekortDager(),
        beregning = MeldekortBeregning(beregninger),
        type = MeldekortBehandlingType.FØRSTE_BEHANDLING,
        status = MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
        simulering = null,
    )
    return simuler(meldekortBehandletAutomatisk).mapLeft {
        // Simuleringsklienten logger feil selv. I førsteomgang ønsker vi ikke stoppe den automatiske ubtbetalingen selvom simuleringen feiler.
        return Pair(meldekortBehandletAutomatisk, null).right()
    }.map { Pair(meldekortBehandletAutomatisk.copy(simulering = it.simulering), it) }
}
