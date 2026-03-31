package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.statistikk.GenererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingAarsak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingResultat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortrolig
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.maskerHvisStrengtFortroligStrict

fun Klagebehandling.genererSaksstatistikk(
    hendelse: StatistikkhendelseType,
): GenererSaksstatistikk {
    return GenererSaksstatistikk { gjelderKode6, versjon, clock ->
        val gjelderKode6 = gjelderKode6(this.fnr)
        SaksstatistikkDTO(
            sakId = this.sakId.toString(),
            saksnummer = this.saksnummer.toString(),
            behandlingId = this.id.toString(),
            relatertBehandlingId = this.formkrav.behandlingDetKlagesPå?.toString(),
            fnr = this.fnr.verdi,
            mottattTidspunkt = this.klagensJournalpostOpprettet,
            registrertTidspunkt = this.opprettet,
            ferdigBehandletTidspunkt = this.avbrutt?.tidspunkt,
            vedtakTidspunkt = null,
            endretTidspunkt = this.sistEndret,
            utbetaltTidspunkt = null,
            tekniskTidspunkt = nå(clock),
            søknadsformat = this.formkrav.innsendingskilde.toStatistikkFormat(),
            forventetOppstartTidspunkt = null,
            behandlingType = StatistikkBehandlingType.KLAGE,
            behandlingStatus = status.tilStatistikkBehandlingStatus(),
            behandlingResultat = if (this.erAvbrutt) {
                StatistikkBehandlingResultat.AVBRUTT
            } else {
                when (this.resultat) {
                    is Klagebehandlingsresultat.Omgjør -> StatistikkBehandlingResultat.MEDHOLD
                    is Klagebehandlingsresultat.Opprettholdt -> StatistikkBehandlingResultat.OPPRETTHOLDT
                    is Klagebehandlingsresultat.Avvist -> StatistikkBehandlingResultat.AVVIST
                    null -> null
                }
            },
            resultatBegrunnelse = if (this.resultat is Klagebehandlingsresultat.Omgjør) this.resultat.årsak.tilResultatBegrunnelse().name else null,
            // skal være -5 for kode 6
            opprettetAv = maskerHvisStrengtFortroligStrict(gjelderKode6, "system"),
            saksbehandler = maskerHvisStrengtFortrolig(gjelderKode6, this.saksbehandler),
            ansvarligBeslutter = null,
            ansvarligenhet = maskerHvisStrengtFortroligStrict(gjelderKode6, "0387"),

            tilbakekrevingsbeløp = null,
            funksjonellPeriodeFom = null,
            funksjonellPeriodeTom = null,
            versjon = versjon,
            hendelse = hendelse.value,
            behandlingAarsak = StatistikkBehandlingAarsak.KLAGE,
        )
    }
}

private fun Klagebehandlingsstatus.tilStatistikkBehandlingStatus(): StatistikkBehandlingStatus = when (this) {
    Klagebehandlingsstatus.AVBRUTT -> StatistikkBehandlingStatus.AVSLUTTET
    Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> StatistikkBehandlingStatus.KLAR_TIL_BEHANDLING
    Klagebehandlingsstatus.UNDER_BEHANDLING -> StatistikkBehandlingStatus.UNDER_BEHANDLING
    Klagebehandlingsstatus.VEDTATT -> StatistikkBehandlingStatus.FERDIG_BEHANDLET
    Klagebehandlingsstatus.OPPRETTHOLDT -> StatistikkBehandlingStatus.OVERSENDT_KA
    Klagebehandlingsstatus.OVERSENDT -> StatistikkBehandlingStatus.OVERSENDT_KA
    Klagebehandlingsstatus.MOTTATT_FRA_KLAGEINSTANS -> StatistikkBehandlingStatus.UNDER_BEHANDLING
    Klagebehandlingsstatus.FERDIGSTILT -> StatistikkBehandlingStatus.FERDIG_BEHANDLET
    Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS -> StatistikkBehandlingStatus.UNDER_BEHANDLING
    Klagebehandlingsstatus.OVERSEND_FEILET -> throw IllegalStateException("Skal ikke generere statistikk for status $this")
}
