package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.overlapper
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltagelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.felles.max
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataClient
import java.time.Clock
import java.time.LocalDate

class HentSaksopplysingerService(
    private val personService: PersonService,
    private val tiltaksdeltagelseKlient: TiltaksdeltagelseKlient,
    private val sokosUtbetaldataClient: SokosUtbetaldataClient,
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
        val personopplysninger = personService.hentPersonopplysninger(fnr)
        val tiltaksdeltagelserSomKanGiRettTilTiltakspenger = tiltaksdeltagelseKlient.hentTiltaksdeltagelser(
            fnr = fnr,
            correlationId = correlationId,
        )
        // Henter oppdaterte tiltaksdeltagelser det er søkt på, ved forlengelelse kan flere overlappe enn på søknadstidspunktet.
        val tiltaksdeltagelserDetErSøktPå: Tiltaksdeltagelser =
            tiltaksdeltagelserSomKanGiRettTilTiltakspenger.filtrerPåTiltaksdeltagelsesIDer(
                tiltaksdeltagelserDetErSøktTiltakspengerFor.ider,
            )
        val aktuelleTiltaksdeltagelser = if (inkluderOverlappendeTiltaksdeltagelserDetErSøktOm) {
            val aktuelle = tiltaksdeltagelserDetErSøktPå
                .filtrerPåTiltaksdeltagelsesIDer(aktuelleTiltaksdeltagelserForBehandlingen)
                .filter { it.periode != null }
            tiltaksdeltagelserDetErSøktPå
                .filter { it.periode != null }
                .filter {
                    it.eksternDeltagelseId in aktuelleTiltaksdeltagelserForBehandlingen ||
                        aktuelle.perioder.overlapper(
                            it.periode!!,
                        )
                }
        } else {
            tiltaksdeltagelserDetErSøktPå
                .filtrerPåTiltaksdeltagelsesIDer(aktuelleTiltaksdeltagelserForBehandlingen)
                .filter { it.periode != null }
        }
        // TODO jah: På sikt bør vi hente per periode i listen og ikke den totale perioden. Dette er mest aktuelt ved revurdering av to eller flere tiltaksdeltagelser som ikke overlapper.
        val saksopplysningsperiode = aktuelleTiltaksdeltagelser.totalPeriode
        // Gjør et unntak for sokosperioden, siden måneden ikke nødvendigvis er utbetalt enda, henter vi fra 1. forrige måned. Vi henter også hele inneværende måned (men ikke lenger enn dagens dato, siden det er en begrensing de har). Klienten fikser det sistnevnte.
        val utbetaldataPeriode = saksopplysningsperiode?.copy(
            fraOgMed = saksopplysningsperiode.fraOgMed.minusMonths(1).withDayOfMonth(1),
            tilOgMed = max(
                LocalDate.now(clock),
                saksopplysningsperiode.tilOgMed.withDayOfMonth(saksopplysningsperiode.tilOgMed.lengthOfMonth()),
            ),
        )
        val ytelser = if (utbetaldataPeriode != null) {
            Ytelser.fromList(
                sokosUtbetaldataClient.hentYtelserFraUtbetaldata(
                    fnr,
                    utbetaldataPeriode,
                    correlationId,
                ),
                utbetaldataPeriode,
            )
        } else {
            Ytelser.IkkeBehandlingsgrunnlag
        }
        return Saksopplysninger(
            fødselsdato = personopplysninger.fødselsdato,
            // Vi tar foreløpig med de periodene
            tiltaksdeltagelse = tiltaksdeltagelserDetErSøktPå.filtrerPåTiltaksdeltagelsesIDer(
                aktuelleTiltaksdeltagelserForBehandlingen,
            ),
            periode = saksopplysningsperiode,
            ytelser = ytelser,
        )
    }
}
