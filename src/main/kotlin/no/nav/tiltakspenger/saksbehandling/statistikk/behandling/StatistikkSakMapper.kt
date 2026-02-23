package no.nav.tiltakspenger.saksbehandling.statistikk.behandling

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Behandlingsarsak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock

fun genererSaksstatistikkForRammevedtak(
    vedtak: Rammevedtak,
    gjelderKode6: Boolean,
    versjon: String,
    clock: Clock,
): StatistikkSakDTO {
    val behandling = vedtak.rammebehandling
    val erSøknadsbehandling = behandling is Søknadsbehandling

    return StatistikkSakDTO(
        sakId = behandling.sakId.toString(),
        saksnummer = behandling.saksnummer.toString(),
        behandlingId = vedtak.rammebehandling.id.toString(),
        // TODO jah: Denne vil vel kunne være en liste? Vi kan legge den på senere.
        relatertBehandlingId = null,
        fnr = behandling.fnr.verdi,
        mottattTidspunkt = if (erSøknadsbehandling) behandling.søknad.opprettet else behandling.opprettet,
        registrertTidspunkt = behandling.opprettet,
        ferdigBehandletTidspunkt = vedtak.opprettet,
        vedtakTidspunkt = vedtak.opprettet,
        endretTidspunkt = vedtak.opprettet,
        utbetaltTidspunkt = null,
        tekniskTidspunkt = nå(clock),
        søknadsformat = behandling.getSoknadsformat(),
        // TODO jah: Hva gjør vi ved revurdering/stans i dette tilfellet. Skal vi sende søknadsbehandling sin første innvilget fraOgMed eller null?
        forventetOppstartTidspunkt = if (erSøknadsbehandling) behandling.vedtaksperiode?.fraOgMed else null,
        behandlingType = if (erSøknadsbehandling) StatistikkBehandlingType.SØKNADSBEHANDLING else StatistikkBehandlingType.REVURDERING,
        // TODO jah: I følge confluence-dokken så finner jeg ikke dette feltet. Burde det heller vært AVSLUTTET?
        behandlingStatus = StatistikkBehandlingStatus.FERDIG_BEHANDLET,
        behandlingResultat = when (vedtak.rammebehandlingsresultat) {
            // I førsteomgang mapper vi bare delvis til innvilgelse.
            is Rammebehandlingsresultat.Innvilgelse -> StatistikkBehandlingResultat.INNVILGET

            is Revurderingsresultat.Stans -> StatistikkBehandlingResultat.STANS

            is Søknadsbehandlingsresultat.Avslag -> StatistikkBehandlingResultat.AVSLAG

            is Omgjøringsresultat.OmgjøringOpphør -> StatistikkBehandlingResultat.OPPHØRT

            is Rammebehandlingsresultat.IkkeValgt -> vedtak.rammebehandlingsresultat.vedtakError()
        },
        // TODO jah: Denne bør ikke være null.
        resultatBegrunnelse = null,

        // skal være -5 for kode 6
        opprettetAv = if (gjelderKode6) "-5" else "system",
        saksbehandler = if (gjelderKode6) "-5" else behandling.saksbehandler,
        ansvarligBeslutter = if (gjelderKode6) "-5" else behandling.beslutter,
        ansvarligenhet = if (gjelderKode6) "-5" else "0387",

        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        versjon = versjon,
        hendelse = "iverksatt_behandling",
        behandlingAarsak = behandling.getBehandlingAarsak(),
    )
}

fun genererSaksstatistikkForBehandling(
    behandling: Rammebehandling,
    gjelderKode6: Boolean,
    versjon: String,
    clock: Clock,
    hendelse: String,
): StatistikkSakDTO {
    val erSøknadsbehandling = behandling is Søknadsbehandling

    return StatistikkSakDTO(
        sakId = behandling.sakId.toString(),
        saksnummer = behandling.saksnummer.toString(),
        behandlingId = behandling.id.toString(),
        relatertBehandlingId = null,
        fnr = behandling.fnr.verdi,
        mottattTidspunkt = if (erSøknadsbehandling) behandling.søknad.opprettet else behandling.opprettet,
        registrertTidspunkt = behandling.opprettet,
        ferdigBehandletTidspunkt = behandling.avbrutt?.tidspunkt,
        vedtakTidspunkt = null,
        endretTidspunkt = behandling.sistEndret,
        utbetaltTidspunkt = null,
        tekniskTidspunkt = nå(clock),
        søknadsformat = behandling.getSoknadsformat(),
        forventetOppstartTidspunkt = if (erSøknadsbehandling) behandling.vedtaksperiode?.fraOgMed else null,
        behandlingType = if (erSøknadsbehandling) StatistikkBehandlingType.SØKNADSBEHANDLING else StatistikkBehandlingType.REVURDERING,
        behandlingStatus = if (behandling.erAvbrutt) {
            StatistikkBehandlingStatus.AVSLUTTET
        } else if (behandling.status == Rammebehandlingsstatus.KLAR_TIL_BESLUTNING || behandling.status == Rammebehandlingsstatus.UNDER_BESLUTNING) {
            StatistikkBehandlingStatus.UNDER_BESLUTNING
        } else {
            StatistikkBehandlingStatus.UNDER_BEHANDLING
        },
        behandlingResultat = if (behandling.erAvbrutt) {
            StatistikkBehandlingResultat.AVBRUTT
        } else {
            null
        },
        resultatBegrunnelse = null,
        // skal være -5 for kode 6
        opprettetAv = if (gjelderKode6) "-5" else "system",
        saksbehandler = if (gjelderKode6) "-5" else behandling.saksbehandler,
        ansvarligBeslutter = if (gjelderKode6) "-5" else behandling.beslutter,
        ansvarligenhet = if (gjelderKode6) "-5" else "0387",

        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        versjon = versjon,
        hendelse = hendelse,
        behandlingAarsak = behandling.getBehandlingAarsak(),
    )
}

private fun Rammebehandling.getSoknadsformat(): StatistikkFormat {
    return when (this) {
        is Søknadsbehandling -> this.søknad.søknadstype.toSøknadsformat()
        is Revurdering -> StatistikkFormat.DIGITAL
    }
}

private fun Rammebehandling.getBehandlingAarsak(): StatistikkBehandlingAarsak? {
    return when (this) {
        is Søknadsbehandling -> {
            return this.søknad.behandlingsarsak?.toStatistikkBehandlingAarsak()
                ?: StatistikkBehandlingAarsak.SOKNAD
        }

        is Revurdering -> {
            when (this.resultat) {
                is Revurderingsresultat.Stans -> {
                    return resultat.valgtHjemmel?.first()?.toBehandlingAarsak()
                }

                is Omgjøringsresultat.OmgjøringOpphør -> {
                    return resultat.valgteHjemler.first().toBehandlingAarsak()
                }

                is Omgjøringsresultat.OmgjøringIkkeValgt,
                is Omgjøringsresultat.OmgjøringInnvilgelse,
                is Revurderingsresultat.Innvilgelse,
                -> null
            }
        }
    }
}

private fun HjemmelForStans.toBehandlingAarsak() =
    when (this) {
        HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> StatistikkBehandlingAarsak.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK
        HjemmelForStans.Alder -> StatistikkBehandlingAarsak.ALDER
        HjemmelForStans.Livsoppholdytelser -> StatistikkBehandlingAarsak.LIVSOPPHOLDYTELSER
        HjemmelForStans.Institusjonsopphold -> StatistikkBehandlingAarsak.INSTITUSJONSOPPHOLD
        HjemmelForStans.Kvalifiseringsprogrammet -> StatistikkBehandlingAarsak.KVALIFISERINGSPROGRAMMET
        HjemmelForStans.Introduksjonsprogrammet -> StatistikkBehandlingAarsak.INTRODUKSJONSPROGRAMMET
        HjemmelForStans.LønnFraTiltaksarrangør -> StatistikkBehandlingAarsak.LONN_FRA_TILTAKSARRANGOR
        HjemmelForStans.LønnFraAndre -> StatistikkBehandlingAarsak.LONN_FRA_ANDRE
        HjemmelForStans.IkkeLovligOpphold -> StatistikkBehandlingAarsak.IKKE_LOVLIG_OPPHOLD
    }

private fun HjemmelForOpphør.toBehandlingAarsak() =
    when (this) {
        HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak -> StatistikkBehandlingAarsak.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK
        HjemmelForOpphør.Alder -> StatistikkBehandlingAarsak.ALDER
        HjemmelForOpphør.Livsoppholdytelser -> StatistikkBehandlingAarsak.LIVSOPPHOLDYTELSER
        HjemmelForOpphør.Institusjonsopphold -> StatistikkBehandlingAarsak.INSTITUSJONSOPPHOLD
        HjemmelForOpphør.Kvalifiseringsprogrammet -> StatistikkBehandlingAarsak.KVALIFISERINGSPROGRAMMET
        HjemmelForOpphør.Introduksjonsprogrammet -> StatistikkBehandlingAarsak.INTRODUKSJONSPROGRAMMET
        HjemmelForOpphør.LønnFraTiltaksarrangør -> StatistikkBehandlingAarsak.LONN_FRA_TILTAKSARRANGOR
        HjemmelForOpphør.LønnFraAndre -> StatistikkBehandlingAarsak.LONN_FRA_ANDRE
        HjemmelForOpphør.IkkeLovligOpphold -> StatistikkBehandlingAarsak.IKKE_LOVLIG_OPPHOLD
        HjemmelForOpphør.FremmetForSent -> StatistikkBehandlingAarsak.FREMMET_FOR_SENT
    }

private fun Søknadstype.toSøknadsformat(): StatistikkFormat =
    when (this) {
        Søknadstype.DIGITAL -> StatistikkFormat.DIGITAL
        Søknadstype.PAPIR_SKJEMA -> StatistikkFormat.PAPIR_SKJEMA
        Søknadstype.PAPIR_FRIHAND -> StatistikkFormat.PAPIR_FRIHAND
        Søknadstype.MODIA -> StatistikkFormat.MODIA
        Søknadstype.ANNET -> StatistikkFormat.ANNET
    }

private fun Behandlingsarsak.toStatistikkBehandlingAarsak(): StatistikkBehandlingAarsak {
    return when (this) {
        Behandlingsarsak.FORLENGELSE_FRA_ARENA -> StatistikkBehandlingAarsak.FORLENGELSE_FRA_ARENA
        Behandlingsarsak.SOKNADSBEHANDLING_FRA_ARENA -> StatistikkBehandlingAarsak.SOKNADSBEHANDLING_FRA_ARENA
        Behandlingsarsak.OVERLAPPENDE_TILTAK_I_ARENA -> StatistikkBehandlingAarsak.OVERLAPPENDE_TILTAK_I_ARENA
        Behandlingsarsak.ANNET -> StatistikkBehandlingAarsak.OVERFORT_FRA_ARENA
    }
}
