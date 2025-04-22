package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AutomatiskMeldekortbehandlingFeilet
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
    override val iverksattTidspunkt: LocalDateTime = opprettet

    // TODO: Hva skal vi sette her?
    override val saksbehandler: String = "E313373"
    override val beslutter: String = "E313373"

    override val sendtTilBeslutning = null
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
        require(brukersMeldekort.behandlesAutomatisk) {
            "Brukers meldekort ${brukersMeldekort.id} må være satt til å behandles automatisk"
        }
    }
}

fun Sak.opprettAutomatiskMeldekortBehandling(
    meldekort: BrukersMeldekort,
    navkontor: Navkontor,
    clock: Clock,
): Either<AutomatiskMeldekortbehandlingFeilet, MeldekortBehandletAutomatisk> {
    val meldekortId = meldekort.id
    val kjedeId = meldekort.kjedeId

    val meldeperiode = validerOgHentMeldeperiodeForBehandling(kjedeId)

    val behandlingerKnyttetTilKjede = this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(kjedeId)

    if (!meldekort.behandlesAutomatisk) {
        logger.error { "Brukers meldekort $meldekortId skal ikke behandles automatisk" }
        return AutomatiskMeldekortbehandlingFeilet.SkalIkkeBehandlesAutomatisk.left()
    }
    if (behandlingerKnyttetTilKjede.isNotEmpty()) {
        logger.error { "Meldeperiodekjeden $kjedeId har allerede minst en behandling. Vi støtter ikke automatisk korrigering fra bruker (meldekort id $meldekortId)" }
        return AutomatiskMeldekortbehandlingFeilet.AlleredeBehandlet.left()
    }
    if (meldekort.meldeperiode != meldeperiode) {
        logger.error { "Meldeperioden for brukers meldekort må være like siste meldeperiode på kjeden for å kunne behandles (meldekort id $meldekortId)" }
        return AutomatiskMeldekortbehandlingFeilet.UtdatertMeldeperiode.left()
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
        meldeperiode = meldeperiode,
        dager = meldekort.tilMeldekortDager(),
        beregning = MeldekortBeregning(beregninger),
        type = MeldekortBehandlingType.FØRSTE_BEHANDLING,
        status = MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
    ).right()
}
