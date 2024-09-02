package no.nav.tiltakspenger.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak

interface RammevedtakRepo {
    fun hent(vedtakId: VedtakId): Rammevedtak?

    fun hentVedtakForBehandling(behandlingId: BehandlingId): Rammevedtak

    fun lagreVedtak(
        vedtak: Rammevedtak,
        context: TransactionContext? = null,
    ): Rammevedtak

    fun hentVedtakSomIkkeErSendtTilMeldekort(limit: Int = 10): List<Rammevedtak>

    fun oppdaterVedtakSendtTilMeldekort(id: VedtakId)

    fun hentVedtakForIdent(ident: Fnr): List<Rammevedtak>
}