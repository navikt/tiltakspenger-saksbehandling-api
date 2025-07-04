package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.UtbetalingBeregning
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock
import java.time.LocalDateTime

/**
 * TODO (kanskje) abn: Split denne til "MeldekortBehandlingVedtak" og "Utbetaling".
 * MeldekortBehandlingVedtak vil da ha en Utbetaling, og Rammevedtak kan ha en Utbetaling ved revurdering som
 * fører til endring på beregningen av allerede utbetalte meldeperioder
 * */

/**
 * @property forrigeUtbetalingsvedtakId er null for første utbetalingsvedtak i en sak.
 * @param opprettet Tidspunktet vi instansierte og persisterte dette utbetalingsvedtaket første gangen. Dette har ingenting med vedtaksbrevet å gjøre.
 */
data class Utbetalingsvedtak(
    val id: VedtakId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val opprettet: LocalDateTime,
    val forrigeUtbetalingsvedtakId: VedtakId?,
    val sendtTilUtbetaling: LocalDateTime?,
    val journalpostId: JournalpostId?,
    val journalføringstidspunkt: LocalDateTime?,
    val status: Utbetalingsstatus?,
    val beregning: UtbetalingBeregning,
    val saksbehandler: String,
    val beslutter: String,
    val brukerNavkontor: Navkontor,
    val rammevedtak: List<VedtakId>?,
    val automatiskBehandlet: Boolean,
    val erKorrigering: Boolean,
    val begrunnelse: String?,
) {
    val periode: Periode = beregning.periode
    val ordinærBeløp: Int = beregning.ordinærBeløp
    val barnetilleggBeløp: Int = beregning.barnetilleggBeløp
    val totalBeløp: Int = beregning.totalBeløp
    val beregningKilde: BeregningKilde = beregning.beregningKilde

    fun oppdaterStatus(status: Utbetalingsstatus?): Utbetalingsvedtak {
        return this.copy(status = status)
    }
}

fun MeldekortBehandling.Behandlet.opprettUtbetalingsvedtak(
    saksnummer: Saksnummer,
    fnr: Fnr,
    forrigeUtbetalingsvedtak: Utbetalingsvedtak?,
    clock: Clock,
): Utbetalingsvedtak =
    Utbetalingsvedtak(
        id = VedtakId.random(),
        opprettet = nå(clock),
        sakId = this.sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        forrigeUtbetalingsvedtakId = forrigeUtbetalingsvedtak?.id,
        sendtTilUtbetaling = null,
        journalpostId = null,
        journalføringstidspunkt = null,
        status = null,
        beregning = this.beregning,
        saksbehandler = this.saksbehandler!!,
        beslutter = this.beslutter!!,
        brukerNavkontor = this.navkontor,
        rammevedtak = this.rammevedtak!!,
        automatiskBehandlet = this is MeldekortBehandletAutomatisk,
        erKorrigering = this.type == MeldekortBehandlingType.KORRIGERING,
        begrunnelse = this.begrunnelse?.verdi,
    )

fun Rammevedtak.opprettUtbetalingsvedtak(
    forrigeUtbetalingsvedtak: Utbetalingsvedtak?,
    clock: Clock,
): Utbetalingsvedtak {
    require(behandling.resultat is RevurderingResultat.Innvilgelse) {
        "Kan kun opprette utbetaling for innvilget revurdering"
    }

    val behandling = this.behandling as Revurdering

    requireNotNull(behandling.beregning) {
        "Rammevedtak $id med behandling ${behandling.id} mangler utbetalingsberegning, kan ikke opprette utbetalingsvedtak"
    }

    requireNotNull(behandling.navkontor) {
        "Rammevedtak $id med behandling ${behandling.id} mangler brukers Nav-kontor, kan ikke opprette utbetalingsvedtak"
    }

    return Utbetalingsvedtak(
        id = VedtakId.random(),
        opprettet = nå(clock),
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        forrigeUtbetalingsvedtakId = forrigeUtbetalingsvedtak?.id,
        sendtTilUtbetaling = null,
        journalpostId = null,
        journalføringstidspunkt = null,
        status = null,
        beregning = behandling.beregning,
        saksbehandler = this.saksbehandlerNavIdent,
        beslutter = this.beslutterNavIdent,
        brukerNavkontor = behandling.navkontor,
        rammevedtak = listOf(this.id),
        automatiskBehandlet = false,
        erKorrigering = false,
        begrunnelse = behandling.begrunnelseVilkårsvurdering?.verdi,
    )
}

fun Utbetalingsvedtak.tilStatistikk(): StatistikkUtbetalingDTO =
    StatistikkUtbetalingDTO(
        // TODO post-mvp jah: Vi sender uuid-delen av denne til helved som behandlingId som mappes videre til OS/UR i feltet 'henvisning'.
        id = this.id.toString(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        ordinærBeløp = this.ordinærBeløp,
        barnetilleggBeløp = this.barnetilleggBeløp,
        totalBeløp = this.totalBeløp,
        // TODO post-mvp jah: Vi oppretter vedtaket før og statistikken før vi sender til helved/utbetaling. Bør vi opprette statistikken etter vi har sendt til helved/utbetaling?
        posteringDato = this.opprettet.toLocalDate(),
        gyldigFraDatoPostering = this.periode.fraOgMed,
        gyldigTilDatoPostering = this.periode.tilOgMed,
        utbetalingId = this.id.uuidPart(),
        vedtakId = rammevedtak?.map { it.toString() },
        opprettet = LocalDateTime.now(),
        sistEndret = LocalDateTime.now(),
        brukerId = this.fnr.verdi,
    )
