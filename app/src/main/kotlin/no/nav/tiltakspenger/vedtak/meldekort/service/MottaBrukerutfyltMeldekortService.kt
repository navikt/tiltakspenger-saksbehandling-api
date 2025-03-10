package no.nav.tiltakspenger.vedtak.meldekort.service

import no.nav.tiltakspenger.vedtak.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.vedtak.meldekort.domene.NyttBrukersMeldekort

class MottaBrukerutfyltMeldekortService(
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
) {
    fun mottaBrukerutfyltMeldekort(brukersMeldekort: NyttBrukersMeldekort) {
        // Dette meldekortet er validert og lagret av meldekort-api, så vi antar at det er gyldig.
        brukersMeldekortRepo.lagre(brukersMeldekort)
    }
}
