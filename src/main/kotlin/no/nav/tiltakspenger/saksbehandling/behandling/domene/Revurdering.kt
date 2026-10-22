package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.RammebehandlingBeslutterEndringer
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock
import java.time.LocalDateTime

data class Revurdering(
    override val id: RammebehandlingId,
    override val status: Rammebehandlingsstatus,
    override val opprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val iverksattTidspunkt: LocalDateTime?,
    override val sendtTilDatadeling: LocalDateTime?,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val saksopplysninger: Saksopplysninger,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val sendtTilBeslutning: LocalDateTime?,
    override val attesteringer: Attesteringer,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val avbrutt: Avbrutt?,
    override val ventestatus: Ventestatus,
    override val venterTil: LocalDateTime?,
    override val resultat: Revurderingsresultat,
    override val begrunnelseVilkårsvurdering: Begrunnelse?,
    override val utbetaling: BehandlingUtbetaling?,
    override val utbetalingskontroll: Utbetalingskontroll?,
    override val klagebehandling: Klagebehandling?,
    override val skalSendeVedtaksbrev: Boolean,
    override val prioritering: Prioritering?,
    override val skalJournalføreNotat: SkalJournalføreNotat,
    override val beslutterEndringer: RammebehandlingBeslutterEndringer,
    val automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn? = null,
    val manueltBehandlesGrunner: List<ManueltBehandlesGrunn> = emptyList(),
) : Rammebehandling {

    override val vedtaksperiode: Periode? = resultat.vedtaksperiode
    override val innvilgelsesperioder: Innvilgelsesperioder? = resultat.innvilgelsesperioder

    override val barnetillegg = resultat.barnetillegg

    override val antallDagerPerMeldeperiode = resultat.antallDagerPerMeldeperiode

    override val valgteTiltaksdeltakelser = resultat.valgteTiltaksdeltakelser

    override val omgjørRammevedtak: OmgjørRammevedtak = resultat.omgjørRammevedtak

    init {
        super.init()

        when (status) {
            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            VEDTATT,
            -> require(erFerdigutfylt()) {
                "For tilstandene $KLAR_TIL_BESLUTNING, $UNDER_BESLUTNING og $VEDTATT må resultatet være ferdigutfylt."
            }

            UNDER_AUTOMATISK_BEHANDLING,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            AVBRUTT,
            -> Unit
        }
    }

    /**
     * Sier noe om tilstanden til behandlingen.
     * Er den klar til å sendes til beslutter og/eller iverksettes?
     * Dette er uavhengig av [status], som sier noe om hvor i prosessen behandlingen er.
     */
    override fun erFerdigutfylt(): Boolean {
        return when {
            !resultat.erFerdigutfylt(saksopplysninger) -> false
            saksbehandler == null -> false
            else -> true
        }
    }

    override fun oppdaterUtbetaling(oppdatertUtbetaling: BehandlingUtbetaling?, clock: Clock): Rammebehandling {
        require(this.erUnderBehandling) {
            "Forventet at behandlingen var under behandling ved oppdatering av utbetaling, men var: ${this.status} for sakId: $sakId og behandlingId: $id"
        }
        return this.copy(
            utbetaling = oppdatertUtbetaling,
            sistEndret = nå(clock),
        )
    }

    override fun oppdaterUtbetalingskontroll(oppdatertKontroll: Utbetalingskontroll?, clock: Clock): Rammebehandling {
        require(this.erUnderBehandlingEllerBeslutning) {
            "Forventet at behandlingen var under behandling eller beslutning ved oppdatering av utbetalingskontroll, men var: ${this.status} for sakId: $sakId og behandlingId: $id"
        }
        return this.copy(
            utbetalingskontroll = oppdatertKontroll,
            sistEndret = nå(clock),
        )
    }

    /**
     * Overlater en automatisk opprettet revurdering til en saksbehandler, med [manueltBehandlesGrunner] som forklaring på hvorfor den ikke kunne behandles automatisk.
     */
    fun tilManuellBehandling(
        manueltBehandlesGrunner: List<ManueltBehandlesGrunn>,
        clock: Clock,
    ): Revurdering {
        check(status == UNDER_AUTOMATISK_BEHANDLING) {
            "Behandlingen må være under automatisk behandling. Behandlingsstatus: ${this.status}."
        }
        return this.copy(
            status = KLAR_TIL_BEHANDLING,
            sistEndret = nå(clock),
            saksbehandler = null,
            manueltBehandlesGrunner = manueltBehandlesGrunner,
        )
    }

    override fun oppdaterKlagebehandling(klagebehandling: Klagebehandling): Rammebehandling {
        require(this.klagebehandling!!.id == klagebehandling.id)
        return this.copy(klagebehandling = klagebehandling)
    }

    /**
     * Flytter tidspunktet en automatisk revurdering på vent skal vurderes på nytt.
     */
    fun oppdaterVenterTil(
        nyVenterTil: LocalDateTime,
        clock: Clock,
    ): Revurdering {
        require(status == UNDER_AUTOMATISK_BEHANDLING && saksbehandler == AUTOMATISK_SAKSBEHANDLER_ID) {
            "Kun behandlinger under automatisk behandling kan oppdatere venterTil-tidspunkt"
        }
        require(ventestatus.erSattPåVent) {
            "Kan ikke oppdatere venterTil hvis behandlingen ikke allerede er satt på vent"
        }
        return this.copy(
            venterTil = nyVenterTil,
            sistEndret = nå(clock),
        )
    }

    companion object {
        fun opprettStans(
            sakId: SakId,
            revurderingId: RammebehandlingId = RammebehandlingId.random(),
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksbehandler: Saksbehandler?,
            saksopplysninger: Saksopplysninger,
            opprettet: LocalDateTime,
            automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn? = null,
        ): Revurdering {
            return opprett(
                revurderingId = revurderingId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = saksopplysninger,
                opprettet = opprettet,
                resultat = Stans.empty,
                klagebehandling = null,
                automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            )
        }

        /**
         * En stans som opprettes av systemet og behandles av den automatiske saksbehandleren.
         * Behandlingen er [UNDER_AUTOMATISK_BEHANDLING] og tildelt [AUTOMATISK_SAKSBEHANDLER_ID] til den enten sendes til beslutning eller overlates til en saksbehandler.
         */
        fun opprettAutomatiskStans(
            sakId: SakId,
            revurderingId: RammebehandlingId = RammebehandlingId.random(),
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksopplysninger: Saksopplysninger,
            opprettet: LocalDateTime,
            automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
        ): Revurdering {
            return opprett(
                revurderingId = revurderingId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                status = UNDER_AUTOMATISK_BEHANDLING,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER_ID,
                saksopplysninger = saksopplysninger,
                opprettet = opprettet,
                resultat = Stans.empty,
                klagebehandling = null,
                automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            )
        }

        /**
         * En forlengelse (revurdering til innvilgelse) som opprettes av systemet og behandles av den automatiske saksbehandleren.
         * Behandlingen er [UNDER_AUTOMATISK_BEHANDLING] og tildelt [AUTOMATISK_SAKSBEHANDLER_ID] til den enten sendes til beslutning eller overlates til en saksbehandler.
         */
        fun opprettAutomatiskForlengelse(
            sakId: SakId,
            revurderingId: RammebehandlingId = RammebehandlingId.random(),
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksopplysninger: Saksopplysninger,
            opprettet: LocalDateTime,
            automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
        ): Revurdering {
            return opprett(
                revurderingId = revurderingId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                status = UNDER_AUTOMATISK_BEHANDLING,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER_ID,
                saksopplysninger = saksopplysninger,
                opprettet = opprettet,
                resultat = Innvilgelse.empty,
                klagebehandling = null,
                automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            )
        }

        /**
         * En omgjøring som opprettes av systemet klar til behandling for en saksbehandler.
         * Den behandles ikke automatisk, men systemet kan fylle den ut før den lagres, se [no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.fyllUtAutomatiskOpprettetOpphør].
         *
         * @param omgjørRammevedtak Rammevedtaket som erstattes helt eller delvis
         */
        fun opprettAutomatiskOmgjøring(
            revurderingId: RammebehandlingId = RammebehandlingId.random(),
            saksopplysninger: Saksopplysninger,
            omgjørRammevedtak: Rammevedtak,
            opprettet: LocalDateTime,
            automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
        ): Revurdering {
            return opprett(
                revurderingId = revurderingId,
                sakId = omgjørRammevedtak.sakId,
                saksnummer = omgjørRammevedtak.saksnummer,
                fnr = omgjørRammevedtak.fnr,
                status = KLAR_TIL_BEHANDLING,
                saksbehandler = null,
                saksopplysninger = saksopplysninger,
                opprettet = opprettet,
                resultat = Omgjøringsresultat.OmgjøringIkkeValgt(
                    omgjørRammevedtak = OmgjørRammevedtak.create(omgjørRammevedtak),
                ),
                klagebehandling = null,
                automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            )
        }

        /**
         * En omgjøring som opprettes av systemet og behandles av den automatiske saksbehandleren.
         * Behandlingen er [UNDER_AUTOMATISK_BEHANDLING] og tildelt [AUTOMATISK_SAKSBEHANDLER_ID] til den enten sendes til beslutning eller overlates til en saksbehandler.
         *
         * @param omgjørRammevedtak Rammevedtaket som erstattes helt eller delvis
         */
        fun opprettAutomatiskBehandletOmgjøring(
            revurderingId: RammebehandlingId = RammebehandlingId.random(),
            saksopplysninger: Saksopplysninger,
            omgjørRammevedtak: Rammevedtak,
            opprettet: LocalDateTime,
            automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
        ): Revurdering {
            return opprett(
                revurderingId = revurderingId,
                sakId = omgjørRammevedtak.sakId,
                saksnummer = omgjørRammevedtak.saksnummer,
                fnr = omgjørRammevedtak.fnr,
                status = UNDER_AUTOMATISK_BEHANDLING,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER_ID,
                saksopplysninger = saksopplysninger,
                opprettet = opprettet,
                resultat = Omgjøringsresultat.OmgjøringIkkeValgt(
                    omgjørRammevedtak = OmgjørRammevedtak.create(omgjørRammevedtak),
                ),
                klagebehandling = null,
                automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            )
        }

        fun opprettInnvilgelse(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksbehandler: Saksbehandler?,
            saksopplysninger: Saksopplysninger,
            opprettet: LocalDateTime,
            klagebehandling: Klagebehandling?,
            revurderingId: RammebehandlingId = RammebehandlingId.random(),
            automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn? = null,
        ): Revurdering {
            return opprett(
                revurderingId = revurderingId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = saksopplysninger,
                opprettet = opprettet,
                resultat = Innvilgelse.empty,
                klagebehandling = klagebehandling,
                automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            )
        }

        /**
         * @param omgjørRammevedtak Forhåndsutfylte vedtak som omgjøres.
         * Kan være tom; det er vedtaksperioden saksbehandler velger som avgjør hvilke vedtak som omgjøres.
         */
        fun opprettOmgjøring(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksbehandler: Saksbehandler?,
            saksopplysninger: Saksopplysninger,
            omgjørRammevedtak: OmgjørRammevedtak,
            klagebehandling: Klagebehandling?,
            opprettet: LocalDateTime,
            revurderingId: RammebehandlingId = RammebehandlingId.random(),
            automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn? = null,
        ): Revurdering {
            return opprett(
                revurderingId = revurderingId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = saksopplysninger,
                opprettet = opprettet,
                resultat = Omgjøringsresultat.OmgjøringIkkeValgt(
                    omgjørRammevedtak = omgjørRammevedtak,
                ),
                klagebehandling = klagebehandling,
                automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            )
        }

        private fun opprett(
            sakId: SakId,
            revurderingId: RammebehandlingId = RammebehandlingId.random(),
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksbehandler: Saksbehandler?,
            saksopplysninger: Saksopplysninger,
            opprettet: LocalDateTime,
            resultat: Revurderingsresultat,
            klagebehandling: Klagebehandling?,
            automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn? = null,
        ): Revurdering = opprett(
            sakId = sakId,
            revurderingId = revurderingId,
            saksnummer = saksnummer,
            fnr = fnr,
            status = if (saksbehandler != null) UNDER_BEHANDLING else KLAR_TIL_BEHANDLING,
            saksbehandler = saksbehandler?.navIdent,
            saksopplysninger = saksopplysninger,
            opprettet = opprettet,
            resultat = resultat,
            klagebehandling = klagebehandling,
            automatiskOpprettetGrunn = automatiskOpprettetGrunn,
        )

        private fun opprett(
            sakId: SakId,
            revurderingId: RammebehandlingId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            status: Rammebehandlingsstatus,
            saksbehandler: String?,
            saksopplysninger: Saksopplysninger,
            opprettet: LocalDateTime,
            resultat: Revurderingsresultat,
            klagebehandling: Klagebehandling?,
            automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn? = null,
        ): Revurdering {
            return Revurdering(
                id = revurderingId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                status = status,
                saksbehandler = saksbehandler,
                saksopplysninger = saksopplysninger,
                opprettet = opprettet,
                sistEndret = opprettet,
                resultat = resultat,
                attesteringer = Attesteringer.empty(),
                sendtTilBeslutning = null,
                beslutter = null,
                fritekstTilVedtaksbrev = null,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                avbrutt = null,
                ventestatus = Ventestatus(),
                venterTil = null,
                begrunnelseVilkårsvurdering = null,
                utbetaling = null,
                utbetalingskontroll = null,
                klagebehandling = klagebehandling,
                skalSendeVedtaksbrev = true,
                prioritering = null,
                skalJournalføreNotat = SkalJournalføreNotat.INGEN,
                beslutterEndringer = RammebehandlingBeslutterEndringer(
                    skalJournalføreNotat = null,
                ),
                automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            )
        }
    }
}
