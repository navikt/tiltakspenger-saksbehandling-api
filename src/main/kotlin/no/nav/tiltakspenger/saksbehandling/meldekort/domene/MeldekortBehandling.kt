package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface MeldekortBehandling {
    val id: MeldekortId
    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr
    val opprettet: LocalDateTime
    val dager: MeldekortDager
    val beregning: MeldekortBeregning?
    val meldeperiode: Meldeperiode
    val type: MeldekortBehandlingType

    /** Vil kunne være null dersom vi ikke har mottatt et meldekort via vår digitale flate. Bør på sikt kunne være en liste? */
    val brukersMeldekort: BrukersMeldekort?

    val kjedeId: MeldeperiodeKjedeId get() = meldeperiode.kjedeId
    val periode: Periode get() = meldeperiode.periode
    val fraOgMed: LocalDate get() = periode.fraOgMed
    val tilOgMed: LocalDate get() = periode.tilOgMed

    val saksbehandler: String
    val beslutter: String?
    val status: MeldekortBehandlingStatus
    val navkontor: Navkontor
    val iverksattTidspunkt: LocalDateTime?
    val sendtTilBeslutning: LocalDateTime?
    val begrunnelse: MeldekortbehandlingBegrunnelse?

    val attesteringer: Attesteringer

    /** Denne styres kun av vedtakene. Dersom vi har en åpen meldekortbehandling (inkl. til beslutning) kan et nytt vedtak overstyre hele meldeperioden til [MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER] */
    val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?

    /** Merk at statusen [IKKE_RETT_TIL_TILTAKSPENGER] anses som avsluttet. Den vil bli erstattet med AVBRUTT senere. */
    val erAvsluttet
        get() = when (status) {
            IKKE_BEHANDLET, KLAR_TIL_BESLUTNING -> false
            GODKJENT, IKKE_RETT_TIL_TILTAKSPENGER -> true
        }

    /** Merk at statusen [IKKE_RETT_TIL_TILTAKSPENGER] anses som avsluttet. Den vil bli erstattet med AVBRUTT senere. */
    fun erÅpen(): Boolean = !erAvsluttet

    /** Oppdaterer meldeperioden til [meldeperiode] dersom den har samme kjede id, den er nyere enn den eksisterende og dette ikke er avsluttet meldekortbehandling. */
    fun oppdaterMeldeperiode(
        meldeperiode: Meldeperiode,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
        clock: Clock,
    ): MeldekortBehandling? {
        require(meldeperiode.kjedeId == kjedeId) {
            "MeldekortBehandling: Kan ikke oppdatere meldeperiode med annen kjede id. ${meldeperiode.kjedeId} != $kjedeId"
        }
        if (erAvsluttet) return null
        if (meldeperiode.versjon <= this.meldeperiode.versjon) return null

        val ikkeRettTilTiltakspengerTidspunkt = if (meldeperiode.ingenDagerGirRett) nå(clock) else null
        return when (this) {
            is MeldekortBehandlet -> this.tilUnderBehandling(
                nyMeldeperiode = meldeperiode,
                ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                tiltakstypePerioder = tiltakstypePerioder,
            )

            is MeldekortUnderBehandling -> this.copy(
                meldeperiode = meldeperiode,
                ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                beregning = null,
            )
        }
    }

    // TODO - test
    fun underkjenn(
        begrunnelse: NonBlankString,
        beslutter: Saksbehandler,
        clock: Clock,
    ): Either<KunneIkkeUnderkjenneMeldekortBehandling, MeldekortBehandling>
}

/**
 * TODO post-mvp jah: Ved revurderinger av rammevedtaket, så må vi basere oss på både forrige meldekort og revurderingsvedtaket. Dette løser vi å flytte mer logikk til Sak.kt.
 * TODO post-mvp jah: Når vi implementerer delvis innvilgelse vil hele meldekortperioder kunne bli SPERRET.
 */
fun Sak.opprettMeldekortBehandling(
    kjedeId: MeldeperiodeKjedeId,
    navkontor: Navkontor,
    saksbehandler: Saksbehandler,
    clock: Clock,
): MeldekortUnderBehandling {
    if (this.meldekortBehandlinger.finnesÅpenMeldekortBehandling) {
        throw IllegalStateException("Kan ikke opprette ny meldekortbehandling når det finnes en åpen behandling på saken (sak $id - kjedeId $kjedeId)")
    }

    val meldeperiodekjede: MeldeperiodeKjede = this.meldeperiodeKjeder.hentMeldeperiodekjedeForKjedeId(kjedeId)
        ?: throw IllegalStateException("Kan ikke opprette meldekortbehandling for kjedeId $kjedeId som ikke finnes")
    val meldeperiode: Meldeperiode = meldeperiodekjede.hentSisteMeldeperiode()
    val behandlingerKnyttetTilKjede = this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(kjedeId)

    if (this.meldekortBehandlinger.isEmpty()) {
        require(meldeperiode == this.meldeperiodeKjeder.first().hentSisteMeldeperiode()) {
            "Dette er første meldekortbehandling på saken og må da behandle den første meldeperiode kjeden. sakId: ${this.id}, meldeperiodekjedeId: ${meldeperiodekjede.kjedeId}"
        }
    }

    this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjede(kjedeId)
        ?.also { foregåendeMeldeperiodekjede ->
            this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(foregåendeMeldeperiodekjede.kjedeId).also {
                if (it.none { it.status == GODKJENT }) {
                    throw IllegalStateException("Kan ikke opprette ny meldekortbehandling før forrige kjede er godkjent")
                }
            }
        }

    if (meldeperiode.ingenDagerGirRett) {
        throw IllegalStateException("Kan ikke starte behandling på meldeperiode uten dager som gir rett til tiltakspenger")
    }

    // TODO abn: må støtte flere brukers meldekort på samme kjede før vi åpner for korrigering fra bruker
    val brukersMeldekort = this.brukersMeldekort.find { it.kjedeId == kjedeId }

    val type =
        if (behandlingerKnyttetTilKjede.isEmpty()) MeldekortBehandlingType.FØRSTE_BEHANDLING else MeldekortBehandlingType.KORRIGERING

    val meldekortId = MeldekortId.random()

    return MeldekortUnderBehandling(
        id = meldekortId,
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = nå(clock),
        navkontor = navkontor,
        ikkeRettTilTiltakspengerTidspunkt = null,
        brukersMeldekort = brukersMeldekort,
        meldeperiode = meldeperiode,
        saksbehandler = saksbehandler.navIdent,
        type = type,
        begrunnelse = null,
        attesteringer = Attesteringer.empty(),
        sendtTilBeslutning = null,
        beregning = null,
        dager = meldeperiode.tilMeldekortDager(),
    )
}
