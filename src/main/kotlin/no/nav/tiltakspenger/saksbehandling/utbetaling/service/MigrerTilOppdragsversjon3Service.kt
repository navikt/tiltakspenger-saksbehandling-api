package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.harStatus
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeMigrereTilOppdragsversjon3
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeOppheveUtbetalingssperre
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MigrertTilOppdragsversjon3
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsmigrering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.V3Simuleringsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.kanMigreresTilOppdragsversjon3
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.logg
import java.time.Clock

/**
 * Migrerer en sak fra [Oppdragsversjon.V2] til [Oppdragsversjon.V3] hos oss og hos Hel Ved i tre trinn, med en [Oppdragsmigrering] for hvert:
 * 1. [migrer] sperrer utbetalingene og simuleringene i saken ([Oppdragsmigrering.SPERRET]).
 * 2. [migrer] ber Hel Ved migrere kjedene, og bytter deretter saken til [Oppdragsversjon.V3] ([Oppdragsmigrering.MIGRERT_HOS_HEL_VED]).
 * 3. [opphevUtbetalingssperre] kalles av `OpphevUtbetalingssperrerJobb` og opphever sperren når en ny simulering av siste sendte utbetaling ikke viser endring ([Oppdragsmigrering.FERDIG]).
 * Når `opphevSperrenStraks` er true, settes saken til [Oppdragsmigrering.FERDIG] etter OK fra Hel Ved uten kontrollsimulering.
 *
 * Hel Ved sender de migrerte kjedene videre asynkront, og en [Oppdragsversjon.V3]-utbetaling som kommer før de er tatt opp, kan bli behandlet som en ny kjede.
 * Sperren hindrer det, og fordi Hel Ved ikke sier fra når kjedene er tatt opp, simulerer jobben til simuleringen ikke viser endring.
 * Gir Hel Ved oss en status for migreringen, kan jobben bruke den i stedet for simuleringen.
 * Hel Ved svarer OK når saken allerede er migrert, så [migrer] kan kjøres på nytt etter en feil.
 * En sak uten sendte utbetalinger har ingen kjeder hos Hel Ved, og byttes bare hos oss.
 *
 * @param erProd Migreringen er avslått i prod.
 */
class MigrerTilOppdragsversjon3Service(
    private val sakRepo: SakRepo,
    private val utbetalingRepo: UtbetalingRepo,
    private val utbetalingsklient: Utbetalingsklient,
    private val v3Simuleringsklient: V3Simuleringsklient,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val erProd: Boolean,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun migrer(
        sakId: SakId,
        opphevSperrenStraks: Boolean = false,
    ): Either<KanIkkeMigrereTilOppdragsversjon3, MigrertTilOppdragsversjon3> {
        if (erProd) return KanIkkeMigrereTilOppdragsversjon3.FunksjonenErIkkePåslått.left()

        val sakFørSperre = hentSak(sakId)
        val kontekst = "sakId: $sakId, saksnummer: ${sakFørSperre.saksnummer}"

        if (utbetalingRepo.hentOppdragsversjon(sakId) == Oppdragsversjon.V3) {
            logger.info { "Saken er allerede migrert til oppdragsversjon 3. $kontekst" }
            return MigrertTilOppdragsversjon3(sakId, sakFørSperre.saksnummer, alleredeMigrert = true).right()
        }

        sperrUtbetalinger(sakId).onLeft { return it.left() }

        // Leses på nytt etter sperren, så en utbetaling som ble sendt før sperren, også gir migrering hos Hel Ved.
        val sak = hentSak(sakId)
        if (sak.utbetalinger.any { it.sendtTilUtbetaling != null }) {
            utbetalingsklient.migrerTilOppdragsversjon3(sak.saksnummer).onLeft {
                return if (it.harStatus(423)) {
                    KanIkkeMigrereTilOppdragsversjon3.HelvedHarIkkeKvittertSisteIverksetting(it).left()
                } else {
                    KanIkkeMigrereTilOppdragsversjon3.HelvedFeilet(it).left()
                }
            }
        }
        utbetalingRepo.oppdaterTilOppdragsversjon3(sakId, nå(clock)).onLeft { return it.left() }
        if (opphevSperrenStraks) {
            utbetalingRepo.opphevUtbetalingssperre(sakId).onLeft { return it.left() }
            logger.info { "Opphevet utbetalingssperren uten kontrollsimulering som del av migreringen. $kontekst" }
        } else {
            logger.info { "Migrerte saken til oppdragsversjon 3. Utbetalingssperren står til jobben opphever den. Antall utbetalinger: ${sak.utbetalinger.size}. $kontekst" }
        }
        return MigrertTilOppdragsversjon3(sakId, sak.saksnummer, alleredeMigrert = false).right()
    }

    /**
     * Opphever sperren når en ny simulering av siste sendte utbetaling ikke viser endring.
     * Sperren står til neste forsøk ellers, og en sak som har ventet lengre enn [Oppdragstilstand.FORVENTET_TID_HOS_HEL_VED], logges som feil.
     * En sak som ikke er sperret, gir [Unit] uten at noe endres.
     */
    suspend fun opphevUtbetalingssperre(sakId: SakId): Either<KanIkkeOppheveUtbetalingssperre, Unit> {
        val sak = hentSak(sakId)
        val kontekst = "sakId: $sakId, saksnummer: ${sak.saksnummer}"
        val tilstand = utbetalingRepo.hentOppdragstilstand(sakId)
        if (!tilstand.sperrerUtbetalinger) return Unit.right()

        return simulerSisteSendteUtbetalingPåNytt(sak, kontekst)
            .flatMap { utbetalingRepo.opphevUtbetalingssperre(sakId) }
            .onLeft { feil ->
                if (feil is Loggbar) {
                    val melding = "${feil.loggkontekst.melding}. $kontekst, migrert hos Hel Ved: ${tilstand.migrertHosHelVedTidspunkt}"
                    if (tilstand.venterForLengePåHelVed(nå(clock))) logger.error { melding } else logger.info { melding }
                }
            }
            .onRight { logger.info { "Opphevet utbetalingssperren. $kontekst" } }
    }

    /**
     * Simulerer den siste sendte utbetalingen på nytt på [Oppdragsversjon.V3], med sakstilstanden vi sendte den med.
     * Har Hel Ved tatt opp de migrerte kjedene, er det ingenting å endre, og Hel Ved svarer at simuleringen er uten endring.
     * Mangler kjedene, simuleres utbetalingen som nye kjeder, og neste utbetaling ville gitt dobbel utbetaling.
     * Bare [Simulering.IngenEndring] regnes som uendret; all annen simulering og alle feil lar sperren stå.
     * En sak uten sendte utbetalinger har ingen kjeder hos Hel Ved, og trenger ingen simulering.
     */
    private suspend fun simulerSisteSendteUtbetalingPåNytt(sak: Sak, kontekst: String): Either<KanIkkeOppheveUtbetalingssperre, Unit> {
        val utbetaling = sak.utbetalinger.lastOrNull { it.sendtTilUtbetaling != null } ?: return Unit.right()
        val simulering = v3Simuleringsklient.simuler(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            behandlingId = utbetaling.beregningKilde.id,
            fnr = utbetaling.fnr,
            beregning = utbetaling.beregning,
            forrigeUtbetalingJson = utbetalingRepo.hentUtbetalingJson(utbetaling.id),
            meldeperiodeKjeder = sak.meldeperiodeKjeder,
            saksbehandler = utbetaling.saksbehandler,
            brukersNavkontor = utbetaling.brukerNavkontor,
        ).getOrElse {
            // KunneIkkeSimulere logger selv på riktig nivå, så jobben logger ikke denne feilen.
            it.logg(logger, "$kontekst, utbetalingId: ${utbetaling.id}. Utbetalingssperren står.")
            return KanIkkeOppheveUtbetalingssperre.SimuleringFeilet(it).left()
        }.simulering
        if (simulering !is Simulering.IngenEndring) return KanIkkeOppheveUtbetalingssperre.SimuleringenViserEndring(utbetaling.id).left()
        return Unit.right()
    }

    /**
     * Sakraden låses før sjekken, slik at ingen utbetaling kan sendes mellom sjekken og sperren, og sperren settes bare når sjekken går gjennom.
     * En sak som allerede er sperret, sjekkes ikke på nytt: ingenting er sendt siden sjekken, og utbetalinger som venter på sperren, skal ikke stoppe et nytt forsøk.
     */
    private fun sperrUtbetalinger(sakId: SakId): Either<KanIkkeMigrereTilOppdragsversjon3.HarUtbetalingSomIkkeErKvittert, Unit> {
        return sessionFactory.withTransactionContext { tx ->
            val alleredeSperret = utbetalingRepo.låsOppdragstilstand(sakId, tx).migrering != null
            val kanSperres = if (alleredeSperret) Unit.right() else hentSak(sakId).utbetalinger.kanMigreresTilOppdragsversjon3()
            kanSperres.onRight { utbetalingRepo.sperrUtbetalinger(sakId, tx) }
        }
    }

    private fun hentSak(sakId: SakId): Sak {
        return sakRepo.hentForSakId(sakId) ?: throw IkkeFunnetException("Fant ikke sak med sakId $sakId")
    }
}
