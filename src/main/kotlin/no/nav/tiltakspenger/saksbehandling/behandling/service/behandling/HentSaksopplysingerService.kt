package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaClient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltakspengevedtakFraArena
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.felles.min
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelserFraRegister
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataClient
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class HentSaksopplysingerService(
    private val hentPersonopplysninger: suspend (fnr: Fnr) -> EnkelPerson,
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
    private val sokosUtbetaldataClient: SokosUtbetaldataClient,
    private val tiltakspengerArenaClient: TiltakspengerArenaClient,
    private val clock: Clock,
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
) {
    /**
     * Tiltakspenger er alltid begrenset av tiltaksdeltakelsen(e) det er søkt på.
     * Derfor begrenser tiltaksdeltakelsesperioden(e) behandlingsgrunnlaget vårt.
     * Vi trenger kun vilkårsvurdere andre vilkår innenfor tiltaksdeltakelsen(e) sin(e) periode(r).
     * Vi gjør et unntak for Sokos' utbetaldata, siden denne måneden ikke nødvendigvis er utbetalt enda. Derfor henter vi for en noe utvidet periode her.
     *
     * @param tiltaksdeltakelserDetErSøktTiltakspengerFor Alle søknader. Merk at flere søknader kan gjelde samme tiltaksdeltakelse, mens periodene kan være endret.
     * @param aktuelleTiltaksdeltakelserForBehandlingen For søknadsbehandling vil det være den tiltaksdeltakelsen det er søkt for. Husk og sett [inkluderOverlappendeTiltaksdeltakelserDetErSøktOm] til true dersom vi skal hente saksopplysninger for tiltaksdeltakelser som overlapper denne.
     * @param inkluderOverlappendeTiltaksdeltakelserDetErSøktOm Typisk bare brukt ved søknadsbehandling når det er søkt om mer enn 1 tiltaksdeltakelse.
     */
    suspend fun hentSaksopplysningerFraRegistre(
        fnr: Fnr,
        correlationId: CorrelationId,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        aktuelleTiltaksdeltakelserForBehandlingen: List<TiltaksdeltakerId>,
        inkluderOverlappendeTiltaksdeltakelserDetErSøktOm: Boolean,
        sessionContext: SessionContext? = null,
    ): Saksopplysninger {
        val oppslagstidspunkt = LocalDateTime.now(clock)

        val aktuelleTiltaksdeltakelser = hentAktuelleTiltaksdeltakelser(
            fnr = fnr,
            correlationId = correlationId,
            tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
            aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
            inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = inkluderOverlappendeTiltaksdeltakelserDetErSøktOm,
            sessionContext = sessionContext,
        )
        // TODO jah: På sikt bør vi hente per periode i listen og ikke den totale perioden. Dette er mest aktuelt ved revurdering av to eller flere tiltaksdeltakelser som ikke overlapper.
        //  TODO 2 jah + Bente: Vi bør nok også krympe perioden basert på kravtidspunktet, slik at vi ikke henter for langt tilbake i tid.
        val saksopplysningsperiode = aktuelleTiltaksdeltakelser.totalPeriode

        return Saksopplysninger(
            fødselsdato = hentPersonopplysninger(fnr).fødselsdato,
            // Vi tar foreløpig med de periodene
            tiltaksdeltakelser = aktuelleTiltaksdeltakelser,
            ytelser = saksopplysningsperiode
                ?.let { hentYtelser(it, fnr, correlationId) }
                ?: Ytelser.IkkeBehandlingsgrunnlag,
            tiltakspengevedtakFraArena = saksopplysningsperiode
                ?.let { hentTiltakspengevedtakFraArena(it, fnr, correlationId) }
                ?: TiltakspengevedtakFraArena.IkkeBehandlingsgrunnlag,
            oppslagstidspunkt = oppslagstidspunkt,
        )
    }

    private suspend fun hentAktuelleTiltaksdeltakelser(
        fnr: Fnr,
        correlationId: CorrelationId,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        aktuelleTiltaksdeltakelserForBehandlingen: List<TiltaksdeltakerId>,
        inkluderOverlappendeTiltaksdeltakelserDetErSøktOm: Boolean,
        sessionContext: SessionContext? = null,
    ): Tiltaksdeltakelser {
        val tiltaksdeltakelserSomKanGiRettTilTiltakspenger = tiltaksdeltakelseKlient.hentTiltaksdeltakelser(
            fnr = fnr,
            tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
            correlationId = correlationId,
        )
        val oppdaterteEksterneIderDetErSoktFor = tiltaksdeltakelserDetErSøktTiltakspengerFor.ider.map {
            tiltaksdeltakerRepo.hentEksternId(id = it, sessionContext = sessionContext)
        }
        // Henter oppdaterte tiltaksdeltakelser det er søkt på, ved forlengelse kan flere overlappe enn på søknadstidspunktet.
        val tiltaksdeltakelserDetErSøktPå: TiltaksdeltakelserFraRegister =
            tiltaksdeltakelserSomKanGiRettTilTiltakspenger.filtrerPåTiltaksdeltakelsesIDer(
                oppdaterteEksterneIderDetErSoktFor,
            )
        val aktuelleEksterneTiltaksdeltakelseIderForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen.map {
            tiltaksdeltakerRepo.hentEksternId(id = it, sessionContext = sessionContext)
        }
        val aktuelleTiltaksdeltakelser = tiltaksdeltakelserDetErSøktPå
            .filtrerPåTiltaksdeltakelsesIDer(aktuelleEksterneTiltaksdeltakelseIderForBehandlingen)
            .let { aktuelle ->
                if (inkluderOverlappendeTiltaksdeltakelserDetErSøktOm) {
                    tiltaksdeltakelserDetErSøktPå.overlappende(aktuelle)
                } else {
                    aktuelle
                }
            }
        val tiltaksdeltakelser = aktuelleTiltaksdeltakelser.value.map {
            val internDeltakelseId = tiltaksdeltakerRepo.hentEllerLagre(
                eksternId = it.eksternDeltakelseId,
                tiltakstype = it.typeKode.tilTiltakstype(),
                sessionContext = sessionContext,
            )
            it.toTiltaksdeltakelse(internDeltakelseId)
        }
        return Tiltaksdeltakelser(tiltaksdeltakelser)
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
