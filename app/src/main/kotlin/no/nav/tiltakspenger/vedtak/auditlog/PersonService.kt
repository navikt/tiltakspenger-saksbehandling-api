package no.nav.tiltakspenger.vedtak.auditlog

import no.nav.tiltakspenger.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.vedtak.repository.person.PersonRepo

class PersonService(
    private val personRepo: PersonRepo,
) {

    fun hentFnrForBehandlingId(behandlingId: BehandlingId): Fnr =
        personRepo.hentFnrForBehandlingId(behandlingId)
            ?: throw IkkeFunnetException("Fant ikke fnr på behandlingId: $behandlingId")

    fun hentFnrForSakId(sakId: SakId): Fnr =
        personRepo.hentFnrForSakId(sakId)
            ?: throw IkkeFunnetException("Fant ikke fnr på sakId: $sakId")

    fun hentFnrForSaksnummer(saksnummer: Saksnummer): Fnr =
        personRepo.hentFnrForSaksnummer(saksnummer)
            ?: throw IkkeFunnetException("Fant ikke fnr for saksnummer: $saksnummer")

    fun hentFnrForVedtakId(vedtakId: VedtakId): Fnr =
        personRepo.hentFnrForVedtakId(vedtakId)
            ?: throw IkkeFunnetException("Fant ikke fnr for vedtakId: $vedtakId")

    fun hentFnrForMeldekortId(meldekortId: MeldekortId): Fnr =
        personRepo.hentFnrForMeldekortId(meldekortId)
            ?: throw IkkeFunnetException("Fant ikke fnr på meldekortId: $meldekortId")

    fun hentFnrForSøknadId(søknadId: SøknadId): Fnr =
        personRepo.hentFnrForSøknadId(søknadId)
            ?: throw IkkeFunnetException("Fant ikke fnr på søknadId: søknadId")
}
