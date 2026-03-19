package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.statistikk.GenererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingResultat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortrolig
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortroligStrict
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

fun Rammevedtak.genererSaksstatistikk(): GenererSaksstatistikk {
    return GenererSaksstatistikk { gjelderKode6, versjon, clock ->
        val behandling = this.rammebehandling
        val erSøknadsbehandling = behandling is Søknadsbehandling
        val gjelderKode6 = gjelderKode6(this.fnr)
        SaksstatistikkDTO(
            sakId = behandling.sakId.toString(),
            saksnummer = behandling.saksnummer.toString(),
            behandlingId = behandling.id.toString(),
            // TODO jah: Denne vil vel kunne være en liste? Vi kan legge den på senere.
            relatertBehandlingId = null,
            fnr = behandling.fnr.verdi,
            mottattTidspunkt = if (erSøknadsbehandling) behandling.søknad.opprettet else behandling.opprettet,
            registrertTidspunkt = behandling.opprettet,
            ferdigBehandletTidspunkt = this.opprettet,
            vedtakTidspunkt = this.opprettet,
            endretTidspunkt = this.opprettet,
            utbetaltTidspunkt = null,
            tekniskTidspunkt = nå(clock),
            søknadsformat = behandling.getSoknadsformat(),
            // TODO jah: Hva gjør vi ved revurdering/stans i dette tilfellet. Skal vi sende søknadsbehandling sin første innvilget fraOgMed eller null?
            forventetOppstartTidspunkt = if (erSøknadsbehandling) behandling.vedtaksperiode?.fraOgMed else null,
            behandlingType = if (erSøknadsbehandling) StatistikkBehandlingType.SØKNADSBEHANDLING else StatistikkBehandlingType.REVURDERING,
            behandlingStatus = StatistikkBehandlingStatus.FERDIG_BEHANDLET,
            behandlingResultat = when (this.rammebehandlingsresultat) {
                // I førsteomgang mapper vi bare delvis til innvilgelse.
                is Rammebehandlingsresultat.Innvilgelse -> StatistikkBehandlingResultat.INNVILGET

                is Revurderingsresultat.Stans -> StatistikkBehandlingResultat.STANS

                is Søknadsbehandlingsresultat.Avslag -> StatistikkBehandlingResultat.AVSLAG

                is Omgjøringsresultat.OmgjøringOpphør -> StatistikkBehandlingResultat.OPPHØRT

                is Rammebehandlingsresultat.IkkeValgt -> this.rammebehandlingsresultat.vedtakError()
            },
            // TODO jah: Denne bør ikke være null.
            resultatBegrunnelse = null,

            // skal være -5 for kode 6
            opprettetAv = maskerHvisStrengtFortroligStrict(gjelderKode6, "system"),
            saksbehandler = maskerHvisStrengtFortrolig(gjelderKode6, behandling.saksbehandler),
            ansvarligBeslutter = maskerHvisStrengtFortrolig(gjelderKode6, behandling.beslutter),
            // TODO jah: Er 0383 for egen ansatt.
            ansvarligenhet = maskerHvisStrengtFortroligStrict(gjelderKode6, "0387"),

            tilbakekrevingsbeløp = null,
            funksjonellPeriodeFom = null,
            funksjonellPeriodeTom = null,
            versjon = versjon,
            hendelse = StatistikkhendelseType.IVERKSATT_BEHANDLING.value,
            behandlingAarsak = behandling.getBehandlingAarsak(),
        )
    }
}
