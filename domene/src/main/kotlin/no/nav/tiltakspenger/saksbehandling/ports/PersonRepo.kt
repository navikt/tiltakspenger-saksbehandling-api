package no.nav.tiltakspenger.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer

/*
    Dette repoet brukes av auditloggeren
 */
interface PersonRepo {
    fun hentFnrForSakId(sakId: SakId): Fnr?
    fun hentFnrForBehandlingId(behandlingId: BehandlingId): Fnr?
    fun hentFnrForSaksnummer(saksnummer: Saksnummer): Fnr?
    fun hentFnrForVedtakId(vedtakId: VedtakId): Fnr?
    fun hentFnrForMeldekortId(meldekortId: MeldekortId): Fnr?
    fun hentFnrForSøknadId(søknadId: SøknadId): Fnr?
}
