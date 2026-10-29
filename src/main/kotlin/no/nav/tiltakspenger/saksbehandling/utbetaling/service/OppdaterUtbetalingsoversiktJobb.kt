package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KLogger
import io.micrometer.core.instrument.MeterRegistry
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.felles.loggFeil
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Avgrensningsårsak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.KunneIkkeHenteUtbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsfeiltype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsplan
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktgrunnlag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.tilUtbetalingsoversiktMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider.erInnenforØkonomisystemetsÅpningstider
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * [logger] og [sikkerlogg] har ingen default-verdi, slik at tester kan sjekke at hvert oppslag gir én logglinje.
 * [no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup.UtbetalingContext] sender inn loggeren med klassens navn, slik at navnet i Loki er uendret.
 */
class OppdaterUtbetalingsoversiktJobb(
    private val sakRepo: SakRepo,
    private val utbetalingsoversiktRepo: UtbetalingsoversiktRepo,
    private val utbetalingsoversiktklient: Utbetalingsoversiktklient,
    private val clock: Clock,
    private val meterRegistry: MeterRegistry,
    private val limit: Int,
    private val logger: KLogger,
    private val sikkerlogg: Sikkerlogg,
) {
    /**
     * Oppdaterer sakene som er klare, og gjør ingenting når økonomisystemet er stengt.
     * En kjøring som starter rett før stengetid kan gå noen minutter over; stengetiden 20:50 har margin til 21:00 for det.
     * Stopper ved første feil som rammer alle saker, ved en uventet feil, og når [MAKS_FEIL_PER_KJØRING] oppslag har feilet.
     * Eierteamet varsles ved mer enn to feil på tre minutter, og tjenesten sier ikke fra om at økonomisystemet er stengt.
     * En feil som rammer alle saker eller en uventet feil gir en [Systempause] der kjøringene står over; det første oppslaget etter pausen er et prøveoppslag.
     * Svarer tjenesten på et oppslag, også med et tomt svar eller en feil som bare gjelder saken, er pausen over.
     * Jobben logger når pausen starter og slutter, og måler antall pauser på rad i `tpts_saksbehandlingapi_utbetalingsoversikt_systempauser_paa_rad`.
     * Bare jobben på lederpoden setter verdien, så andre poder viser 0.
     */
    suspend fun oppdaterUtbetalingsoversikter() {
        if (!erInnenforØkonomisystemetsÅpningstider(clock)) return
        if (systempause?.let { nå(clock) < it.nesteForsøk } == true) return
        var antallFeil = 0
        for (sakId in utbetalingsoversiktRepo.hentSakerKlareForOppslag(nå = nå(clock), limit = limit)) {
            val skalStoppe = Either.catch { oppdaterForSak(sakId) }.fold(
                ifLeft = {
                    logger.error(it) { "Uventet feil ved oppdatering av utbetalingsoversikt, stopper kjøringen. sakId=$sakId." }
                    pauseEtterTekniskFeil()
                    true
                },
                ifRight = { utfall ->
                    utfall.fold(
                        ifLeft = { feil ->
                            antallFeil++
                            if (feil.rammerAlleSaker) pauseEtterTekniskFeil() else avsluttSystempause()
                            feil.rammerAlleSaker || antallFeil >= MAKS_FEIL_PER_KJØRING
                        },
                        ifRight = {
                            avsluttSystempause()
                            false
                        },
                    )
                },
            )
            if (skalStoppe) return
        }
    }

    // pausen ligger i minnet på lederpoden, så en omstart eller et nytt ledervalg nullstiller den og gir ett prøveoppslag for tidlig; flytt den til databasen hvis det blir et problem.
    private var systempause: Systempause? = null

    private val antallPauserPåRadMåler: AtomicLong =
        meterRegistry.gauge("tpts_saksbehandlingapi_utbetalingsoversikt_systempauser_paa_rad", AtomicLong(0))!!

    private fun pauseEtterTekniskFeil() {
        val antallPauserPåRad = (systempause?.antallPauserPåRad ?: 0) + 1
        val (_, nesteForsøk) = nå(clock).shouldRetry(count = antallPauserPåRad, clock = clock, delayTable = PAUSELENGDER, maxDelay = 60.minutes)
        systempause = Systempause(antallPauserPåRad = antallPauserPåRad, nesteForsøk = nesteForsøk)
        antallPauserPåRadMåler.set(antallPauserPåRad)
        logger.warn { "Pauser oppdatering av utbetalingsoversikter etter teknisk feil. antallPauserPåRad=$antallPauserPåRad, nesteForsøk=$nesteForsøk." }
    }

    private fun avsluttSystempause() {
        val pause = systempause ?: return
        systempause = null
        antallPauserPåRadMåler.set(0)
        logger.info { "Tjenesten svarer igjen og pausen er over. antallPauserPåRad=${pause.antallPauserPåRad}." }
    }

    /** Kjøringene står over til [nesteForsøk], etter [antallPauserPåRad] kjøringer på rad som stoppet på en teknisk feil. */
    private data class Systempause(val antallPauserPåRad: Long, val nesteForsøk: LocalDateTime)

    /**
     * Slår opp utbetalingene for én sak og lagrer utfallet, også når oppslaget feiler.
     * Utfallet måles før lagringen, slik at en lagringsfeil ikke skjuler det i metrikkene.
     * Loggen skrives etter lagringen, slik at hvert oppslag gir én linje: info når det lykkes og error når det feiler.
     * Kaster lagringen, logges bare denne feilen.
     * Forutsetter at saken finnes og har minst én sendt utbetaling.
     */
    suspend fun oppdaterForSak(sakId: SakId): Either<KunneIkkeHenteUtbetalingsoversikt, Unit> {
        val sak = sakRepo.hentForSakId(sakId)!!
        val grunnlag = Utbetalingsoversiktgrunnlag.forSak(sak)
        val oppslag = Oppslag.forGrunnlag(grunnlag, iDag = LocalDate.now(clock))
        val tidligereFeilPåRad = utbetalingsoversiktRepo.hentStatusForSak(sakId).antallFeilPåRad
        val correlationId = CorrelationId.generate()
        val kontekst = "sakId=${sak.id}, saksnummer=${sak.saksnummer}, correlationId=${correlationId.value}"

        return utbetalingsoversiktklient.hent(grunnlag, oppslag.periode, oppslag.periodetype, correlationId)
            .onRight { svar ->
                val hentet = svar.metadata.hentet()
                val oversikt = Utbetalingsoversikt.Vellykket(
                    id = UtbetalingsoversiktId.random(),
                    sakId = sak.id,
                    hentet = hentet,
                    oppslag = oppslag,
                    plan = Oppslagsplan.etterVellykketOppslag(
                        hentet = hentet,
                        harNyligSendtUtbetaling = sak.utbetalinger.harOkUtbetalingSendtEtter(hentet.minusDays(30)),
                    ),
                    utbetalinger = svar.utbetalinger,
                )
                registrerMetrikker(resultat = "vellykket", metadata = svar.metadata)
                registrerFjernedeYtelser(svar.avgrensning.fjernedeYtelserPerÅrsak)
                utbetalingsoversiktRepo.lagre(oversikt, svar.tilUtbetalingsoversiktMetadata(correlationId))
                logger.info {
                    "Hentet utbetalingsoversikt. $kontekst, antallUtbetalinger=${oversikt.utbetalinger.size}, antallMottatt=${svar.avgrensning.antallUtbetalingerMottatt}, " +
                        "antallYtelserMottatt=${svar.avgrensning.antallYtelserMottatt}, fjernedeYtelserPerÅrsak=${svar.avgrensning.fjernedeYtelserPerÅrsak}, nesteOppslag=${oversikt.plan.nesteOppslag}."
                }
            }
            .onLeft { feil ->
                val hentet = feil.metadata.hentet()
                val oversikt = Utbetalingsoversikt.Feilet(
                    id = UtbetalingsoversiktId.random(),
                    sakId = sak.id,
                    hentet = hentet,
                    oppslag = oppslag,
                    plan = Oppslagsplan.etterFeiletOppslag(hentet = hentet, tidligereFeilPåRad = tidligereFeilPåRad, clock = clock),
                    feiltype = feil.feiltype,
                )
                registrerMetrikker(resultat = feil.feiltype.tilMetrikkverdi(), metadata = feil.metadata)
                utbetalingsoversiktRepo.lagre(oversikt, feil.metadata.tilUtbetalingsoversiktMetadata(correlationId))
                feil.loggFeil(logger, sikkerlogg, "$kontekst, nesteOppslag=${oversikt.plan.nesteOppslag}")
            }
            .map { }
    }

    private fun HttpKlientMetadata.hentet() = tidsstempler.responsMottatt ?: nå(clock)

    private fun registrerMetrikker(resultat: String, metadata: HttpKlientMetadata) {
        meterRegistry.counter("tpts_saksbehandlingapi_utbetalingsoversikt_oppslag", "resultat", resultat).increment()
        meterRegistry.timer("tpts_saksbehandlingapi_utbetalingsoversikt_oppslag_varighet").record(metadata.totalDuration.toJavaDuration())
    }

    /**
     * Teller fjernede ytelser per årsak i stedet for å logge dem, og infolinja viser hvilken sak de gjelder.
     * Alle årsaker telles ved hvert vellykket oppslag, også med 0, slik at seriene finnes før første avvik og `increase()` fanger opp det første hoppet.
     */
    private fun registrerFjernedeYtelser(fjernedeYtelserPerÅrsak: Map<Avgrensningsårsak, Int>) {
        Avgrensningsårsak.entries.forEach { årsak ->
            meterRegistry.counter("tpts_saksbehandlingapi_utbetalingsoversikt_fjernede_ytelser", "aarsak", årsak.tilMetrikkverdi())
                .increment((fjernedeYtelserPerÅrsak[årsak] ?: 0).toDouble())
        }
    }

    private fun Avgrensningsårsak.tilMetrikkverdi(): String = when (this) {
        Avgrensningsårsak.MANGLER_YTELSESTYPE -> "uten_ytelsestype"
        Avgrensningsårsak.UTELATT_YTELSESTYPE -> "annen_ytelsestype"
        Avgrensningsårsak.UKJENT_YTELSESTYPE -> "ukjent_ytelsestype"
        Avgrensningsårsak.UTENFOR_PERIODENE -> "utenfor_periodene"
        Avgrensningsårsak.ANNEN_ELLER_MANGLENDE_RETTIGHETSHAVER -> "annen_rettighetshaver"
    }

    private fun Oppslagsfeiltype.tilMetrikkverdi(): String = when (this) {
        Oppslagsfeiltype.TILGANG_AVVIST -> "tilgang_avvist"
        Oppslagsfeiltype.TJENESTEFEIL -> "tjenestefeil"
        Oppslagsfeiltype.SAK_AVVIST -> "sak_avvist"
        Oppslagsfeiltype.ULESELIG_SVAR -> "uleselig_svar"
        Oppslagsfeiltype.UGYLDIG_INNHOLD -> "ugyldig_innhold"
    }

    private companion object {
        const val MAKS_FEIL_PER_KJØRING = 2

        /** Dette er en driftsgrense mot hamring og ikke et anslag på når tjenesten er tilbake; etter tre pauser på rad varer den en time. */
        val PAUSELENGDER = mapOf(1L to 5.minutes, 2L to 15.minutes, 3L to 30.minutes)
    }
}
