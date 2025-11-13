package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.jobb.MinimalStatistikkSakDTO

interface StatistikkSakRepo {
    fun lagre(dto: StatistikkSakDTO, context: TransactionContext? = null)
    fun oppdaterAdressebeskyttelse(sakId: SakId)
    fun hent(sakId: SakId): List<StatistikkSakDTO>
    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, context: TransactionContext? = null)
    fun hentAlleMedFeilEndrettidspunkt(limit: Int = 50): List<MinimalStatistikkSakDTO>
    fun oppdaterEndretTidspunkt(minimalStatistikkSakDTO: MinimalStatistikkSakDTO, context: TransactionContext? = null)
}
