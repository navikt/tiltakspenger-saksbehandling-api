package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingAarsak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingResultat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkHendelse
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortrolig
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortroligStrict
import java.time.Clock

fun Klagevedtak.genererSaksstatistikk(
    gjelderKode6: Boolean,
    versjon: String,
    clock: Clock,
): StatistikkSakDTO {
    return StatistikkSakDTO(
        sakId = this.behandling.sakId.toString(),
        saksnummer = this.behandling.saksnummer.toString(),
        behandlingId = this.behandling.id.toString(),
        relatertBehandlingId = this.behandling.resultat?.rammebehandlingId?.toString(),
        fnr = this.behandling.fnr.verdi,
        mottattTidspunkt = this.behandling.klagensJournalpostOpprettet,
        registrertTidspunkt = this.behandling.opprettet,
        ferdigBehandletTidspunkt = this.opprettet,
        vedtakTidspunkt = this.opprettet,
        endretTidspunkt = this.opprettet,
        utbetaltTidspunkt = null,
        tekniskTidspunkt = nå(clock),
        søknadsformat = this.behandling.formkrav.innsendingskilde.toStatistikkFormat(),
        forventetOppstartTidspunkt = null,
        behandlingType = StatistikkBehandlingType.KLAGE,
        behandlingStatus = StatistikkBehandlingStatus.FERDIG_BEHANDLET,
        behandlingResultat = StatistikkBehandlingResultat.AVVIST,
        resultatBegrunnelse = null,
        // skal være -5 for kode 6
        opprettetAv = maskerHvisStrengtFortroligStrict(gjelderKode6, "system"),
        saksbehandler = maskerHvisStrengtFortrolig(gjelderKode6, this.behandling.saksbehandler),
        ansvarligBeslutter = null,
        // TODO jah: Er 0383 for egen ansatt.
        ansvarligenhet = maskerHvisStrengtFortroligStrict(gjelderKode6, "0387"),
        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        versjon = versjon,
        hendelse = StatistikkHendelse.IVERKSATT_BEHANDLING.value,
        behandlingAarsak = StatistikkBehandlingAarsak.KLAGE,
    )
}
