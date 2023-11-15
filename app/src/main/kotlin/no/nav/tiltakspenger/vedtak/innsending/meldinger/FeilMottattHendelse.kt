package no.nav.tiltakspenger.vedtak.innsending.meldinger

import no.nav.tiltakspenger.vedtak.innsending.Aktivitetslogg
import no.nav.tiltakspenger.vedtak.innsending.Feil
import no.nav.tiltakspenger.vedtak.innsending.ISøkerHendelse
import no.nav.tiltakspenger.vedtak.innsending.InnsendingHendelse

class FeilMottattHendelse(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val ident: String,
    private val feil: Feil,
) : InnsendingHendelse(aktivitetslogg), ISøkerHendelse {

    override fun journalpostId() = journalpostId
    override fun ident() = ident

    fun feil() = feil
}