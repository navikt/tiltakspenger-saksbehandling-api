package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.UNDER_BEHANDLING
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

    /** Pdd har kun automatiske behandlinger tilknyttet et brukers meldekort */
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
    val begrunnelse: MeldekortBehandlingBegrunnelse?

    val attesteringer: Attesteringer

    /** Denne styres kun av vedtakene. Dersom vi har en åpen meldekortbehandling (inkl. til beslutning) kan et nytt vedtak overstyre hele meldeperioden til [MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER] */
    val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?

    /** Merk at statusen [IKKE_RETT_TIL_TILTAKSPENGER] anses som avsluttet. Den vil bli erstattet med AVBRUTT senere. */
    val erAvsluttet
        get() = when (status) {
            UNDER_BEHANDLING, KLAR_TIL_BESLUTNING -> false
            GODKJENT, AUTOMATISK_BEHANDLET, IKKE_RETT_TIL_TILTAKSPENGER -> true
        }

    val beløpTotal: Int?
    val ordinærBeløp: Int?
    val barnetilleggBeløp: Int?

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
            is MeldekortBehandletManuelt -> this.tilUnderBehandling(
                nyMeldeperiode = meldeperiode,
                ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
            )

            is MeldekortUnderBehandling -> this.copy(
                meldeperiode = meldeperiode,
                ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                beregning = null,
            )

            is MeldekortBehandletAutomatisk -> null
        }
    }

    sealed interface Behandlet : MeldekortBehandling {
        override val beregning: MeldekortBeregning
        override val beløpTotal: Int get() = beregning.beregnTotaltBeløp()
        override val ordinærBeløp: Int get() = beregning.beregnTotalOrdinærBeløp()
        override val barnetilleggBeløp: Int get() = beregning.beregnTotalBarnetillegg()

        /**
         *  Perioden for beregningen av meldekortet.
         *  Fra og med start av meldeperioden, til og med siste dag med en beregnet utbetaling
         *  Ved korrigeringer tilbake i tid kan tilOgMed strekke seg til påfølgende meldeperioder dersom disse påvirkes av beregningen
         * */
        val beregningPeriode: Periode get() = beregning.periode
    }
}

fun Sak.validerOpprettMeldekortBehandling(kjedeId: MeldeperiodeKjedeId) {
    val meldeperiodekjede: MeldeperiodeKjede = this.meldeperiodeKjeder.hentMeldeperiodekjedeForKjedeId(kjedeId)!!
    val meldeperiode: Meldeperiode = meldeperiodekjede.hentSisteMeldeperiode()

    if (this.meldekortBehandlinger.isEmpty() &&
        meldeperiode != this.meldeperiodeKjeder.first()
            .hentSisteMeldeperiode()
    ) {
        throw IllegalStateException(
            "Dette er første meldekortbehandling på saken og må da behandle den første meldeperiode kjeden. sakId: ${this.id}, meldeperiodekjedeId: ${meldeperiodekjede.kjedeId}",
        )
    }

    this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjede(kjedeId)
        ?.also { foregåendeMeldeperiodekjede ->
            this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(foregåendeMeldeperiodekjede.kjedeId)
                .also { behandlinger ->
                    if (behandlinger.none { it.status == GODKJENT || it.status == AUTOMATISK_BEHANDLET }) {
                        throw IllegalStateException("Kan ikke opprette ny meldekortbehandling før forrige kjede er godkjent")
                    }
                }
        }

    if (meldeperiode.ingenDagerGirRett) {
        throw IllegalStateException("Kan ikke starte behandling på meldeperiode uten dager som gir rett til tiltakspenger")
    }
}
