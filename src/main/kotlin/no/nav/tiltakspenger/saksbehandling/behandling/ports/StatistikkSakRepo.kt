package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO

interface StatistikkSakRepo {
    fun lagre(dto: StatistikkSakDTO, context: TransactionContext? = null)
    fun oppdaterAdressebeskyttelse(sakId: SakId)
    fun hent(sakId: SakId): List<StatistikkSakDTO>
}
