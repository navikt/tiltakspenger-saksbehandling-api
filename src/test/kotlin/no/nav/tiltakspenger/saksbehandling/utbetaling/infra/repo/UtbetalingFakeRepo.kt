package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk.Companion.opprett
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingResponse
import no.nav.utsjekk.kontrakter.iverksett.IverksettV2Dto
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.collections.set

class UtbetalingFakeRepo : UtbetalingRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<UtbetalingId, VedtattUtbetaling>())
    private val response = arrow.atomic.Atomic(mutableMapOf<UtbetalingId, UtbetalingResponse>())

    override fun lagre(
        utbetaling: VedtattUtbetaling,
        context: TransactionContext?,
    ) {
        data.get()[utbetaling.id] = utbetaling
    }

    override fun markerSendtTilUtbetaling(
        utbetalingId: UtbetalingId,
        tidspunkt: LocalDateTime,
        utbetalingsrespons: SendtUtbetaling,
    ) {
        val utbetaling = data.get()[utbetalingId]!!

        data.get()[utbetalingId] = utbetaling.copy(
            sendtTilUtbetaling = VedtattUtbetaling.SendtTilUtbetaling(
                sendtTidspunkt = tidspunkt,
                satstype = deserialize<IverksettV2Dto>(utbetalingsrespons.request).vedtak.utbetalinger.tilSatstypePeriodisering(
                    utbetaling.periode,
                ),
                status = null,
            ),
        )
        response.get()[utbetalingId] = utbetalingsrespons
    }

    override fun lagreFeilResponsFraUtbetaling(
        utbetalingId: UtbetalingId,
        utbetalingsrespons: KunneIkkeUtbetale,
    ) {
        response.get()[utbetalingId] = utbetalingsrespons
    }

    override fun hentUtbetalingJson(utbetalingId: UtbetalingId): String? {
        return response.get()[utbetalingId]?.request
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
        val utbetaling = data.get()[utbetalingId]!!
        data.get()[utbetalingId] =
            utbetaling.copy(sendtTilUtbetaling = utbetaling.sendtTilUtbetaling!!.copy(status = status))
    }

    override fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int): List<UtbetalingDetSkalHentesStatusFor> {
        return data.get().values.filter {
            it.status in listOf(
                null,
                Utbetalingsstatus.IkkePåbegynt,
                Utbetalingsstatus.SendtTilOppdrag,
            ) &&
                it.sendtTilUtbetaling != null
        }.take(limit).map {
            UtbetalingDetSkalHentesStatusFor(
                utbetalingId = it.id,
                sakId = it.sakId,
                saksnummer = it.saksnummer,
                opprettet = it.opprettet,
                sendtTilUtbetalingstidspunkt = it.sendtTilUtbetaling!!.sendtTidspunkt,
                forsøkshistorikk = opprett(
                    forrigeForsøk = it.sendtTilUtbetaling.sendtTidspunkt.plus(1, ChronoUnit.MICROS),
                    antallForsøk = 1,
                    clock = fixedClock,
                ),
            )
        }
    }
}
