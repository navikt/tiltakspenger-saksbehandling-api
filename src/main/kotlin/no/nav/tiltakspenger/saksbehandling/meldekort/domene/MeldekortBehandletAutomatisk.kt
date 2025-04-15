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
) : MeldekortBehandling.Behandlet {
    override val status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
    override val iverksattTidspunkt: LocalDateTime = opprettet

    // TODO: Hva skal vi sette her? :D
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
    }
}

fun Sak.opprettAutomatiskBehandling(
    meldekort: BrukersMeldekort,
    navkontor: Navkontor,
    clock: Clock,
): MeldekortBehandletAutomatisk {
    val kjedeId = meldekort.kjedeId

    if (this.meldekortBehandlinger.finnesÅpenMeldekortBehandling) {
        throw IllegalStateException("Kan ikke opprette ny meldekortbehandling når det finnes en åpen behandling på saken (sak $id - kjedeId $kjedeId)")
    }

    val behandlingerKnyttetTilKjede = this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(kjedeId)

    require(behandlingerKnyttetTilKjede.isEmpty()) {
        "Meldeperiodekjeden $kjedeId har allerede minst en behandling. Vi støtter ikke automatisk korrigering fra bruker."
    }

    val meldeperiodekjede: MeldeperiodeKjede = this.meldeperiodeKjeder.hentMeldeperiodekjedeForKjedeId(kjedeId)
        ?: throw IllegalStateException("Kan ikke opprette meldekortbehandling for kjedeId $kjedeId som ikke finnes")

    val meldeperiode: Meldeperiode = meldeperiodekjede.hentSisteMeldeperiode()

    require(meldekort.meldeperiode == meldeperiode) {
        "Meldeperioden for brukers meldekort må være like siste meldeperiode på kjeden for å kunne behandles"
    }

    require(!meldeperiode.ingenDagerGirRett) {
        "Kan ikke starte behandling på meldeperiode uten dager som gir rett til tiltakspenger"
    }

    if (this.meldekortBehandlinger.isEmpty()) {
        require(meldeperiode == this.meldeperiodeKjeder.first().hentSisteMeldeperiode()) {
            "Dette er første meldekortbehandling på saken og må da behandle den første meldeperiode kjeden. sakId: ${this.id}, meldeperiodekjedeId: ${meldeperiodekjede.kjedeId}"
        }
    }

    this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjede(kjedeId)
        ?.also { foregåendeMeldeperiodekjede ->
            this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(foregåendeMeldeperiodekjede.kjedeId).also {
                if (it.none { it.status == MeldekortBehandlingStatus.GODKJENT }) {
                    throw IllegalStateException("Kan ikke opprette ny meldekortbehandling før forrige kjede er godkjent")
                }
            }
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
    )
}
