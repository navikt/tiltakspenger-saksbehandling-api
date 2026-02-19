package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtak
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class Klagevedtak(
    override val id: VedtakId,
    override val opprettet: LocalDateTime,
    val behandling: Klagebehandling,
    override val journalpostId: JournalpostId?,
    override val journalføringstidspunkt: LocalDateTime?,
    val distribusjonId: DistribusjonId?,
    val distribusjonstidspunkt: LocalDateTime?,
    val vedtaksdato: LocalDate?,
    val sendtTilDatadeling: LocalDateTime?,
) : Vedtak {

    override val beregning: Beregning? = null
    override val utbetaling: VedtattUtbetaling? = null
    override val beslutter: String? = null

    override val sakId: SakId = behandling.sakId
    override val saksnummer: Saksnummer = behandling.saksnummer
    override val fnr: Fnr = behandling.fnr
    override val saksbehandler: String = behandling.saksbehandler!!

    val resultat: Klagebehandlingsresultat = behandling.resultat!!

    companion object {
        fun createFromKlagebehandling(
            id: VedtakId = VedtakId.random(),
            clock: Clock,
            klagebehandling: Klagebehandling,
        ): Klagevedtak {
            return Klagevedtak(
                id = id,
                opprettet = nå(clock),
                behandling = klagebehandling,
                journalpostId = null,
                journalføringstidspunkt = null,
                distribusjonId = null,
                distribusjonstidspunkt = null,
                vedtaksdato = null,
                sendtTilDatadeling = null,
            )
        }
    }

    init {
        require(behandling.id != id) {
            "Klagevedtakets id kan ikke være lik klagebehandlingens id. sakId=$sakId, saksnummer=$saksnummer, vedtakId=$id, behandlingId=${behandling.id}."
        }
        require(opprettet >= behandling.opprettet) {
            "Klagevedtakets opprettet-tidspunkt må være etter eller lik klagebehandlingens opprettet-tidspunkt. sakId=$sakId, saksnummer=$saksnummer, vedtakId=$id, behandlingId=${behandling.id}."
        }
        require(behandling.resultat is Klagebehandlingsresultat.Avvist) {
            "Klagevedtak kan kun opprettes for klagebehandlinger med resultat Avvist, men var ${behandling.resultat}. Ved medhold/omgjøring eies klagebehandlingen av rammevedtaket. sakId=$sakId, saksnummer=$saksnummer, vedtakId=$id, behandlingId=${behandling.id}, resultat=${behandling.resultat}."
        }
    }
}
