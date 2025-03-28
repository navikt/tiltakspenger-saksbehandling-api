package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO

interface StatistikkSakRepo {
    fun lagre(dto: no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO, context: TransactionContext? = null)
}
