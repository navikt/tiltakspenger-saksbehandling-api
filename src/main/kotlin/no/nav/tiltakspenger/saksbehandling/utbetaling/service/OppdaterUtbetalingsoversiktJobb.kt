package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.KunneIkkeHenteUtbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsfeiltype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsperiodetype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsplan
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.tilUtbetalingsoversiktMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider.erInnenforØkonomisystemetsÅpningstider
import no.nav.tiltakspenger.saksbehandling.vedtak.periodeForUtbetalingsoversikt
import java.time.Clock
import kotlin.time.toJavaDuration

class OppdaterUtbetalingsoversiktJobb(
    private val sakRepo: SakRepo,
    private val utbetalingsoversiktRepo: UtbetalingsoversiktRepo,
    private val utbetalingsoversiktklient: Utbetalingsoversiktklient,
    private val clock: Clock,
    private val meterRegistry: MeterRegistry,
    private val limit: Int,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Oppdaterer sakene som er klare, og gjør ingenting når økonomisystemet er stengt.
     * En kjøring som starter rett før stengetid kan gå noen minutter over; stengetiden 20:50 har margin til 21:00 for det.
     * Stopper ved første feil som rammer alle saker, ved en uventet feil, og når [MAKS_FEIL_PER_KJØRING] oppslag har feilet.
     * Eierteamet varsles ved mer enn to feil på tre minutter, og tjenesten sier ikke fra om at økonomisystemet er stengt.
     */
    suspend fun oppdaterUtbetalingsoversikter() {
        if (!erInnenforØkonomisystemetsÅpningstider(clock)) return
        var antallFeil = 0
        for (sakId in utbetalingsoversiktRepo.hentSakerKlareForOppslag(nå = nå(clock), limit = limit)) {
            val skalStoppe = Either.catch { oppdaterForSak(sakId) }.fold(
                ifLeft = {
                    logger.error(it) { "Uventet feil ved oppdatering av utbetalingsoversikt, stopper kjøringen. sakId=$sakId." }
                    true
                },
                ifRight = { utfall ->
                    utfall.fold(
                        ifLeft = { feil ->
                            antallFeil++
                            feil.rammerAlleSaker || antallFeil >= MAKS_FEIL_PER_KJØRING
                        },
                        ifRight = { false },
                    )
                },
            )
            if (skalStoppe) return
        }
    }

    /**
     * Slår opp utbetalingene for én sak og lagrer utfallet, også når oppslaget feiler.
     * Utfallet av kallet logges og måles før lagringen, så en lagringsfeil ikke skjuler det.
     * Forutsetter at saken finnes og har minst én sendt utbetaling; da har den også et vedtak med periode.
     */
    suspend fun oppdaterForSak(sakId: SakId): Either<KunneIkkeHenteUtbetalingsoversikt, Unit> {
        val sak = sakRepo.hentForSakId(sakId)!!
        val oppslag = Oppslag(sak.vedtaksliste.periodeForUtbetalingsoversikt()!!, Oppslagsperiodetype.YTELSESPERIODE)
        val tidligereFeilPåRad = utbetalingsoversiktRepo.hentStatusForSak(sakId).antallFeilPåRad
        val correlationId = CorrelationId.generate()
        val kontekst = "sakId=${sak.id}, saksnummer=${sak.saksnummer}, correlationId=${correlationId.value}"

        return utbetalingsoversiktklient.hent(sak.fnr, oppslag.periode, oppslag.periodetype, correlationId)
            .onRight { response ->
                val hentet = response.metadata.hentet()
                val oversikt = Utbetalingsoversikt.Vellykket(
                    id = UtbetalingsoversiktId.random(),
                    sakId = sak.id,
                    hentet = hentet,
                    oppslag = oppslag,
                    plan = Oppslagsplan.etterVellykketOppslag(
                        hentet = hentet,
                        harNyligSendtUtbetaling = sak.utbetalinger.harOkUtbetalingSendtEtter(hentet.minusDays(30)),
                    ),
                    utbetalinger = response.body,
                )
                registrerMetrikker(resultat = "vellykket", metadata = response.metadata)
                logger.info {
                    "Hentet utbetalingsoversikt. $kontekst, antallUtbetalinger=${oversikt.utbetalinger.size}, nesteOppslag=${oversikt.plan.nesteOppslag}."
                }
                utbetalingsoversiktRepo.lagre(oversikt, response.metadata.tilUtbetalingsoversiktMetadata(correlationId))
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
                feil.logg("$kontekst, nesteOppslag=${oversikt.plan.nesteOppslag}")
                utbetalingsoversiktRepo.lagre(oversikt, feil.metadata.tilUtbetalingsoversiktMetadata(correlationId))
            }
            .map { }
    }

    private fun HttpKlientMetadata.hentet() = tidsstempler.responsMottatt ?: nå(clock)

    private fun registrerMetrikker(resultat: String, metadata: HttpKlientMetadata) {
        meterRegistry.counter("tpts_saksbehandlingapi_utbetalingsoversikt_oppslag", "resultat", resultat).increment()
        meterRegistry.timer("tpts_saksbehandlingapi_utbetalingsoversikt_oppslag_varighet").record(metadata.totalDuration.toJavaDuration())
    }

    private fun Oppslagsfeiltype.tilMetrikkverdi(): String = when (this) {
        Oppslagsfeiltype.TILGANG_AVVIST -> "tilgang_avvist"
        Oppslagsfeiltype.TJENESTEFEIL -> "tjenestefeil"
        Oppslagsfeiltype.SAK_AVVIST -> "sak_avvist"
        Oppslagsfeiltype.ULESELIG_SVAR -> "uleselig_svar"
        Oppslagsfeiltype.UGYLDIG_INNHOLD -> "ugyldig_innhold"
    }

    private fun KunneIkkeHenteUtbetalingsoversikt.logg(kontekst: String) {
        when (this) {
            is KunneIkkeHenteUtbetalingsoversikt.TilgangAvvist -> httpKlientError.loggFeil(logger, OPERASJON, kontekst)

            is KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil -> httpKlientError.loggFeil(logger, OPERASJON, kontekst)

            is KunneIkkeHenteUtbetalingsoversikt.SakAvvist -> httpKlientError.loggFeil(logger, OPERASJON, kontekst)

            is KunneIkkeHenteUtbetalingsoversikt.UleseligSvar -> httpKlientError.loggFeil(logger, OPERASJON, kontekst)

            is KunneIkkeHenteUtbetalingsoversikt.UgyldigInnhold -> {
                val start = "Feil ved $OPERASJON. $kontekst. Svaret brøt kontrakten: $feil mot ${metadata.endepunkt}."
                logger.error { "$start ${Sikkerlogg.seSikkerlogg}" }
                Sikkerlogg.error { "$start Request: ${metadata.rawRequestString}. Response: ${metadata.rawResponseString}." }
            }
        }
    }

    private companion object {
        const val OPERASJON = "henting av utbetalingsoversikt"
        const val MAKS_FEIL_PER_KJØRING = 2
    }
}
