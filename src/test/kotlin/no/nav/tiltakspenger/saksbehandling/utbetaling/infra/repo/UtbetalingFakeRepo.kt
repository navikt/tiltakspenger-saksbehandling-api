package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk.Companion.opprett
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KvitteringsstatusLagring
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingResponse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskobling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsreferanse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingsreferanseTreff
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.collections.set

class UtbetalingFakeRepo : UtbetalingRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<UtbetalingId, VedtattUtbetaling>())
    private val response = arrow.atomic.Atomic(mutableMapOf<UtbetalingId, UtbetalingResponse>())
    private val requestSnapshots = arrow.atomic.Atomic(mutableMapOf<UtbetalingId, String>())
    private val oppdragsversjoner = arrow.atomic.Atomic(mutableMapOf<SakId, Oppdragsversjon>())

    fun settOppdragsversjon(sakId: SakId, oppdragsversjon: Oppdragsversjon) {
        oppdragsversjoner.get()[sakId] = oppdragsversjon
    }

    fun lagre(
        utbetaling: VedtattUtbetaling,
        context: TransactionContext?,
    ) {
        data.get()[utbetaling.id] = utbetaling
    }

    override fun hent(utbetalingId: UtbetalingId, context: TransactionContext): VedtattUtbetaling? =
        data.get()[utbetalingId]

    override fun finnUtbetalingerForKvitteringsreferanser(
        referanser: Set<Utbetalingsreferanse>,
        context: TransactionContext,
    ): List<UtbetalingsreferanseTreff> =
        data.get().values.mapNotNull { utbetaling ->
            val referanse = Utbetalingsreferanse(utbetaling.id.uuidPart())
            if (referanse in referanser) {
                UtbetalingsreferanseTreff(
                    referanse = referanse,
                    kobling = Utbetalingskobling(utbetaling.id, utbetaling.sakId),
                )
            } else {
                null
            }
        }

    override fun antallFeiledeUtbetalingerPåVersjon3(): Long =
        data.get().values.count {
            it.status == Utbetalingsstatus.FeiletMotOppdrag &&
                hentOppdragsversjon(it.sakId) == Oppdragsversjon.V3
        }.toLong()

    override fun antallUtbetalingerUtenOkPåVersjon3(eldreEnn: LocalDateTime): Long =
        data.get().values.count {
            it.status in listOf(
                null,
                Utbetalingsstatus.IkkePåbegynt,
                Utbetalingsstatus.SendtTilOppdrag,
            ) &&
                it.opprettet <= eldreEnn &&
                hentOppdragsversjon(it.sakId) == Oppdragsversjon.V3
        }.toLong()

    override fun markerSendtTilUtbetaling(
        utbetalingId: UtbetalingId,
        tidspunkt: LocalDateTime,
        context: TransactionContext,
    ) {
        data.get()[utbetalingId] = data.get()[utbetalingId]!!.copy(sendtTilUtbetaling = tidspunkt)
    }

    override fun lagreUtbetalingsrespons(
        utbetalingId: UtbetalingId,
        utbetalingsrespons: UtbetalingResponse,
        context: TransactionContext?,
    ) {
        response.get()[utbetalingId] = utbetalingsrespons
    }

    override fun hentUtbetalingJson(utbetalingId: UtbetalingId): String? {
        return requestSnapshots.get()[utbetalingId] ?: response.get()[utbetalingId]?.request
    }

    override fun lagreUtbetalingJsonHvisMangler(
        utbetalingId: UtbetalingId,
        request: String,
        context: TransactionContext,
    ): String? = synchronized(requestSnapshots) {
        if (data.get()[utbetalingId] == null) return@synchronized null
        requestSnapshots.get().getOrPut(utbetalingId) { request }
    }

    override fun hentForUtsjekk(limit: Int): List<VedtattUtbetaling> {
        return data.get().values.filter { it.sendtTilUtbetaling == null }.take(limit)
    }

    override fun oppdaterUtbetalingsstatus(
        utbetalingId: UtbetalingId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext?,
    ) {
        data.get()[utbetalingId] = data.get()[utbetalingId]!!.copy(status = status)
    }

    override fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int): List<UtbetalingDetSkalHentesStatusFor> {
        return data.get().values.filter {
            it.status in listOf(
                null,
                Utbetalingsstatus.IkkePåbegynt,
                Utbetalingsstatus.SendtTilOppdrag,
            ) &&
                it.sendtTilUtbetaling != null &&
                hentOppdragsversjon(it.sakId) == Oppdragsversjon.V2
        }.take(limit).map {
            UtbetalingDetSkalHentesStatusFor(
                utbetalingId = it.id,
                sakId = it.sakId,
                saksnummer = it.saksnummer,
                opprettet = it.opprettet,
                sendtTilUtbetalingstidspunkt = it.sendtTilUtbetaling!!,
                forsøkshistorikk = opprett(
                    forrigeForsøk = it.sendtTilUtbetaling.plus(1, ChronoUnit.MICROS),
                    antallForsøk = 1,
                    clock = fixedClock,
                ),
            )
        }
    }

    override fun hentOppdragsversjon(
        sakId: SakId,
        context: TransactionContext?,
    ): Oppdragsversjon {
        return oppdragsversjoner.get()[sakId] ?: Oppdragsversjon.V2
    }

    override fun lagreKvitteringsstatus(
        utbetalingId: UtbetalingId,
        status: Utbetalingsstatus,
        context: TransactionContext?,
    ): KvitteringsstatusLagring {
        val utbetaling = data.get()[utbetalingId] ?: return KvitteringsstatusLagring.FantIkkeUtbetaling
        if (hentOppdragsversjon(utbetaling.sakId) != Oppdragsversjon.V3) return KvitteringsstatusLagring.FantIkkeUtbetaling
        if (utbetaling.status in TERMINALE_STATUSER && status !in TERMINALE_STATUSER) {
            return KvitteringsstatusLagring.IgnorertFordiUtbetalingenErTerminal
        }
        data.get()[utbetalingId] = utbetaling.copy(status = status)
        return KvitteringsstatusLagring.Lagret
    }

    private companion object {
        val TERMINALE_STATUSER = setOf(
            Utbetalingsstatus.Ok,
            Utbetalingsstatus.OkUtenUtbetaling,
            Utbetalingsstatus.FeiletMotOppdrag,
            Utbetalingsstatus.Avbrutt,
        )
    }
}
