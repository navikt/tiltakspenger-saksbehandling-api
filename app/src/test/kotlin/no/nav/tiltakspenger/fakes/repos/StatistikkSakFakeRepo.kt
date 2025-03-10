package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.vedtak.saksbehandling.service.statistikk.sak.StatistikkSakDTO

class StatistikkSakFakeRepo : StatistikkSakRepo {
    private val data = Atomic(mutableMapOf<SakId, StatistikkSakDTO>())

    override fun lagre(dto: StatistikkSakDTO, context: TransactionContext?) {
        data.get()[SakId.fromString(dto.sakId)] = dto
    }
}
