package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.statistikk.GenererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingAarsak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingResultat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortrolig
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortroligStrict

fun Klagevedtak.genererSaksstatistikk(): GenererSaksstatistikk {
    return GenererSaksstatistikk { gjelderKode6, versjon, clock ->
        val gjelderKode6 = gjelderKode6(this.fnr)
        SaksstatistikkDTO(
            sakId = this.behandling.sakId.toString(),
            saksnummer = this.behandling.saksnummer.toString(),
            behandlingId = this.behandling.id.toString(),
            relatertBehandlingId = this.behandling.resultat?.tilknyttetBehandlingId?.toString(),
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
            hendelse = StatistikkhendelseType.IVERKSATT_BEHANDLING.value,
            behandlingAarsak = StatistikkBehandlingAarsak.KLAGE,
        )
    }
}
