package no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.behandling.domene.vedtak.Vedtakstype
import java.time.Clock

fun genererStatistikkForNyFørstegangsbehandling(
    behandling: Behandling,
    gjelderKode6: Boolean,
    versjon: String,
    clock: Clock,
): no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO {
    return no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO(
        sakId = behandling.sakId.toString(),
        saksnummer = behandling.saksnummer.toString(),
        behandlingId = behandling.id.toString(),
        relatertBehandlingId = null,
        fnr = behandling.fnr.verdi,
        mottattTidspunkt = behandling.søknad!!.opprettet,
        registrertTidspunkt = behandling.opprettet,
        ferdigBehandletTidspunkt = null,
        vedtakTidspunkt = null,
        endretTidspunkt = nå(clock),
        utbetaltTidspunkt = null,
        tekniskTidspunkt = nå(clock),
        søknadsformat = no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.Format.DIGITAL.name,
        // TODO jah: Dette bør være et eget felt som utledes fra tiltaksperioden og kravfrist.
        forventetOppstartTidspunkt = behandling.saksopplysningsperiode?.fraOgMed,
        sakYtelse = "IND",
        behandlingType = no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingType.FØRSTEGANGSBEHANDLING,
        behandlingStatus = no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingStatus.UNDER_BEHANDLING,
        behandlingResultat = null,
        resultatBegrunnelse = null,
        behandlingMetode = no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingMetode.MANUELL.name,
        // skal være -5 for kode 6
        opprettetAv = if (gjelderKode6) "-5" else "system",
        saksbehandler = if (gjelderKode6) "-5" else behandling.saksbehandler,
        ansvarligBeslutter = if (gjelderKode6) "-5" else null,

        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        avsender = "tiltakspenger-saksbehandling-api",
        versjon = versjon,
        hendelse = "opprettet_behandling",
    )
}

fun genererSaksstatistikkForRammevedtak(
    vedtak: Rammevedtak,
    gjelderKode6: Boolean,
    versjon: String,
    clock: Clock,
): no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO {
    val behandling = vedtak.behandling
    return no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO(
        sakId = behandling.sakId.toString(),
        saksnummer = behandling.saksnummer.toString(),
        behandlingId = vedtak.behandling.id.toString(),
        // TODO jah: Denne vil vel kunne være en liste? Vi kan legge den på senere.
        relatertBehandlingId = null,
        fnr = behandling.fnr.verdi,
        mottattTidspunkt = if (behandling.erFørstegangsbehandling) behandling.søknad!!.opprettet else behandling.opprettet,
        registrertTidspunkt = behandling.opprettet,
        ferdigBehandletTidspunkt = vedtak.opprettet,
        vedtakTidspunkt = vedtak.opprettet,
        endretTidspunkt = vedtak.opprettet,
        utbetaltTidspunkt = null,
        tekniskTidspunkt = nå(clock),
        søknadsformat = no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.Format.DIGITAL.name,
        // TODO jah: Hva gjør vi ved revurdering/stans i dette tilfellet. Skal vi sende førstegangsbehandling sin første innvilget fraOgMed eller null?
        forventetOppstartTidspunkt = if (behandling.erFørstegangsbehandling) behandling.saksopplysningsperiode?.fraOgMed else null,
        sakYtelse = "IND",
        behandlingType = if (behandling.erFørstegangsbehandling) no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingType.FØRSTEGANGSBEHANDLING else no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingType.REVURDERING,
        // TODO jah: I følge confluence-dokken så finner jeg ikke dette feltet. Burde det heller vært AVSLUTTET?
        behandlingStatus = no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingStatus.FERDIG_BEHANDLET,
        behandlingResultat = when (vedtak.vedtaksType) {
            Vedtakstype.INNVILGELSE -> no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingResultat.INNVILGET
            Vedtakstype.STANS -> no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingResultat.STANS
        },
        // TODO jah: Denne bør ikke være null.
        resultatBegrunnelse = null,
        behandlingMetode = no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingMetode.MANUELL.name,

        // skal være -5 for kode 6
        opprettetAv = if (gjelderKode6) "-5" else "system",
        saksbehandler = if (gjelderKode6) "-5" else behandling.saksbehandler,
        ansvarligBeslutter = if (gjelderKode6) "-5" else behandling.beslutter,

        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        avsender = "tiltakspenger-saksbehandling-api",
        versjon = versjon,
        hendelse = "iverksatt_behandling",
    )
}
