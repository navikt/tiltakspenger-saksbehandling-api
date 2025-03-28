package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkUtbetalingDTO

class StatistikkStønadFakeRepo : StatistikkStønadRepo {

    private val stønadsdata = Atomic(mutableMapOf<String, no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkStønadDTO>())
    private val utbetalingsdata = Atomic(mutableMapOf<String, no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkUtbetalingDTO>())

    override fun lagre(dto: no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkStønadDTO, context: TransactionContext?) {
        stønadsdata.get()[dto.id.toString()] = dto
    }

    override fun lagre(dto: no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkUtbetalingDTO, context: TransactionContext?) {
        utbetalingsdata.get()[dto.id] = dto
    }
}
