@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.statistikk.vedtak

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo

class StatistikkStønadFakeRepo : StatistikkStønadRepo {

    private val stønadsdata = Atomic(mutableMapOf<String, StatistikkStønadDTO>())
    private val utbetalingsdata = Atomic(mutableMapOf<String, StatistikkUtbetalingDTO>())

    override fun lagre(dto: StatistikkStønadDTO, context: TransactionContext?) {
        stønadsdata.get()[dto.id.toString()] = dto
    }

    override fun lagre(dto: StatistikkUtbetalingDTO, context: TransactionContext?) {
        utbetalingsdata.get()[dto.id] = dto
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        val statistikkStønadDTO = stønadsdata.get().values.find { it.brukerId == gammeltFnr.verdi }
        statistikkStønadDTO?.let {
            stønadsdata.get()[it.sakId] = it.copy(
                brukerId = nyttFnr.verdi,
            )
        }
    }

    override fun hent(sakId: SakId): List<StatistikkStønadDTO> {
        return stønadsdata.get()[sakId.toString()]?.let { listOf(it) } ?: emptyList()
    }
}
