package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.meldekortbehandling

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.GenererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingAarsak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingResultat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkFormat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortrolig
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortroligStrict
import java.time.LocalDateTime

/**
 * Saksstatistikk for manuelle meldekortbehandlinger; Team Sak har ikke bedt om de automatiske.
 * Avklart med Team Sak i #tiltakspenger-dvh oktober 2026:
 * - [SaksstatistikkDTO.relatertBehandlingId] er rammebehandlingen bak det første rammevedtaket meldekortbehandlingen bygger på.
 * - [SaksstatistikkDTO.mottattTidspunkt] er når det første av brukers meldekort i behandlingen gikk til manuell behandling, ellers når behandlingen ble opprettet.
 */
fun Sak.genererSaksstatistikk(
    meldekortbehandling: Meldekortbehandling,
    hendelse: StatistikkhendelseType,
): GenererSaksstatistikk {
    require(!meldekortbehandling.erAutomatiskBehandling) {
        "Skal ikke generere saksstatistikk for automatisk meldekortbehandling ${meldekortbehandling.id}"
    }
    val brukersMeldekort = meldekortbehandling.kjedeIder.toList().flatMap { finnBrukersMeldekortFor(meldekortbehandling, it) }
    val mottattTidspunkt = brukersMeldekort.minOfOrNull { it.tidspunktTilManuellBehandling(meldekortbehandling.opprettet) }
        ?: meldekortbehandling.opprettet
    val relatertBehandlingId = hentRammevedtakForId(
        meldekortbehandling.meldeperioder.first().meldeperiode.rammevedtak.verdier.first(),
    ).rammebehandling.id.toString()

    return GenererSaksstatistikk { gjelderKode6, versjon ->
        val gjelderKode6 = gjelderKode6(meldekortbehandling.fnr)
        SaksstatistikkDTO(
            sakId = meldekortbehandling.sakId.toString(),
            saksnummer = meldekortbehandling.saksnummer.toString(),
            behandlingId = meldekortbehandling.id.toString(),
            relatertBehandlingId = relatertBehandlingId,
            fnr = meldekortbehandling.fnr.verdi,
            mottattTidspunkt = mottattTidspunkt,
            registrertTidspunkt = meldekortbehandling.opprettet,
            ferdigBehandletTidspunkt = meldekortbehandling.iverksattTidspunkt ?: meldekortbehandling.avbrutt?.tidspunkt,
            vedtakTidspunkt = meldekortbehandling.iverksattTidspunkt,
            endretTidspunkt = meldekortbehandling.sistEndret,
            utbetaltTidspunkt = null,
            søknadsformat = if (brukersMeldekort.isNotEmpty()) StatistikkFormat.DIGITAL else StatistikkFormat.ANNET,
            forventetOppstartTidspunkt = null,
            behandlingType = StatistikkBehandlingType.MELDEKORT,
            behandlingStatus = meldekortbehandling.status.tilStatistikkBehandlingStatus(),
            behandlingResultat = when (meldekortbehandling.status) {
                MeldekortbehandlingStatus.GODKJENT -> StatistikkBehandlingResultat.GODKJENT
                MeldekortbehandlingStatus.AVBRUTT -> StatistikkBehandlingResultat.AVBRUTT
                else -> null
            },
            resultatBegrunnelse = null,
            // skal være -5 for kode 6
            opprettetAv = maskerHvisStrengtFortroligStrict(gjelderKode6, "system"),
            saksbehandler = maskerHvisStrengtFortrolig(gjelderKode6, meldekortbehandling.saksbehandler),
            ansvarligBeslutter = maskerHvisStrengtFortrolig(gjelderKode6, meldekortbehandling.beslutter),
            ansvarligenhet = maskerHvisStrengtFortroligStrict(gjelderKode6, "0387"),
            tilbakekrevingsbeløp = null,
            funksjonellPeriodeFom = null,
            funksjonellPeriodeTom = null,
            versjon = versjon,
            hendelse = hendelse.value,
            behandlingAarsak = if (meldekortbehandling.meldeperioder.all { it.erKorrigering }) {
                StatistikkBehandlingAarsak.KORRIGERING
            } else {
                StatistikkBehandlingAarsak.MELDEKORT
            },
        )
    }
}

/**
 * Den manuelle behandlingen er ikke koblet til brukers meldekort, så vi finner det via meldeperiodekjeden:
 * meldekortene fra bruker som kom inn før behandlingen ble opprettet, og som ingen annen ikke-avbrutt behandling er opprettet etter.
 * Har bruker sendt flere, regner Team Sak fra det første som gikk til manuell behandling (avklart i #tiltakspenger-dvh oktober 2026).
 * Er alle meldekortene på kjeden behandlet av en tidligere behandling, for eksempel når saksbehandler korrigerer på eget initiativ, er lista tom.
 */
private fun Sak.finnBrukersMeldekortFor(meldekortbehandling: Meldekortbehandling, kjedeId: MeldeperiodeKjedeId): List<BrukersMeldekort> {
    return brukersMeldekort
        .filter { it.kjedeId == kjedeId && it.mottatt <= meldekortbehandling.opprettet }
        .filter { meldekort ->
            meldekortbehandlinger.none {
                it.id != meldekortbehandling.id &&
                    !it.erAvbrutt &&
                    it.kjedeIder.contains(kjedeId) &&
                    it.opprettet >= meldekort.mottatt &&
                    it.opprettet < meldekortbehandling.opprettet
            }
        }
}

/**
 * Siste automatiske forsøk er tidspunktet meldekortet gikk til manuell behandling, men bare når den automatiske behandlingen har gitt opp.
 * Prøves meldekortet fortsatt automatisk, tok saksbehandler det før det gikk til manuell behandling; senere forsøk havner etter [opprettet] og gir samme svar.
 */
private fun BrukersMeldekort.tidspunktTilManuellBehandling(opprettet: LocalDateTime): LocalDateTime {
    if (behandlesAutomatisk) return opprettet
    return minOf(behandletAutomatiskForsøkshistorikk.forrigeForsøk ?: mottatt, opprettet)
}

private fun MeldekortbehandlingStatus.tilStatistikkBehandlingStatus(): StatistikkBehandlingStatus = when (this) {
    MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> StatistikkBehandlingStatus.KLAR_TIL_BEHANDLING

    MeldekortbehandlingStatus.UNDER_BEHANDLING -> StatistikkBehandlingStatus.UNDER_BEHANDLING

    MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
    MeldekortbehandlingStatus.UNDER_BESLUTNING,
    -> StatistikkBehandlingStatus.UNDER_BESLUTNING

    MeldekortbehandlingStatus.GODKJENT -> StatistikkBehandlingStatus.FERDIG_BEHANDLET

    MeldekortbehandlingStatus.AVBRUTT -> StatistikkBehandlingStatus.AVSLUTTET

    MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET -> throw IllegalStateException("Skal ikke generere saksstatistikk for automatisk meldekortbehandling")
}
