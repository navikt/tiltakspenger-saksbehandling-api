package no.nav.tiltakspenger.vedtak.repository.statistikk.stønad

import kotliquery.TransactionalSession
import no.nav.tiltakspenger.saksbehandling.service.statistikk.stønad.StatistikkStønadDTO

interface StatistikkStønadDAO {
    fun lagre(dto: StatistikkStønadDTO, tx: TransactionalSession)
}
