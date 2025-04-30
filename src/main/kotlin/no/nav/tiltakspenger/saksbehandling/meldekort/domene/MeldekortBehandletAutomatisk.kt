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
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.KunneIkkeOvertaMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
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

    override fun leggTilbakeMeldekortBehandling(saksbehandler: Saksbehandler): Either<KanIkkeLeggeTilbakeMeldekortBehandling, MeldekortBehandling> {
        throw IllegalStateException("Kan ikke legge tilbake automatisk behandlet meldekort")
    }
}

fun Sak.opprettAutomatiskMeldekortBehandling(
    meldekort: BrukersMeldekort,
    navkontor: Navkontor,
    clock: Clock,
): Either<BrukersMeldekortBehandletAutomatiskStatus, MeldekortBehandletAutomatisk> {
    val meldekortId = meldekort.id
    val kjedeId = meldekort.kjedeId

    validerOpprettMeldekortBehandling(kjedeId)

    val sisteMeldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    val behandlingerKnyttetTilKjede = this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(kjedeId)

    if (!meldekort.behandlesAutomatisk) {
        logger.error { "Brukers meldekort $meldekortId skal ikke behandles automatisk" }
        return BrukersMeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK.left()
    }
    if (behandlingerKnyttetTilKjede.isNotEmpty()) {
        logger.error { "Meldeperiodekjeden $kjedeId har allerede minst en behandling. Vi støtter ikke automatisk korrigering fra bruker (meldekort id $meldekortId)" }
        return BrukersMeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET.left()
    }
    if (meldekort.meldeperiode != sisteMeldeperiode) {
        logger.error { "Meldeperioden for brukers meldekort må være like siste meldeperiode på kjeden for å kunne behandles (meldekort id $meldekortId)" }
        return BrukersMeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE.left()
    }

    val meldekortBehandlingId = MeldekortId.random()

    val beregninger = meldekort.beregn(
        meldekortBehandlingId = meldekortBehandlingId,
        meldekortBehandlinger = this.meldekortBehandlinger,
        barnetilleggsPerioder = this.barnetilleggsperioder,
        tiltakstypePerioder = this.tiltakstypeperioder,
    )

    return MeldekortBehandletAutomatisk(
        id = meldekortBehandlingId,
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = nå(clock),
        navkontor = navkontor,
        brukersMeldekort = meldekort,
        meldeperiode = sisteMeldeperiode,
        dager = meldekort.tilMeldekortDager(),
        beregning = MeldekortBeregning(beregninger),
        type = MeldekortBehandlingType.FØRSTE_BEHANDLING,
        status = MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
    ).right()
}
