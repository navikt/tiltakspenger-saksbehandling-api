package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.LocalDateTime

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
): MeldekortBehandletAutomatisk {
    val kjedeId = meldekort.kjedeId

    val meldeperiode = validerOgHentMeldeperiodeForBehandling(kjedeId)

    val behandlingerKnyttetTilKjede = this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(kjedeId)

    require(meldekort.behandlesAutomatisk) {
        "Brukers meldekort ${meldekort.id} kan ikke behandles automatisk"
    }
    require(behandlingerKnyttetTilKjede.isEmpty()) {
        "Meldeperiodekjeden $kjedeId har allerede minst en behandling. Vi støtter ikke automatisk korrigering fra bruker."
    }
    require(meldekort.meldeperiode == meldeperiode) {
        "Meldeperioden for brukers meldekort må være like siste meldeperiode på kjeden for å kunne behandles"
    }

    val meldekortBehandlingId = MeldekortId.random()

    val dager = meldekort.tilMeldekortDager()

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
        dager = dager,
        beregning = MeldekortBeregning(beregninger),
        type = MeldekortBehandlingType.FØRSTE_BEHANDLING,
        status = MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
    )
}
