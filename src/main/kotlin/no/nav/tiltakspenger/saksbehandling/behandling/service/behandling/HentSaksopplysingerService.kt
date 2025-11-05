package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaClient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltagelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltakspengevedtakFraArena
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.felles.min
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataClient
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class HentSaksopplysingerService(
    private val hentPersonopplysninger: suspend (fnr: Fnr) -> EnkelPerson,
    private val tiltaksdeltagelseKlient: TiltaksdeltagelseKlient,
    private val sokosUtbetaldataClient: SokosUtbetaldataClient,
    private val tiltakspengerArenaClient: TiltakspengerArenaClient,
    private val clock: Clock,
) {
    /**
     * Tiltakspenger er alltid begrenset av tiltaksdeltagelsen(e) det er søkt på.
     * Derfor begrenser tiltaksdeltagelsesperioden(e) behandlingsgrunnlaget vårt.
     * Vi trenger kun vilkårsvurdere andre vilkår innenfor tiltaksdeltagelsen(e) sin(e) periode(r).
     * Vi gjør et unntak for Sokos' utbetaldata, siden denne måneden ikke nødvendigvis er utbetalt enda. Derfor henter vi for en noe utvidet periode her.
     *
     * @param tiltaksdeltagelserDetErSøktTiltakspengerFor Alle søknader. Merk at flere søknader kan gjelde samme tiltaksdeltagelse, mens periodene kan være endret.
     * @param aktuelleTiltaksdeltagelserForBehandlingen For søknadsbehandling vil det være den tiltaksdeltagelsen det er søkt for. Husk og sett [inkluderOverlappendeTiltaksdeltagelserDetErSøktOm] til true dersom vi skal hente saksopplysninger for tiltaksdeltagelser som overlapper denne.
     * @param inkluderOverlappendeTiltaksdeltagelserDetErSøktOm Typisk bare brukt ved søknadsbehandling når det er søkt om mer enn 1 tiltaksdeltagelse.
     */
    suspend fun hentSaksopplysningerFraRegistre(
        fnr: Fnr,
        correlationId: CorrelationId,
        tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
        aktuelleTiltaksdeltagelserForBehandlingen: List<String>,
        inkluderOverlappendeTiltaksdeltagelserDetErSøktOm: Boolean,
    ): Saksopplysninger {
        val oppslagstidspunkt = LocalDateTime.now(clock)

        val aktuelleTiltaksdeltagelser = hentAktuelleTiltaksdeltagelser(
            fnr = fnr,
            correlationId = correlationId,
            tiltaksdeltagelserDetErSøktTiltakspengerFor = tiltaksdeltagelserDetErSøktTiltakspengerFor,
            aktuelleTiltaksdeltagelserForBehandlingen = aktuelleTiltaksdeltagelserForBehandlingen,
            inkluderOverlappendeTiltaksdeltagelserDetErSøktOm = inkluderOverlappendeTiltaksdeltagelserDetErSøktOm,
        )
        // TODO jah: På sikt bør vi hente per periode i listen og ikke den totale perioden. Dette er mest aktuelt ved revurdering av to eller flere tiltaksdeltagelser som ikke overlapper.
        //  TODO 2 jah + Bente: Vi bør nok også krympe perioden basert på kravtidspunktet, slik at vi ikke henter for langt tilbake i tid.
        val saksopplysningsperiode = aktuelleTiltaksdeltagelser.totalPeriode

        return Saksopplysninger(
            fødselsdato = hentPersonopplysninger(fnr).fødselsdato,
            // Vi tar foreløpig med de periodene
            tiltaksdeltagelser = aktuelleTiltaksdeltagelser,
            ytelser = saksopplysningsperiode
                ?.let { hentYtelser(it, fnr, correlationId) }
                ?: Ytelser.IkkeBehandlingsgrunnlag,
            tiltakspengevedtakFraArena = saksopplysningsperiode
                ?.let { hentTiltakspengevedtakFraArena(it, fnr, correlationId) }
                ?: TiltakspengevedtakFraArena.IkkeBehandlingsgrunnlag,
            oppslagstidspunkt = oppslagstidspunkt,
        )
    }

    private suspend fun hentAktuelleTiltaksdeltagelser(
        fnr: Fnr,
        correlationId: CorrelationId,
        tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
        aktuelleTiltaksdeltagelserForBehandlingen: List<String>,
        inkluderOverlappendeTiltaksdeltagelserDetErSøktOm: Boolean,
    ): Tiltaksdeltagelser {
        val tiltaksdeltagelserSomKanGiRettTilTiltakspenger = tiltaksdeltagelseKlient.hentTiltaksdeltagelser(
            fnr = fnr,
            tiltaksdeltagelserDetErSøktTiltakspengerFor = tiltaksdeltagelserDetErSøktTiltakspengerFor,
            correlationId = correlationId,
        )
        // Henter oppdaterte tiltaksdeltagelser det er søkt på, ved forlengelse kan flere overlappe enn på søknadstidspunktet.
        val tiltaksdeltagelserDetErSøktPå: Tiltaksdeltagelser =
            tiltaksdeltagelserSomKanGiRettTilTiltakspenger.filtrerPåTiltaksdeltagelsesIDer(
                tiltaksdeltagelserDetErSøktTiltakspengerFor.ider,
            )
        val aktuelleTiltaksdeltagelser = tiltaksdeltagelserDetErSøktPå
            .filtrerPåTiltaksdeltagelsesIDer(aktuelleTiltaksdeltagelserForBehandlingen)
            .let { aktuelle ->
                if (inkluderOverlappendeTiltaksdeltagelserDetErSøktOm) {
                    tiltaksdeltagelserDetErSøktPå.overlappende(aktuelle)
                } else {
                    aktuelle
                }
            }
        return aktuelleTiltaksdeltagelser
    }

    private suspend fun hentYtelser(
        saksopplysningsperiode: Periode,
        fnr: Fnr,
        correlationId: CorrelationId,
    ): Ytelser {
        // Gjør et unntak for sokosperioden, siden måneden ikke nødvendigvis er utbetalt enda, henter vi fra 1. forrige måned. Vi henter også hele inneværende måned (men ikke lenger enn dagens dato, siden det er en begrensing de har). Klienten fikser det sistnevnte.
        val utbetaldataFraOgMed = saksopplysningsperiode.fraOgMed.minusMonths(1).withDayOfMonth(1)
        // Utbetaldata støtter senest dagens dato.
        val utbetaldataTilOgMed = saksopplysningsperiode.tilOgMed.let {
            min(LocalDate.now(clock), it.withDayOfMonth(it.lengthOfMonth()))
        }
        val utbetaldataPeriode =
            if (utbetaldataTilOgMed < utbetaldataFraOgMed) {
                return Ytelser.IkkeBehandlingsgrunnlag
            } else {
                Periode(utbetaldataFraOgMed, utbetaldataTilOgMed)
            }
        return Ytelser.fromList(
            ytelser = sokosUtbetaldataClient.hentYtelserFraUtbetaldata(
                fnr,
                utbetaldataPeriode,
                correlationId,
            ),
            oppslagsperiode = utbetaldataPeriode,
            oppslagstidspunkt = LocalDateTime.now(clock),
        )
    }

    private suspend fun hentTiltakspengevedtakFraArena(
        saksopplysningsperiode: Periode,
        fnr: Fnr,
        correlationId: CorrelationId,
    ): TiltakspengevedtakFraArena =
        TiltakspengevedtakFraArena.fromList(
            arenaTpVedtak = tiltakspengerArenaClient.hentTiltakspengevedtakFraArena(
                fnr,
                saksopplysningsperiode,
                correlationId,
            ),
            oppslagsperiode = saksopplysningsperiode,
            oppslagstidspunkt = LocalDateTime.now(clock),
        )
}
