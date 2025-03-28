package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO

class StatistikkSakFakeRepo : StatistikkSakRepo {
    private val data = Atomic(mutableMapOf<SakId, no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO>())

    override fun lagre(dto: no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO, context: TransactionContext?) {
        data.get()[SakId.fromString(dto.sakId)] = dto
    }
}
