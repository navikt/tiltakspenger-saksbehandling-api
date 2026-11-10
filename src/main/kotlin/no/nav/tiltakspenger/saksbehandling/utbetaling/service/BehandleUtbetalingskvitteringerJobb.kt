package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.jobber.tilSamletResultat
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.Utbetalingsmålinger
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeBehandleUtbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KvitteringsstatusLagring
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.RåUtbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskobling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsreferanse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.Clock

/**
 * Behandler kvitteringer i offset-rekkefølge innen topic og partisjon, med backoff som lagres i databasen.
 * En ubehandlet kvittering blokkerer høyere offset for samme nøkkel i samme topic og partisjon, også under backoff.
 * Kafka gir ingen rekkefølge på tvers av partisjoner.
 * Oppfølgingen teller også utbetalinger uten kvittering, uavhengig av køen og neste forsøk.
 */
class BehandleUtbetalingskvitteringerJobb(
    private val utbetalingskvitteringRepo: UtbetalingskvitteringRepo,
    private val utbetalingRepo: UtbetalingRepo,
    private val tilUtbetalingsstatus: (String) -> Either<KunneIkkeBehandleUtbetalingskvittering, Utbetalingskvittering>,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val målinger: Utbetalingsmålinger,
    private val log: KLogger,
    private val sikkerlogg: Sikkerlogg,
) {
    fun behandle(): TaskResultat {
        val resultater = utbetalingskvitteringRepo.hentUbehandledeIder(nå(clock), limit = 100).map {
            behandle(it).fold({ TaskResultat.Feilet }, { TaskResultat.Ferdig })
        }
        følgOppUtbetalinger()
        return resultater.tilSamletResultat()
    }

    fun behandle(id: UtbetalingskvitteringId): Either<KunneIkkeBehandleUtbetalingskvittering, Unit> {
        // Closure-resultatet er utilgjengelig ved commit-feil, så denne feilinformasjonen må overleve rollback.
        var kvitteringVedFeil: RåUtbetalingskvittering? = null
        var kontekst = "kvitteringId=$id"

        fun registrerKobling(
            kvittering: RåUtbetalingskvittering,
            utbetaling: VedtattUtbetaling,
        ): Either<KunneIkkeBehandleUtbetalingskvittering, Utbetalingskobling> = either {
            val kobling = Utbetalingskobling(utbetaling.id, utbetaling.sakId)
            kontekst = "$kontekst, ${utbetaling.enkelLoggKontekst}"
            ensure(kvittering.utbetalingId == null || kvittering.utbetalingId == kobling.utbetalingId) {
                KunneIkkeBehandleUtbetalingskvittering.LagretUtbetalingskoblingAvviker
            }
            ensure(kvittering.sakId == null || kvittering.sakId == kobling.sakId) {
                KunneIkkeBehandleUtbetalingskvittering.LagretUtbetalingskoblingAvviker
            }
            kvitteringVedFeil = kvittering.knyttTil(kobling)
            kobling
        }

        fun behandleTransaksjonelt(): Either<KunneIkkeBehandleUtbetalingskvittering, Behandlingstransaksjon> =
            Either.catch {
                sessionFactory.withTransactionContext { context ->
                    val råKvittering = utbetalingskvitteringRepo.hent(id, context)
                    kvitteringVedFeil = råKvittering
                    either {
                        if (!råKvittering.skalBehandles(clock)) {
                            return@either Behandlingstransaksjon(råKvittering, kontekst, null)
                        }

                        val v3UtbetalingId = utledV3UtbetalingId(råKvittering.key).bind()
                        val v3Utbetaling = v3UtbetalingId?.let { hentUtbetaling(it, context).bind() }
                        if (v3Utbetaling != null) {
                            registrerKobling(råKvittering, v3Utbetaling).bind()
                        }

                        val value = ensureNotNull(råKvittering.value) {
                            KunneIkkeBehandleUtbetalingskvittering.ManglerInnhold
                        }
                        val kvittering = tilUtbetalingsstatus(value).bind()
                        val utbetaling = finnUtbetalingForKvittering(
                            råKvittering = råKvittering,
                            kvittering = kvittering,
                            v3Utbetaling = v3Utbetaling,
                            context = context,
                        ).bind()
                        val kobling = when {
                            utbetaling == null -> kvitteringVedFeil?.utbetalingskobling
                            v3Utbetaling != null -> Utbetalingskobling(v3Utbetaling.id, v3Utbetaling.sakId)
                            else -> registrerKobling(råKvittering, utbetaling).bind()
                        }

                        val lagretStatus = when (kvittering) {
                            is Utbetalingskvittering.Simulering -> null

                            is Utbetalingskvittering.Status -> {
                                val funnetUtbetaling = ensureNotNull(utbetaling) {
                                    KunneIkkeBehandleUtbetalingskvittering.ManglerUtbetalingskobling
                                }
                                if (utbetalingRepo.hentOppdragsversjon(funnetUtbetaling.sakId) == Oppdragsversjon.V3) {
                                    when (
                                        utbetalingRepo.lagreKvitteringsstatus(
                                            funnetUtbetaling.id,
                                            kvittering.status,
                                            context,
                                        )
                                    ) {
                                        KvitteringsstatusLagring.Lagret -> kvittering.status

                                        KvitteringsstatusLagring.IgnorertFordiUtbetalingenErTerminal -> null

                                        KvitteringsstatusLagring.FantIkkeUtbetaling -> raise(
                                            KunneIkkeBehandleUtbetalingskvittering.FantIkkeUtbetaling(
                                                funnetUtbetaling.id,
                                            ),
                                        )
                                    }
                                } else {
                                    null
                                }
                            }
                        }

                        utbetalingskvitteringRepo.markerBehandlet(id, nå(clock), kobling, context)
                        Behandlingstransaksjon(kvitteringVedFeil ?: råKvittering, kontekst, lagretStatus)
                    }
                }
            }.mapLeft { KunneIkkeBehandleUtbetalingskvittering.UventetFeil(it) }.flatMap { it }

        return behandleTransaksjonelt()
            .onLeft { feil -> lagreFeilOgLogge(id, feil, kvitteringVedFeil, kontekst) }
            .onRight(::loggLagretStatus)
            .map { }
    }

    private fun finnUtbetalingForKvittering(
        råKvittering: RåUtbetalingskvittering,
        kvittering: Utbetalingskvittering,
        v3Utbetaling: VedtattUtbetaling?,
        context: TransactionContext,
    ): Either<KunneIkkeBehandleUtbetalingskvittering, VedtattUtbetaling?> = either {
        if (v3Utbetaling != null) return@either v3Utbetaling
        if (kvittering.referanser.isNotEmpty()) return@either oppslagOgKobling(kvittering.referanser, context).bind()
        if (råKvittering.utbetalingId != null) return@either hentLagretUtbetaling(råKvittering, context).bind()
        if (kvittering is Utbetalingskvittering.Simulering) return@either null
        raise(KunneIkkeBehandleUtbetalingskvittering.ManglerUtbetalingskobling)
    }

    private fun oppslagOgKobling(
        referanser: Set<Utbetalingsreferanse>,
        context: TransactionContext,
    ): Either<KunneIkkeBehandleUtbetalingskvittering, VedtattUtbetaling> = either {
        if (referanser.isEmpty()) raise(KunneIkkeBehandleUtbetalingskvittering.ManglerUtbetalingskobling)
        val treff = utbetalingRepo.finnUtbetalingerForKvitteringsreferanser(referanser, context)
        if (treff.isEmpty()) raise(KunneIkkeBehandleUtbetalingskvittering.ManglerUtbetalingskobling)
        ensure(
            treff.size == referanser.size &&
                treff.map { it.referanse }.toSet() == referanser &&
                treff.map { it.kobling }.distinct().size == 1,
        ) {
            KunneIkkeBehandleUtbetalingskvittering.TvetydigUtbetalingskobling
        }
        val forventetKobling = treff.first().kobling
        val utbetaling = ensureNotNull(utbetalingRepo.hent(forventetKobling.utbetalingId, context)) {
            KunneIkkeBehandleUtbetalingskvittering.FantIkkeUtbetaling(forventetKobling.utbetalingId)
        }
        ensure(utbetaling.sakId == forventetKobling.sakId) {
            KunneIkkeBehandleUtbetalingskvittering.LagretUtbetalingskoblingAvviker
        }
        utbetaling
    }

    private fun hentLagretUtbetaling(
        kvittering: RåUtbetalingskvittering,
        context: TransactionContext,
    ): Either<KunneIkkeBehandleUtbetalingskvittering, VedtattUtbetaling> = either {
        val utbetalingId = ensureNotNull(kvittering.utbetalingId) {
            KunneIkkeBehandleUtbetalingskvittering.ManglerUtbetalingskobling
        }
        val utbetaling = hentUtbetaling(utbetalingId, context).bind()
        ensure(kvittering.sakId == null || kvittering.sakId == utbetaling.sakId) {
            KunneIkkeBehandleUtbetalingskvittering.LagretUtbetalingskoblingAvviker
        }
        utbetaling
    }

    private fun lagreFeilOgLogge(
        id: UtbetalingskvitteringId,
        feil: KunneIkkeBehandleUtbetalingskvittering,
        kvittering: RåUtbetalingskvittering?,
        kontekst: String,
    ) {
        val feilnavn = feil.javaClass.simpleName
        kvittering?.let {
            utbetalingskvitteringRepo.lagreFeil(
                id,
                feilnavn,
                feil.forbigående,
                it.registrerForsøk(clock),
                it.utbetalingskobling,
            )
        }
        val melding = "Kunne ikke behandle utbetalingskvittering: $feilnavn, forbigående=${feil.forbigående}, $kontekst. Se sikkerlogg for detaljer."
        if (feil.forbigående) log.warn { melding } else log.error { melding }
        val årsak = when (feil) {
            is KunneIkkeBehandleUtbetalingskvittering.KunneIkkeTolkeInnhold -> feil.feil

            is KunneIkkeBehandleUtbetalingskvittering.UventetFeil -> feil.feil

            is KunneIkkeBehandleUtbetalingskvittering.FantIkkeUtbetaling,
            KunneIkkeBehandleUtbetalingskvittering.UgyldigInnhold,
            KunneIkkeBehandleUtbetalingskvittering.UgyldigNøkkel,
            KunneIkkeBehandleUtbetalingskvittering.ManglerInnhold,
            KunneIkkeBehandleUtbetalingskvittering.ManglerUtbetalingskobling,
            KunneIkkeBehandleUtbetalingskvittering.TvetydigUtbetalingskobling,
            KunneIkkeBehandleUtbetalingskvittering.LagretUtbetalingskoblingAvviker,
            -> null
        }
        if (feil.forbigående) {
            sikkerlogg.warn(årsak) { "$melding Nøkkel=${kvittering?.key}, innhold=${kvittering?.value}" }
        } else {
            sikkerlogg.error(årsak) { "$melding Nøkkel=${kvittering?.key}, innhold=${kvittering?.value}" }
        }
    }

    private fun loggLagretStatus(transaksjon: Behandlingstransaksjon) {
        transaksjon.lagretStatus?.let { status ->
            if (status == Utbetalingsstatus.FeiletMotOppdrag) {
                log.error { "Utbetaling feilet mot oppdrag med status $status. ${transaksjon.kontekst}. Må følges opp manuelt. Se sikkerlogg for detaljer." }
                sikkerlogg.error { "Utbetaling feilet mot oppdrag med status $status. ${transaksjon.kontekst}. Innhold=${transaksjon.kvittering.value}" }
            } else {
                log.info { "Lagret utbetalingsstatus $status. ${transaksjon.kontekst}. Se sikkerlogg for detaljer." }
                sikkerlogg.info { "Lagret utbetalingsstatus $status. ${transaksjon.kontekst}. Innhold=${transaksjon.kvittering.value}" }
            }
        }
    }

    private fun hentUtbetaling(
        utbetalingId: UtbetalingId,
        context: TransactionContext,
    ) = either {
        ensureNotNull(utbetalingRepo.hent(utbetalingId, context)) {
            KunneIkkeBehandleUtbetalingskvittering.FantIkkeUtbetaling(utbetalingId)
        }
    }

    private fun utledV3UtbetalingId(key: String?): Either<KunneIkkeBehandleUtbetalingskvittering, UtbetalingId?> {
        if (key == null || !key.startsWith(V3_KEY_PREFIX)) return Either.Right(null)
        return Either.catch { UtbetalingId.fromString(key) }
            .mapLeft { KunneIkkeBehandleUtbetalingskvittering.UgyldigNøkkel }
    }

    private var sistVarslet: Pair<Long, Long>? = null

    /** Tellerne mates fra lagret tilstand, også etter omstart og mens kvitteringene venter på neste forsøk. */
    fun følgOppUtbetalinger() {
        val frist = nå(clock).minusDays(3)
        val feilet = utbetalingRepo.antallFeiledeUtbetalingerPåVersjon3() + utbetalingskvitteringRepo.antallPermanenteFeil()
        val ikkeOk = utbetalingRepo.antallUtbetalingerUtenOkPåVersjon3(frist) + utbetalingskvitteringRepo.antallForbigåendeFeil(frist)
        målinger.feilet.increment(feilet.toDouble())
        målinger.ikkeOk.increment(ikkeOk.toDouble())
        if (sistVarslet != (feilet to ikkeOk)) {
            if (feilet > 0) log.error { "Utbetaling eller utbetalingskvittering krever manuell oppfølging: $feilet permanente avvik." }
            if (ikkeOk > 0) log.error { "Utbetaling eller utbetalingskvittering har ikke fått OK-status innen 3 dager: $ikkeOk avvik krever manuell oppfølging." }
            sistVarslet = feilet to ikkeOk
        }
    }

    private data class Behandlingstransaksjon(
        val kvittering: RåUtbetalingskvittering,
        val kontekst: String,
        val lagretStatus: Utbetalingsstatus?,
    )

    private companion object {
        const val V3_KEY_PREFIX = "utbetaling_"
    }
}
