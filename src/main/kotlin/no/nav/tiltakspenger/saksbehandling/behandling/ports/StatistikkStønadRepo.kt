package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkUtbetalingDTO

interface StatistikkStønadRepo {
    fun lagre(dto: no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkStønadDTO, context: TransactionContext? = null)
    fun lagre(dto: no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkUtbetalingDTO, context: TransactionContext? = null)
}
