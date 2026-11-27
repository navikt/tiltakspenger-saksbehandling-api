package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.felles.Kafkametadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.RåUtbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskobling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringRepo
import java.time.LocalDateTime

class UtbetalingskvitteringFakeRepo : UtbetalingskvitteringRepo {
    private data class Rad(
        val kvittering: RåUtbetalingskvittering,
        val kafkametadata: Kafkametadata,
        val behandletFeil: String?,
        val forbigående: Boolean?,
    )

    private val data = Atomic(mutableListOf<Rad>())

    override fun lagreNy(
        key: String?,
        value: String?,
        kafkametadata: Kafkametadata,
        mottatt: LocalDateTime,
    ): Boolean {
        val rader = data.get()
        val finnes = rader.any {
            it.kafkametadata.topic == kafkametadata.topic &&
                it.kafkametadata.partisjon == kafkametadata.partisjon &&
                it.kafkametadata.offset == kafkametadata.offset
        }
        if (finnes) return false
        rader.add(
            Rad(
                kvittering = RåUtbetalingskvittering(
                    id = UtbetalingskvitteringId.random(),
                    key = key,
                    value = value,
                    mottatt = mottatt,
                    behandlet = null,
                    sakId = null,
                    utbetalingId = null,
                    forsøkshistorikk = null,
                ),
                kafkametadata = kafkametadata,
                behandletFeil = null,
                forbigående = null,
            ),
        )
        return true
    }

    override fun hentUbehandledeIder(nå: LocalDateTime, limit: Int): List<UtbetalingskvitteringId> {
        val rader = data.get()
        return rader
            .filter { it.kvittering.behandlet == null }
            .filter { rad ->
                rad.kvittering.key == null ||
                    rader.none { eldre ->
                        eldre.kvittering.key == rad.kvittering.key &&
                            eldre.kafkametadata.topic == rad.kafkametadata.topic &&
                            eldre.kafkametadata.partisjon == rad.kafkametadata.partisjon &&
                            eldre.kafkametadata.offset < rad.kafkametadata.offset &&
                            eldre.kvittering.behandlet == null
                    }
            }
            .filter { it.kvittering.forsøkshistorikk?.nesteForsøk?.let { due -> due <= nå } ?: true }
            .sortedWith(
                compareBy<Rad>({ it.kafkametadata.topic }, { it.kafkametadata.partisjon }, { it.kafkametadata.offset }),
            )
            .take(limit)
            .map { it.kvittering.id }
    }

    override fun hent(id: UtbetalingskvitteringId, context: TransactionContext): RåUtbetalingskvittering =
        data.get().first { it.kvittering.id == id }.kvittering

    override fun markerBehandlet(
        id: UtbetalingskvitteringId,
        behandlet: LocalDateTime,
        kobling: Utbetalingskobling?,
        context: TransactionContext,
    ) {
        data.get().replaceAll {
            if (it.kvittering.id == id) {
                it.copy(
                    kvittering = (kobling?.let(it.kvittering::knyttTil) ?: it.kvittering).markerBehandlet(behandlet),
                    behandletFeil = null,
                    forbigående = null,
                )
            } else {
                it
            }
        }
    }

    override fun lagreFeil(
        id: UtbetalingskvitteringId,
        feil: String,
        forbigående: Boolean,
        forsøkshistorikk: Forsøkshistorikk,
        kobling: Utbetalingskobling?,
    ) {
        data.get().replaceAll {
            if (it.kvittering.id == id) {
                it.copy(
                    kvittering = (kobling?.let(it.kvittering::knyttTil) ?: it.kvittering)
                        .copy(forsøkshistorikk = forsøkshistorikk),
                    behandletFeil = feil,
                    forbigående = forbigående,
                )
            } else {
                it
            }
        }
    }

    override fun antallPermanenteFeil(): Long =
        data.get().count { it.kvittering.behandlet == null && it.forbigående == false }.toLong()

    override fun antallForbigåendeFeil(eldreEnn: LocalDateTime): Long =
        data.get().count {
            it.kvittering.behandlet == null &&
                it.forbigående == true &&
                it.kvittering.mottatt <= eldreEnn
        }.toLong()
}
