package no.nav.tiltakspenger.fakes.repos

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.ports.PersonRepo

class PersonFakeRepo(
    private val sakFakeRepo: SakFakeRepo,
    private val søknadFakeRepo: SøknadFakeRepo,
    private val meldekortBehandlingFakeRepo: MeldekortBehandlingFakeRepo,
    private val behandlingFakeRepo: BehandlingFakeRepo,
) : PersonRepo {

    override fun hentFnrForSakId(sakId: SakId): Fnr {
        return sakFakeRepo.data.get()[sakId]!!.fnr
    }

    override fun hentFnrForBehandlingId(behandlingId: BehandlingId): Fnr? {
        return behandlingFakeRepo.hentOrNull(behandlingId)?.fnr
    }

    override fun hentFnrForSaksnummer(saksnummer: Saksnummer): Fnr? {
        return sakFakeRepo.hentFnrForSaksnummer(saksnummer)
    }

    override fun hentFnrForMeldekortId(meldekortId: MeldekortId): Fnr? {
        return meldekortBehandlingFakeRepo.hentFnrForMeldekortId(meldekortId)
    }

    override fun hentFnrForSøknadId(søknadId: SøknadId): Fnr {
        return søknadFakeRepo.hentForSøknadId(søknadId).fnr
    }
}
