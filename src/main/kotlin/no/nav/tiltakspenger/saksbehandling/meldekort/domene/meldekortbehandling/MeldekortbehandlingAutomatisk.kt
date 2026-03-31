package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.beregnMeldekort
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.KunneIkkeOvertaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * Denne er iverksatt/godkjent.
 * Gjelder kun tilstanden AUTOMATISK_BEHANDLET
 */
data class MeldekortBehandletAutomatisk(
    override val id: MeldekortId,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val opprettet: LocalDateTime,
    override val dager: MeldekortDager,
    override val beregning: Beregning,
    override val simulering: Simulering?,
    override val meldeperiode: Meldeperiode,
    override val brukersMeldekort: BrukersMeldekort,
    override val navkontor: Navkontor,
    override val type: MeldekortbehandlingType,
    override val status: MeldekortbehandlingStatus,
    override val sistEndret: LocalDateTime,
) : Meldekortbehandling.Behandlet {
    // Automatiske behandlinger iverksettes umiddelbart
    override val iverksattTidspunkt = opprettet
    override val sendtTilBeslutning = opprettet

    override val saksbehandler = AUTOMATISK_SAKSBEHANDLER_ID
    override val beslutter = AUTOMATISK_SAKSBEHANDLER_ID

    override val begrunnelse = null
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null
    override val ikkeRettTilTiltakspengerTidspunkt = null

    override val attesteringer = Attesteringer.empty()
    override val avbrutt: Avbrutt? = null
    override val skalSendeVedtaksbrev: Boolean = true

    init {
        require(type == MeldekortbehandlingType.FØRSTE_BEHANDLING) {
            "Vi støtter ikke automatisk behandling av korrigering fra bruker"
        }
        require(status === MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET) {
            "Ugyldig status for automatisk behandling: $status"
        }
    }

    override fun overta(
        saksbehandler: Saksbehandler,
        clock: Clock,
    ): Either<KunneIkkeOvertaMeldekortbehandling, Meldekortbehandling> {
        return KunneIkkeOvertaMeldekortbehandling.KanIkkeOvertaAutomatiskBehandling.left()
    }

    override fun taMeldekortbehandling(saksbehandler: Saksbehandler, clock: Clock): Meldekortbehandling {
        throw IllegalStateException("Kan ikke tildele automatisk behandlet meldekort")
    }

    override fun leggTilbakeMeldekortbehandling(saksbehandler: Saksbehandler, clock: Clock): Meldekortbehandling {
        throw IllegalStateException("Kan ikke legge tilbake automatisk behandlet meldekort")
    }

    override fun oppdaterSimulering(simulering: Simulering?): Meldekortbehandling {
        throw IllegalStateException("Kan ikke oppdatere simulering på automatisk behandlet meldekort")
    }
}

suspend fun Sak.opprettAutomatiskMeldekortbehandling(
    brukersMeldekort: BrukersMeldekort,
    hentNavkontor: suspend (fnr: Fnr) -> Navkontor,
    clock: Clock,
    simuler: suspend (behandling: Meldekortbehandling, navkontor: Navkontor) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>,
): Either<MeldekortBehandletAutomatiskStatus, Pair<MeldekortBehandletAutomatisk, SimuleringMedMetadata?>> {
    validerOpprettAutomatiskMeldekortbehandling(brukersMeldekort).onLeft {
        return it.left()
    }

    val navkontor = Either.catch {
        hentNavkontor(fnr)
    }.getOrElse {
        with("Kunne ikke hente navkontor for sak $id") {
            logger.error(it) { this }
            Sikkerlogg.error(it) { "$this - fnr ${fnr.verdi}" }
        }
        return MeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET.left()
    }

    val meldekortbehandlingId = MeldekortId.random()

    val beregninger = this.beregnMeldekort(
        meldekortIdSomBeregnes = meldekortbehandlingId,
        meldeperioderSomBeregnes = brukersMeldekort.tilMeldekortDager(),
    )
    val nå = nå(clock)
    val meldekortBehandletAutomatisk = MeldekortBehandletAutomatisk(
        id = meldekortbehandlingId,
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = nå,
        navkontor = navkontor,
        brukersMeldekort = brukersMeldekort,
        meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(brukersMeldekort.kjedeId),
        dager = brukersMeldekort.tilMeldekortDager(),
        beregning = Beregning(beregninger, nå),
        type = MeldekortbehandlingType.FØRSTE_BEHANDLING,
        status = MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
        simulering = null,
        sistEndret = nå,
    )

    return simuler(meldekortBehandletAutomatisk, navkontor).mapLeft {
        // Simuleringsklienten logger feil selv. I førsteomgang ønsker vi ikke stoppe den automatiske utbetalingen selv om simuleringen feiler.
        return Pair(meldekortBehandletAutomatisk, null).right()
    }.map { Pair(meldekortBehandletAutomatisk.copy(simulering = it.simulering), it) }
}
