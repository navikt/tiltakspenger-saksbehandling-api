package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.statistikk.GenererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingAarsak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingResultat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkFormat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortrolig
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortroligStrict

fun Rammebehandling.genererSaksstatistikk(
    hendelse: StatistikkhendelseType,
): GenererSaksstatistikk {
    return GenererSaksstatistikk { gjelderKode6, versjon, clock ->
        val erSøknadsbehandling = this is Søknadsbehandling
        val gjelderKode6 = gjelderKode6(this.fnr)
        SaksstatistikkDTO(
            sakId = this.sakId.toString(),
            saksnummer = this.saksnummer.toString(),
            behandlingId = this.id.toString(),
            relatertBehandlingId = null,
            fnr = this.fnr.verdi,
            mottattTidspunkt = if (erSøknadsbehandling) this.søknad.opprettet else this.opprettet,
            registrertTidspunkt = this.opprettet,
            ferdigBehandletTidspunkt = this.avbrutt?.tidspunkt,
            vedtakTidspunkt = null,
            endretTidspunkt = this.sistEndret,
            utbetaltTidspunkt = null,
            tekniskTidspunkt = nå(clock),
            søknadsformat = this.getSoknadsformat(),
            forventetOppstartTidspunkt = if (erSøknadsbehandling) this.vedtaksperiode?.fraOgMed else null,
            behandlingType = if (erSøknadsbehandling) StatistikkBehandlingType.SØKNADSBEHANDLING else StatistikkBehandlingType.REVURDERING,
            behandlingStatus = if (this.erAvbrutt) {
                StatistikkBehandlingStatus.AVSLUTTET
            } else if (this.status == Rammebehandlingsstatus.KLAR_TIL_BESLUTNING || this.status == Rammebehandlingsstatus.UNDER_BESLUTNING) {
                StatistikkBehandlingStatus.UNDER_BESLUTNING
            } else {
                StatistikkBehandlingStatus.UNDER_BEHANDLING
            },
            behandlingResultat = if (this.erAvbrutt) {
                StatistikkBehandlingResultat.AVBRUTT
            } else {
                null
            },
            resultatBegrunnelse = null,

            // skal være -5 for kode 6
            opprettetAv = maskerHvisStrengtFortroligStrict(gjelderKode6, "system"),
            saksbehandler = maskerHvisStrengtFortrolig(gjelderKode6, this.saksbehandler),
            ansvarligBeslutter = maskerHvisStrengtFortrolig(gjelderKode6, this.beslutter),
            ansvarligenhet = maskerHvisStrengtFortroligStrict(gjelderKode6, "0387"),

            tilbakekrevingsbeløp = null,
            funksjonellPeriodeFom = null,
            funksjonellPeriodeTom = null,
            versjon = versjon,
            hendelse = hendelse.value,
            behandlingAarsak = this.getBehandlingAarsak(),
        )
    }
}

fun Rammebehandling.getSoknadsformat(): StatistikkFormat {
    return when (this) {
        is Søknadsbehandling -> this.søknad.søknadstype.toStatistikkFormat()
        is Revurdering -> StatistikkFormat.DIGITAL
    }
}

fun Rammebehandling.getBehandlingAarsak(): StatistikkBehandlingAarsak? {
    return when (this) {
        is Søknadsbehandling -> {
            this.søknad.behandlingsarsak?.toStatistikkBehandlingAarsak() ?: StatistikkBehandlingAarsak.SOKNAD
        }

        is Revurdering -> {
            when (this.resultat) {
                is Revurderingsresultat.Stans -> {
                    resultat.valgtHjemmel?.first()?.toBehandlingAarsak()
                }

                is Omgjøringsresultat.OmgjøringOpphør -> {
                    resultat.valgteHjemler.first().toBehandlingAarsak()
                }

                is Omgjøringsresultat.OmgjøringIkkeValgt,
                is Omgjøringsresultat.OmgjøringInnvilgelse,
                is Revurderingsresultat.Innvilgelse,
                -> null
            }
        }
    }
}
