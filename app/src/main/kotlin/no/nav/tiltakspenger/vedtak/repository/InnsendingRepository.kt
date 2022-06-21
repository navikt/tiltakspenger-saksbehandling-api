package no.nav.tiltakspenger.vedtak.repository

import no.nav.tiltakspenger.vedtak.Innsending

interface InnsendingRepository {
    fun hent(journalpostId: String): Innsending?
    fun lagre(innsending: Innsending): Int
}