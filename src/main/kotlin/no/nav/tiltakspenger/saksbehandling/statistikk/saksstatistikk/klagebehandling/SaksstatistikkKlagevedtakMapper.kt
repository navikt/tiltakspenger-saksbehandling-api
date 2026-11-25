package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.statistikk.GenererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingAarsak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingResultat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.ansvar

fun Klagevedtak.genererAvvistSaksstatistikk(): GenererSaksstatistikk {
    return GenererSaksstatistikk { skjermingsoppslag, versjon ->
        val ansvar = skjermingsoppslag.ansvar(fnr = this.fnr, saksbehandler = this.behandling.saksbehandler, ansvarligBeslutter = null)
        SaksstatistikkDTO(
            sakId = this.behandling.sakId.toString(),
            saksnummer = this.behandling.saksnummer.toString(),
            behandlingId = this.behandling.id.toString(),
            relatertBehandlingId = this.behandling.formkrav.behandlingDetKlagesPå?.toString(),
            fnr = this.behandling.fnr.verdi,
            mottattTidspunkt = this.behandling.klagensJournalpostOpprettet,
            registrertTidspunkt = this.behandling.opprettet,
            ferdigBehandletTidspunkt = this.opprettet,
            vedtakTidspunkt = this.opprettet,
            endretTidspunkt = this.opprettet,
            utbetaltTidspunkt = null,
            søknadsformat = this.behandling.formkrav.innsendingskilde.toStatistikkFormat(),
            forventetOppstartTidspunkt = null,
            behandlingType = StatistikkBehandlingType.KLAGE,
            behandlingStatus = StatistikkBehandlingStatus.FERDIG_BEHANDLET,
            behandlingResultat = StatistikkBehandlingResultat.AVVIST,
            resultatBegrunnelse = null,
            opprettetAv = ansvar.opprettetAv,
            saksbehandler = ansvar.saksbehandler,
            ansvarligBeslutter = null,
            // TODO jah: Er 0383 for egen ansatt.
            ansvarligenhet = ansvar.ansvarligenhet,
            tilbakekrevingsbeløp = null,
            funksjonellPeriodeFom = null,
            funksjonellPeriodeTom = null,
            versjon = versjon,
            hendelse = StatistikkhendelseType.IVERKSATT_BEHANDLING.value,
            behandlingAarsak = StatistikkBehandlingAarsak.KLAGE,
        )
    }
}
