package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.stønad.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.stønad.StatistikkUtbetalingDTO

class StatistikkStønadFakeRepo : StatistikkStønadRepo {

    private val stønadsdata = Atomic(mutableMapOf<String, StatistikkStønadDTO>())
    private val utbetalingsdata = Atomic(mutableMapOf<String, StatistikkUtbetalingDTO>())

    override fun lagre(dto: StatistikkStønadDTO, context: TransactionContext?) {
        stønadsdata.get()[dto.id.toString()] = dto
    }

    override fun lagre(dto: StatistikkUtbetalingDTO, context: TransactionContext?) {
        utbetalingsdata.get()[dto.id] = dto
    }
}
