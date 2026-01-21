package no.nav.tiltakspenger.saksbehandling.vedtak

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodiserbar
import no.nav.tiltakspenger.libs.periodisering.leggSammen
import no.nav.tiltakspenger.libs.periodisering.trekkFra
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjortAvRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperioder
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.utsjekk.kontrakter.felles.Satstype
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param opprettet Tidspunktet vi instansierte og persisterte dette vedtaket første gangen. Dette har ingenting med vedtaksbrevet å gjøre.
 * @param periode Stansperiode eller innvilgelsesperiode (ikke nødvendigvis det samme som vedtaksperiode).
 * @param vedtaksdato Datoen vi bruker i brevet. Lagres samtidig som vi genererer og journalfører brevet. Vil være null fram til dette.
 * @param omgjortAvRammevedtak Dersom dette vedtaket er erstattet helt eller delvis av ett eller flere senere vedtak.
 * @param omgjørRammevedtak Dersom dette vedtaket helt eller delvis omgjør ett eller flere tidligere vedtak.
 */
data class Rammevedtak(
    override val id: VedtakId = VedtakId.random(),
    override val opprettet: LocalDateTime,
    override val sakId: SakId,
    // TODO jah: Rename til vedtaksperiode og fjern fra basen. Denne bør heller reflektere behandlingen.
    override val periode: Periode,
    override val journalpostId: JournalpostId?,
    override val journalføringstidspunkt: LocalDateTime?,
    override val utbetaling: VedtattUtbetaling?,
    val behandling: Rammebehandling,
    val vedtaksdato: LocalDate?,
    val distribusjonId: DistribusjonId?,
    val distribusjonstidspunkt: LocalDateTime?,
    val sendtTilDatadeling: LocalDateTime?,
    val brevJson: String?,
    val omgjortAvRammevedtak: OmgjortAvRammevedtak,
) : Vedtak,
    Periodiserbar {
    override val fnr: Fnr = behandling.fnr
    override val saksnummer: Saksnummer = behandling.saksnummer
    override val saksbehandler: String = behandling.saksbehandler!!
    override val beslutter: String = behandling.beslutter!!

    override val beregning: Beregning? = behandling.utbetaling?.beregning

    val resultat: BehandlingResultat = behandling.resultat!!

    val omgjørRammevedtak: OmgjørRammevedtak by lazy { behandling.resultat!!.omgjørRammevedtak }

    /** Er dette en omgjøringsbehandling? */
    val erOmgjøringsbehandling: Boolean by lazy {
        behandling.resultat is RevurderingResultat.Omgjøring
    }

    val erAvslag: Boolean by lazy {
        behandling.resultat is SøknadsbehandlingResultat.Avslag
    }

    val erStans: Boolean by lazy {
        behandling.resultat is RevurderingResultat.Stans
    }

    /** Vil være null for stans, avslag eller dersom bruker ikke har rett på barnetillegg  */
    val barnetillegg: Barnetillegg? by lazy { behandling.barnetillegg }

    /** Vil være null for stans og avslag */
    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode>? =
        behandling.antallDagerPerMeldeperiode

    val valgteTiltaksdeltakelser: IkkeTomPeriodisering<Tiltaksdeltakelse>? = behandling.valgteTiltaksdeltakelser

    val innvilgelsesperioder: Innvilgelsesperioder? = behandling.innvilgelsesperioder

    val opprinneligInnvilgetPerioder: List<Periode> by lazy {
        innvilgelsesperioder?.perioder?.leggSammen() ?: emptyList()
    }

    val gjeldendeInnvilgetPerioder: List<Periode> by lazy {
        if (erAvslag || erStans) return@lazy emptyList()
        if (omgjortAvRammevedtak.isEmpty()) {
            innvilgelsesperioder?.perioder ?: emptyList()
        } else {
            innvilgelsesperioder?.perioder?.trekkFra(omgjortAvRammevedtak.perioder) ?: emptyList()
        }.leggSammen()
    }

    /**
     * Avslag anses ikke som gjeldende.
     */
    val gjeldendePerioder: List<Periode> by lazy {
        if (erAvslag) return@lazy emptyList()
        if (omgjortAvRammevedtak.isEmpty()) {
            listOf(periode)
        } else {
            this.periode.trekkFra(omgjortAvRammevedtak.perioder)
        }
    }

    /** Merk at et vedtak fremdeles er gjeldende dersom det er delvis omgjort. Avslag anses ikke som gjeldende. */
    val erGjeldende: Boolean by lazy {
        gjeldendePerioder.isNotEmpty()
    }

    val gyldigeKommandoer: Rammevedtakskommandoer by lazy {
        Rammevedtakskommandoer(
            setOfNotNull(
                gyldigStanskommando,
                gyldigOmgjøringskommando,
                gyldigOpphørskommando,
            ),
        )
    }

    /**
     * Krever at det finnes en gjeldende innvilgelsesperiode.
     * Hvis det er flere perioder (hull), støtter vi bare og stanse den siste.
     * Merk at til og med-verdien ikke kan styres av saksbehandler.
     * Saksbehandler kan kun velge fra og med-dato, og stanses alltid ut hele den gjeldende innvilgede vedtaksperioden.
     */
    val gyldigStanskommando: Rammevedtakskommando.Stans? by lazy {
        gjeldendeInnvilgetPerioder.lastOrNull()?.let {
            Rammevedtakskommando.Stans(
                tidligsteFraOgMedDato = it.fraOgMed,
                tvungenStansTilOgMedDato = it.tilOgMed,
            )
        }
    }

    /**
     * Krever at vedtaket er gjeldende i sin helhet.
     * Kan kun omgjøres helt, ikke delvis.
     */
    val gyldigOmgjøringskommando: Rammevedtakskommando.Omgjør? by lazy {
        if (omgjortAvRammevedtak.isNotEmpty()) return@lazy null
        if (erAvslag) return@lazy null
        Rammevedtakskommando.Omgjør(tvungenOmgjøringsperiode = periode)
    }

    /**
     * Ikke implementert enda.
     * Merk at man bare skal kunne opphøre en sammenhengende periode.
     */
    val gyldigOpphørskommando: Rammevedtakskommando.Opphør? by lazy {
        if (gjeldendeInnvilgetPerioder.isEmpty()) return@lazy null
        Rammevedtakskommando.Opphør(gjeldendeInnvilgetPerioder.toNonEmptyListOrNull()!!)
    }

    /**
     * Et vedtak kan bare bli helt omgjort en gang.
     * Delvis omgjøring kan skje flere ganger, men hver periode kan kun omgjøres en gang.
     *
     * Tenkt kalt under behandlingen for å avgjøre hvilke rammevedtak som vil bli omgjort.
     * Obs: Merk at en annen behandling kan ha omgjort det samme/de samme vedtakene etter at denne metoden er kalt, men før denne behandlingen iverksettes.
     * @param vedtaksperiode til en ny behandling/vedtak som potensielt vil omgjøre dette vedtaket. Kan være en ren innvilgelse, et rent opphør eller en blanding.
     * @return En tom liste dersom dette vedtaket ikke lenger gjelder eller [vedtaksperiode] ikke overlapper, ellers en liste over perioder som omgjøres.
     */
    fun finnPerioderSomOmgjøres(vedtaksperiode: Periode): Omgjøringsperioder {
        return Omgjøringsperioder(
            vedtaksperiode.overlappendePerioder(gjeldendePerioder).map {
                Omgjøringsperiode(
                    rammevedtakId = this.id,
                    periode = it,
                    omgjøringsgrad = if (it == this.periode) Omgjøringsgrad.HELT else Omgjøringsgrad.DELVIS,
                )
            }.sortedBy { it.periode.fraOgMed },
        )
    }

    /**
     * Når et nytt vedtak iverksettes, gjør vi en sjekk på om det nye vedtaket omgjør det eksisterende vedtaket eller ikke.
     */
    fun oppdaterOmgjortAvRammevedtak(nyttRammevedtak: Rammevedtak): Rammevedtak {
        val nyeOmgjøringsperioder = Omgjøringsperioder(
            nyttRammevedtak.periode.overlappendePerioder(gjeldendePerioder).map {
                Omgjøringsperiode(
                    rammevedtakId = nyttRammevedtak.id,
                    periode = it,
                    omgjøringsgrad = if (it == this.periode) Omgjøringsgrad.HELT else Omgjøringsgrad.DELVIS,
                )
            },
        )
        if (nyeOmgjøringsperioder.isEmpty()) return this
        return this.copy(omgjortAvRammevedtak = this.omgjortAvRammevedtak.leggTil(nyeOmgjøringsperioder))
    }

    init {
        require(behandling.erVedtatt) { "Kan ikke lage vedtak for behandling som ikke er vedtatt. BehandlingId: ${behandling.id}" }
        require(sakId == behandling.sakId) { "SakId i vedtak og behandling må være lik. SakId: $sakId, BehandlingId: ${behandling.id}" }
        require(this@Rammevedtak.periode == behandling.vedtaksperiode) { "Periode i vedtak (${this@Rammevedtak.periode}) og behandlingens vedtaksperiode (${behandling.vedtaksperiode}) må være lik. SakId: $sakId, Saksnummer: ${behandling.saksnummer} BehandlingId: ${behandling.id}" }
        require(id !in omgjørRammevedtak.rammevedtakIDer)
        require(id !in omgjortAvRammevedtak.rammevedtakIDer)

        utbetaling?.also {
            require(id == it.vedtakId)
            require(sakId == it.sakId)
            require(fnr == it.fnr)
            require(opprettet == it.opprettet)
            require(saksbehandler == it.saksbehandler)
            require(beslutter == it.beslutter)
            require(behandling.id == it.beregningKilde.id)
        }
        if (erAvslag) {
            require(utbetaling == null) { "Vedtak som er avslag kan ikke ha utbetaling. VedtakId: $id" }
            require(omgjørRammevedtak.isEmpty()) {
                "Avslagsvedtak kan ikke omgjøre andre vedtak. SakId: $sakId. VedtakId: $id. omgjørRammevedtak: $omgjørRammevedtak"
            }
            require(omgjortAvRammevedtak.isEmpty()) {
                "Avslagsvedtak kan ikke bli omgjort av andre vedtak. SakId: $sakId. VedtakId: $id. omgjortAvRammevedtak: $omgjortAvRammevedtak"
            }
        }
    }
}

fun Sak.opprettVedtak(
    behandling: Rammebehandling,
    clock: Clock,
): Pair<Sak, Rammevedtak> {
    require(behandling.status == Rammebehandlingsstatus.VEDTATT) { "Krever behandlingsstatus VEDTATT når vi skal opprette et vedtak." }

    val vedtakId = VedtakId.random()
    val opprettet = nå(clock)

    val utbetaling: VedtattUtbetaling? = behandling.utbetaling?.let {
        VedtattUtbetaling(
            id = UtbetalingId.random(),
            vedtakId = vedtakId,
            sakId = this.id,
            saksnummer = this.saksnummer,
            fnr = this.fnr,
            brukerNavkontor = it.navkontor,
            opprettet = opprettet,
            saksbehandler = behandling.saksbehandler!!,
            beslutter = behandling.beslutter!!,
            beregning = it.beregning,
            forrigeUtbetalingId = this.utbetalinger.lastOrNull()?.id,
            statusMetadata = Forsøkshistorikk.opprett(clock = clock),
            satstype = Satstype.DAGLIG,
            sendtTilUtbetaling = null,
            status = null,
        )
    }

    val vedtak = Rammevedtak(
        id = vedtakId,
        opprettet = opprettet,
        sakId = this.id,
        behandling = behandling,
        periode = behandling.vedtaksperiode!!,
        omgjortAvRammevedtak = OmgjortAvRammevedtak.empty,
        utbetaling = utbetaling,
        vedtaksdato = null,
        journalpostId = null,
        journalføringstidspunkt = null,
        distribusjonId = null,
        distribusjonstidspunkt = null,
        sendtTilDatadeling = null,
        brevJson = null,
    )

    val oppdatertSak = this.leggTilRammevedtak(vedtak)

    return oppdatertSak to vedtak
}
