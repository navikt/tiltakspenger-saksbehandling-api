package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling

import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingAarsak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkFormat
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Behandlingsarsak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype

fun HjemmelForStans.toBehandlingAarsak(): StatistikkBehandlingAarsak {
    return when (this) {
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
}

fun HjemmelForOpphør.toBehandlingAarsak(): StatistikkBehandlingAarsak {
    return when (this) {
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
}

fun Søknadstype.toStatistikkFormat(): StatistikkFormat {
    return when (this) {
        Søknadstype.DIGITAL -> StatistikkFormat.DIGITAL
        Søknadstype.PAPIR_SKJEMA -> StatistikkFormat.PAPIR_SKJEMA
        Søknadstype.PAPIR_FRIHAND -> StatistikkFormat.PAPIR_FRIHAND
        Søknadstype.MODIA -> StatistikkFormat.MODIA
        Søknadstype.ANNET -> StatistikkFormat.ANNET
    }
}

fun Behandlingsarsak.toStatistikkBehandlingAarsak(): StatistikkBehandlingAarsak {
    return when (this) {
        Behandlingsarsak.FORLENGELSE_FRA_ARENA -> StatistikkBehandlingAarsak.FORLENGELSE_FRA_ARENA
        Behandlingsarsak.SOKNADSBEHANDLING_FRA_ARENA -> StatistikkBehandlingAarsak.SOKNADSBEHANDLING_FRA_ARENA
        Behandlingsarsak.OVERLAPPENDE_TILTAK_I_ARENA -> StatistikkBehandlingAarsak.OVERLAPPENDE_TILTAK_I_ARENA
        Behandlingsarsak.ANNET -> StatistikkBehandlingAarsak.OVERFORT_FRA_ARENA
    }
}
