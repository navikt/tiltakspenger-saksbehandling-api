package no.nav.tiltakspenger.saksbehandling.statistikk.behandling

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import java.time.Clock

fun genererSaksstatistikkForRammevedtak(
    vedtak: Rammevedtak,
    gjelderKode6: Boolean,
    versjon: String,
    clock: Clock,
): StatistikkSakDTO {
    val behandling = vedtak.behandling
    return StatistikkSakDTO(
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
        søknadsformat = Format.DIGITAL.name,
        // TODO jah: Hva gjør vi ved revurdering/stans i dette tilfellet. Skal vi sende førstegangsbehandling sin første innvilget fraOgMed eller null?
        forventetOppstartTidspunkt = if (behandling.erFørstegangsbehandling) behandling.saksopplysningsperiode?.fraOgMed else null,
        behandlingType = if (behandling.erFørstegangsbehandling) BehandlingType.FØRSTEGANGSBEHANDLING else BehandlingType.REVURDERING,
        // TODO jah: I følge confluence-dokken så finner jeg ikke dette feltet. Burde det heller vært AVSLUTTET?
        behandlingStatus = BehandlingStatus.FERDIG_BEHANDLET,
        behandlingResultat = when (vedtak.vedtaksType) {
            Vedtakstype.INNVILGELSE -> BehandlingResultat.INNVILGET
            Vedtakstype.STANS -> BehandlingResultat.STANS
            Vedtakstype.AVSLAG -> BehandlingResultat.AVSLAG
        },
        // TODO jah: Denne bør ikke være null.
        resultatBegrunnelse = null,

        // skal være -5 for kode 6
        opprettetAv = if (gjelderKode6) "-5" else "system",
        saksbehandler = if (gjelderKode6) "-5" else behandling.saksbehandler,
        ansvarligBeslutter = if (gjelderKode6) "-5" else behandling.beslutter,

        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        versjon = versjon,
        hendelse = "iverksatt_behandling",
        behandlingAarsak = behandling.getBehandlingAarsak(),
    )
}

fun genererSaksstatistikkForBehandling(
    behandling: Behandling,
    gjelderKode6: Boolean,
    versjon: String,
    clock: Clock,
    hendelse: String,
): StatistikkSakDTO {
    return StatistikkSakDTO(
        sakId = behandling.sakId.toString(),
        saksnummer = behandling.saksnummer.toString(),
        behandlingId = behandling.id.toString(),
        relatertBehandlingId = null,
        fnr = behandling.fnr.verdi,
        mottattTidspunkt = if (behandling.erFørstegangsbehandling) behandling.søknad!!.opprettet else behandling.opprettet,
        registrertTidspunkt = behandling.opprettet,
        ferdigBehandletTidspunkt = behandling.avbrutt?.tidspunkt,
        vedtakTidspunkt = null,
        endretTidspunkt = nå(clock),
        utbetaltTidspunkt = null,
        tekniskTidspunkt = nå(clock),
        søknadsformat = Format.DIGITAL.name,
        forventetOppstartTidspunkt = if (behandling.erFørstegangsbehandling) behandling.saksopplysningsperiode?.fraOgMed else null,
        behandlingType = if (behandling.erFørstegangsbehandling) BehandlingType.FØRSTEGANGSBEHANDLING else BehandlingType.REVURDERING,
        behandlingStatus = if (behandling.erAvbrutt) {
            BehandlingStatus.AVSLUTTET
        } else if (behandling.status == Behandlingsstatus.KLAR_TIL_BESLUTNING || behandling.status == Behandlingsstatus.UNDER_BESLUTNING) {
            BehandlingStatus.UNDER_BESLUTNING
        } else {
            BehandlingStatus.UNDER_BEHANDLING
        },
        behandlingResultat = null,
        resultatBegrunnelse = null,
        // skal være -5 for kode 6
        opprettetAv = if (gjelderKode6) "-5" else "system",
        saksbehandler = if (gjelderKode6) "-5" else behandling.saksbehandler,
        ansvarligBeslutter = if (gjelderKode6) "-5" else behandling.beslutter,

        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        versjon = versjon,
        hendelse = hendelse,
        behandlingAarsak = behandling.getBehandlingAarsak(),
    )
}

private fun Behandling.getBehandlingAarsak(): BehandlingAarsak? {
    if (this.erFørstegangsbehandling) {
        return BehandlingAarsak.SOKNAD
    }
    if (this.erRevurdering && this.valgtHjemmelHarIkkeRettighet.isNotEmpty()) {
        val valgtHjemmel = valgtHjemmelHarIkkeRettighet.first()
        return valgtHjemmel.toBehandlingAarsak()
    }
    return null
}

private fun ValgtHjemmelHarIkkeRettighet.toBehandlingAarsak() =
    when (this) {
        ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> BehandlingAarsak.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK
        ValgtHjemmelForStans.Alder -> BehandlingAarsak.ALDER
        ValgtHjemmelForStans.Livsoppholdytelser -> BehandlingAarsak.LIVSOPPHOLDYTELSER
        ValgtHjemmelForStans.Institusjonsopphold -> BehandlingAarsak.INSTITUSJONSOPPHOLD
        ValgtHjemmelForStans.Kvalifiseringsprogrammet -> BehandlingAarsak.KVALIFISERINGSPROGRAMMET
        ValgtHjemmelForStans.Introduksjonsprogrammet -> BehandlingAarsak.INTRODUKSJONSPROGRAMMET
        ValgtHjemmelForStans.LønnFraTiltaksarrangør -> BehandlingAarsak.LONN_FRA_TILTAKSARRANGOR
        ValgtHjemmelForStans.LønnFraAndre -> BehandlingAarsak.LONN_FRA_ANDRE
        else -> null
    }
