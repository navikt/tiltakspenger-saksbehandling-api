package no.nav.tiltakspenger.saksbehandling.service.statistikk.sak

import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Vedtakstype

fun genererStatistikkForNyFørstegangsbehandling(
    behandling: Behandling,
    gjelderKode6: Boolean,
    versjon: String,
): StatistikkSakDTO {
    return StatistikkSakDTO(
        sakId = behandling.sakId.toString(),
        saksnummer = behandling.saksnummer.toString(),
        behandlingId = behandling.id.toString(),
        relatertBehandlingId = null,
        ident = behandling.fnr.verdi,
        mottattTidspunkt = behandling.søknad!!.opprettet,
        registrertTidspunkt = behandling.opprettet,
        ferdigBehandletTidspunkt = null,
        vedtakTidspunkt = null,
        endretTidspunkt = nå(),
        utbetaltTidspunkt = null,
        tekniskTidspunkt = nå(),
        søknadsformat = Format.DIGITAL.name,
        // TODO jah: Dette bør være et eget felt som utledes fra tiltaksperioden og kravfrist.
        forventetOppstartTidspunkt = behandling.saksopplysningsperiode?.fraOgMed,
        vilkår = emptyList(),
        sakYtelse = "IND",
        sakUtland = "N",
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        behandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
        behandlingResultat = null,
        resultatBegrunnelse = null,
        behandlingMetode = BehandlingMetode.MANUELL.name,
        // skal være -5 for kode 6
        opprettetAv = if (gjelderKode6) "-5" else "system",
        saksbehandler = if (gjelderKode6) "-5" else behandling.saksbehandler,
        ansvarligBeslutter = if (gjelderKode6) "-5" else null,
        ansvarligEnhet = if (gjelderKode6) "-5" else null,

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
): StatistikkSakDTO {
    val behandling = vedtak.behandling
    return StatistikkSakDTO(
        sakId = behandling.sakId.toString(),
        saksnummer = behandling.saksnummer.toString(),
        behandlingId = vedtak.behandling.id.toString(),
        // TODO jah: Denne vil vel kunne være en liste? Vi kan legge den på senere.
        relatertBehandlingId = null,
        ident = behandling.fnr.verdi,
        mottattTidspunkt = if (behandling.erFørstegangsbehandling) behandling.søknad!!.opprettet else behandling.opprettet,
        registrertTidspunkt = behandling.opprettet,
        ferdigBehandletTidspunkt = vedtak.opprettet,
        vedtakTidspunkt = vedtak.opprettet,
        endretTidspunkt = vedtak.opprettet,
        utbetaltTidspunkt = null,
        tekniskTidspunkt = nå(),
        søknadsformat = Format.DIGITAL.name,
        // TODO jah: Hva gjør vi ved revurdering/stans i dette tilfellet. Skal vi sende førstegangsbehandling sin første innvilget fraOgMed eller null?
        forventetOppstartTidspunkt = if (behandling.erFørstegangsbehandling) behandling.saksopplysningsperiode?.fraOgMed else null,
        vilkår = emptyList(),
        sakYtelse = "IND",
        sakUtland = "N",
        behandlingType = if (behandling.erFørstegangsbehandling) BehandlingType.FØRSTEGANGSBEHANDLING else BehandlingType.REVURDERING,
        // TODO jah: I følge confluence-dokken så finner jeg ikke dette feltet. Burde det heller vært AVSLUTTET?
        behandlingStatus = BehandlingStatus.FERDIG_BEHANDLET,
        behandlingResultat = when (vedtak.vedtaksType) {
            Vedtakstype.INNVILGELSE -> BehandlingResultat.INNVILGET
            Vedtakstype.STANS -> BehandlingResultat.STANS
        },
        // TODO jah: Denne bør ikke være null.
        resultatBegrunnelse = null,
        behandlingMetode = BehandlingMetode.MANUELL.name,

        // skal være -5 for kode 6
        opprettetAv = if (gjelderKode6) "-5" else "system",
        saksbehandler = if (gjelderKode6) "-5" else behandling.saksbehandler,
        ansvarligBeslutter = if (gjelderKode6) "-5" else behandling.beslutter,
        ansvarligEnhet = if (gjelderKode6) "-5" else "må hentes fra NORG",

        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        avsender = "tiltakspenger-saksbehandling-api",
        versjon = versjon,
        hendelse = "iverksatt_behandling",
    )
}
