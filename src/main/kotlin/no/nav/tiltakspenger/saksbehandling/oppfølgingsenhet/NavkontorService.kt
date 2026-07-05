package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.logging.Sikkerlogg

class NavkontorService(
    private val veilarboppfolgingKlient: VeilarboppfolgingKlient,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Returnerer [Navkontor] for bakoverkompatibilitet. Klienten returnerer rik metadata
     * ([NavkontorMedMetadata]) som vi senere vil ønske å lagre - se [hentOppfolgingsenhetMedMetadata].
     * Kaster [IllegalStateException] dersom klienten ikke klarte å hente navkontor.
     */
    suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
        sakId: String? = null,
        saksnummer: String? = null,
        rammebehandlingId: String? = null,
        meldekortbehandlingId: String? = null,
    ): Navkontor = hentOppfolgingsenhetMedMetadata(
        fnr = fnr,
        sakId = sakId,
        saksnummer = saksnummer,
        rammebehandlingId = rammebehandlingId,
        meldekortbehandlingId = meldekortbehandlingId,
    ).fold(
        ifLeft = { error ->
            val kontekst = "sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $rammebehandlingId, meldekortbehandlingId: $meldekortbehandlingId"
            logger.error { "Feil ved henting av oppfølgingsenhet. $kontekst. Feiltype=${error.beskrivelse()}. Se sikkerlogg for mer informasjon." }
            Sikkerlogg.error {
                "Feil ved henting av oppfølgingsenhet. $kontekst. request=${error.veilarboppfolgingKall?.request}, response=${error.veilarboppfolgingKall?.response}, httpStatus=${error.veilarboppfolgingKall?.httpStatus}."
            }
            error("Kunne ikke hente navkontor: $error")
        },
        ifRight = { it.navkontor },
    )

    suspend fun hentOppfolgingsenhetMedMetadata(
        fnr: Fnr,
        sakId: String? = null,
        saksnummer: String? = null,
        rammebehandlingId: String? = null,
        meldekortbehandlingId: String? = null,
    ) = veilarboppfolgingKlient.hentOppfolgingsenhet(
        fnr = fnr,
        sakId = sakId,
        saksnummer = saksnummer,
        rammebehandlingId = rammebehandlingId,
        meldekortbehandlingId = meldekortbehandlingId,
    )
}

private fun KanIkkeHenteOppfølgingsenhet.beskrivelse(): String = when (this) {
    is KanIkkeHenteOppfølgingsenhet.KallFeilet -> "KallFeilet"
    is KanIkkeHenteOppfølgingsenhet.UventetHttpStatus -> "UventetHttpStatus(status=$status)"
    is KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet -> "ManglerOppfolgingsenhet"
}
