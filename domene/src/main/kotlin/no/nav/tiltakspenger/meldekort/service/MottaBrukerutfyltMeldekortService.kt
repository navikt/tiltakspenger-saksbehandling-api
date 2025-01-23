package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.meldekort.domene.MeldekortBrukerRepo
import no.nav.tiltakspenger.meldekort.domene.NyttBrukersMeldekort

class MottaBrukerutfyltMeldekortService(
    private val meldekortBrukerRepo: MeldekortBrukerRepo,
) {
    fun mottaBrukerutfyltMeldekort(brukersMeldekort: NyttBrukersMeldekort) {
        // Dette meldekortet er validert og lagret av meldekort-api, så vi antar at det er gyldig.
        meldekortBrukerRepo.lagre(brukersMeldekort)
    }
}
