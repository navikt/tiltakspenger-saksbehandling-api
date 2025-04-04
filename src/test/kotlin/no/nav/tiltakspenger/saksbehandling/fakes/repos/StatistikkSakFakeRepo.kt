package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO

class StatistikkSakFakeRepo : StatistikkSakRepo {
    private val data = Atomic(mutableMapOf<SakId, StatistikkSakDTO>())

    override fun lagre(dto: StatistikkSakDTO, context: TransactionContext?) {
        data.get()[SakId.fromString(dto.sakId)] = dto
    }

    override fun oppdaterAdressebeskyttelse(sakId: SakId) {
        val statistikkSak = data.get()[sakId]
        statistikkSak?.let {
            data.get()[sakId] = it.copy(
                opprettetAv = "-5",
                saksbehandler = "-5",
                ansvarligBeslutter = "-5",
            )
        }
    }

    override fun hent(sakId: SakId): List<StatistikkSakDTO> {
        return data.get()[sakId]?.let { listOf(it) } ?: emptyList()
    }
}
