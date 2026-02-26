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
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
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
        opprettetAv = maskerHvisStrengtFortroligStrict(gjelderKode6, "system"),
        saksbehandler = maskerHvisStrengtFortrolig(gjelderKode6, behandling.saksbehandler),
        ansvarligBeslutter = maskerHvisStrengtFortrolig(gjelderKode6, behandling.beslutter),
        ansvarligenhet = maskerHvisStrengtFortroligStrict(gjelderKode6, "0387"),

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
        opprettetAv = maskerHvisStrengtFortroligStrict(gjelderKode6, "system"),
        saksbehandler = maskerHvisStrengtFortrolig(gjelderKode6, behandling.saksbehandler),
        ansvarligBeslutter = maskerHvisStrengtFortrolig(gjelderKode6, behandling.beslutter),
        ansvarligenhet = maskerHvisStrengtFortroligStrict(gjelderKode6, "0387"),

        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        versjon = versjon,
        hendelse = hendelse,
        behandlingAarsak = behandling.getBehandlingAarsak(),
    )
}

fun genererSaksstatistikkForKlagebehandling(
    behandling: Klagebehandling,
    gjelderKode6: Boolean,
    versjon: String,
    clock: Clock,
    hendelse: String,
): StatistikkSakDTO {
    return StatistikkSakDTO(
        sakId = behandling.sakId.toString(),
        saksnummer = behandling.saksnummer.toString(),
        behandlingId = behandling.id.toString(),
        relatertBehandlingId = behandling.resultat?.rammebehandlingId?.toString(),
        fnr = behandling.fnr.verdi,
        mottattTidspunkt = behandling.klagensJournalpostOpprettet,
        registrertTidspunkt = behandling.opprettet,
        ferdigBehandletTidspunkt = behandling.avbrutt?.tidspunkt,
        vedtakTidspunkt = null,
        endretTidspunkt = behandling.sistEndret,
        utbetaltTidspunkt = null,
        tekniskTidspunkt = nå(clock),
        søknadsformat = behandling.formkrav.innsendingskilde.toStatistikkFormat(),
        forventetOppstartTidspunkt = null,
        behandlingType = StatistikkBehandlingType.KLAGE,
        behandlingStatus = if (behandling.erAvbrutt) {
            StatistikkBehandlingStatus.AVSLUTTET
        } else if (behandling.ventestatus.erSattPåVent) {
            StatistikkBehandlingStatus.VENTENDE_SAKER
        } else {
            when (behandling.status) {
                Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> StatistikkBehandlingStatus.KLAR_TIL_BEHANDLING
                Klagebehandlingsstatus.UNDER_BEHANDLING -> StatistikkBehandlingStatus.UNDER_BEHANDLING
                Klagebehandlingsstatus.OPPRETTHOLDT -> StatistikkBehandlingStatus.OVERSENDT_KA
                Klagebehandlingsstatus.AVBRUTT -> StatistikkBehandlingStatus.AVSLUTTET
                Klagebehandlingsstatus.VEDTATT -> StatistikkBehandlingStatus.FERDIG_BEHANDLET
                Klagebehandlingsstatus.OVERSENDT -> throw IllegalStateException("Vi sender ikke statistikk på at en sak venter på å bli plukket opp av jobben som sender klager til klageinstansen.")
                Klagebehandlingsstatus.FERDIGSTILT -> throw IllegalStateException("Vi sender ikke statistikk på at en sak er ferdigstilt, da det i praksis ikke finnes noen forskjell på ferdigstilt og oversendt til KA. Vi anser saken som avsluttet når den er oversendt til KA.")
            }
        },
        behandlingResultat = if (behandling.erAvbrutt) {
            StatistikkBehandlingResultat.AVBRUTT
        } else {
            when (behandling.resultat) {
                is Klagebehandlingsresultat.Omgjør -> StatistikkBehandlingResultat.MEDHOLD
                is Klagebehandlingsresultat.Opprettholdt -> StatistikkBehandlingResultat.OPPRETTHOLDT
                is Klagebehandlingsresultat.Avvist -> StatistikkBehandlingResultat.AVVIST
                null -> null
            }
        },
        resultatBegrunnelse = if (behandling.resultat is Klagebehandlingsresultat.Omgjør) behandling.resultat.årsak.tilResultatBegrunnelse().name else null,
        // skal være -5 for kode 6
        opprettetAv = maskerHvisStrengtFortroligStrict(gjelderKode6, "system"),
        saksbehandler = maskerHvisStrengtFortrolig(gjelderKode6, behandling.saksbehandler),
        ansvarligBeslutter = null,
        ansvarligenhet = maskerHvisStrengtFortroligStrict(gjelderKode6, "0387"),

        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        versjon = versjon,
        hendelse = hendelse,
        behandlingAarsak = StatistikkBehandlingAarsak.KLAGE,
    )
}

fun genererSaksstatistikkForKlagevedtak(
    vedtak: Klagevedtak,
    gjelderKode6: Boolean,
    versjon: String,
    clock: Clock,
): StatistikkSakDTO {
    val behandling = vedtak.behandling
    return StatistikkSakDTO(
        sakId = vedtak.behandling.sakId.toString(),
        saksnummer = behandling.saksnummer.toString(),
        behandlingId = behandling.id.toString(),
        relatertBehandlingId = behandling.resultat?.rammebehandlingId?.toString(),
        fnr = behandling.fnr.verdi,
        mottattTidspunkt = behandling.klagensJournalpostOpprettet,
        registrertTidspunkt = behandling.opprettet,
        ferdigBehandletTidspunkt = vedtak.opprettet,
        vedtakTidspunkt = vedtak.opprettet,
        endretTidspunkt = vedtak.opprettet,
        utbetaltTidspunkt = null,
        tekniskTidspunkt = nå(clock),
        søknadsformat = behandling.formkrav.innsendingskilde.toStatistikkFormat(),
        forventetOppstartTidspunkt = null,
        behandlingType = StatistikkBehandlingType.KLAGE,
        behandlingStatus = when (behandling.status) {
            Klagebehandlingsstatus.KLAR_TIL_BEHANDLING,
            Klagebehandlingsstatus.UNDER_BEHANDLING,
            Klagebehandlingsstatus.OPPRETTHOLDT,
            Klagebehandlingsstatus.OVERSENDT,
            -> throw IllegalStateException("Klagevedtaket må ende opp i en endelig status.")

            Klagebehandlingsstatus.AVBRUTT -> StatistikkBehandlingStatus.AVSLUTTET

            Klagebehandlingsstatus.VEDTATT -> StatistikkBehandlingStatus.FERDIG_BEHANDLET

            Klagebehandlingsstatus.FERDIGSTILT -> throw IllegalStateException("${behandling.status} er ikke en status som brukes for et klagevedtak. Dette skjedde for klagebehandling ${behandling.id}")
        },
        behandlingResultat = if (behandling.erAvbrutt) {
            StatistikkBehandlingResultat.AVBRUTT
        } else {
            when (behandling.resultat) {
                is Klagebehandlingsresultat.Omgjør -> StatistikkBehandlingResultat.MEDHOLD
                is Klagebehandlingsresultat.Opprettholdt -> StatistikkBehandlingResultat.OPPRETTHOLDT
                is Klagebehandlingsresultat.Avvist -> StatistikkBehandlingResultat.AVVIST
                null -> null
            }
        },
        resultatBegrunnelse = if (behandling.resultat is Klagebehandlingsresultat.Omgjør) behandling.resultat.årsak.tilResultatBegrunnelse().name else null,
        // skal være -5 for kode 6
        opprettetAv = maskerHvisStrengtFortroligStrict(gjelderKode6, "system"),
        saksbehandler = maskerHvisStrengtFortrolig(gjelderKode6, behandling.saksbehandler),
        ansvarligBeslutter = null,
        ansvarligenhet = maskerHvisStrengtFortroligStrict(gjelderKode6, "0387"),

        tilbakekrevingsbeløp = null,
        funksjonellPeriodeFom = null,
        funksjonellPeriodeTom = null,
        versjon = versjon,
        hendelse = "iverksatt_klagebehandling",
        behandlingAarsak = StatistikkBehandlingAarsak.KLAGE,
    )
}

fun maskerHvisStrengtFortrolig(
    erStrengtFortrolig: Boolean,
    verdi: String?,
): String? {
    return if (verdi != null) {
        return maskerHvisStrengtFortroligStrict(erStrengtFortrolig, verdi)
    } else {
        verdi
    }
}

fun maskerHvisStrengtFortroligStrict(
    erStrengtFortrolig: Boolean,
    verdi: String,
): String {
    return if (erStrengtFortrolig) {
        "-5"
    } else {
        verdi
    }
}

private fun Rammebehandling.getSoknadsformat(): StatistikkFormat {
    return when (this) {
        is Søknadsbehandling -> this.søknad.søknadstype.toStatistikkFormat()
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

private fun Søknadstype.toStatistikkFormat(): StatistikkFormat =
    when (this) {
        Søknadstype.DIGITAL -> StatistikkFormat.DIGITAL
        Søknadstype.PAPIR_SKJEMA -> StatistikkFormat.PAPIR_SKJEMA
        Søknadstype.PAPIR_FRIHAND -> StatistikkFormat.PAPIR_FRIHAND
        Søknadstype.MODIA -> StatistikkFormat.MODIA
        Søknadstype.ANNET -> StatistikkFormat.ANNET
    }

private fun KlageInnsendingskilde.toStatistikkFormat() =
    when (this) {
        KlageInnsendingskilde.DIGITAL -> StatistikkFormat.DIGITAL
        KlageInnsendingskilde.PAPIR -> StatistikkFormat.PAPIR
        KlageInnsendingskilde.MODIA -> StatistikkFormat.MODIA
        KlageInnsendingskilde.ANNET -> StatistikkFormat.ANNET
    }

private fun KlageOmgjøringsårsak.tilResultatBegrunnelse() =
    when (this) {
        KlageOmgjøringsårsak.FEIL_LOVANVENDELSE -> StatistikkResultatBegrunnelse.FEIL_LOVANVENDELSE
        KlageOmgjøringsårsak.FEIL_REGELVERKSFORSTAAELSE -> StatistikkResultatBegrunnelse.FEIL_REGELVERKSFORSTAAELSE
        KlageOmgjøringsårsak.FEIL_ELLER_ENDRET_FAKTA -> StatistikkResultatBegrunnelse.FEIL_ELLER_ENDRET_FAKTA
        KlageOmgjøringsårsak.PROSESSUELL_FEIL -> StatistikkResultatBegrunnelse.PROSESSUELL_FEIL
        KlageOmgjøringsårsak.ANNET -> StatistikkResultatBegrunnelse.ANNET
    }

private fun Behandlingsarsak.toStatistikkBehandlingAarsak(): StatistikkBehandlingAarsak {
    return when (this) {
        Behandlingsarsak.FORLENGELSE_FRA_ARENA -> StatistikkBehandlingAarsak.FORLENGELSE_FRA_ARENA
        Behandlingsarsak.SOKNADSBEHANDLING_FRA_ARENA -> StatistikkBehandlingAarsak.SOKNADSBEHANDLING_FRA_ARENA
        Behandlingsarsak.OVERLAPPENDE_TILTAK_I_ARENA -> StatistikkBehandlingAarsak.OVERLAPPENDE_TILTAK_I_ARENA
        Behandlingsarsak.ANNET -> StatistikkBehandlingAarsak.OVERFORT_FRA_ARENA
    }
}
