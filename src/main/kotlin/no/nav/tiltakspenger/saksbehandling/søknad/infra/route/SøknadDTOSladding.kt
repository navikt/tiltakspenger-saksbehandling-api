package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.SLADDET_TEKST
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdet

/**
 * Sladding av [SøknadDTO].
 * Barna i barnetillegget er identifiserte personer og sladdes på linje med bruker selv.
 * Tiltaket, svarene i søknaden og journalpost-ID-en er ikke personopplysninger og beholdes.
 */

fun SøknadDTO.sladdet(): SøknadDTO = this.copy(
    barnetillegg = barnetillegg.map { it.sladdet() },
    avbrutt = avbrutt?.sladdet(),
)

fun SøknadDTO.BarnetilleggFraSøknadDTO.sladdet(): SøknadDTO.BarnetilleggFraSøknadDTO = this.copy(
    fornavn = fornavn?.let { SLADDET_TEKST },
    mellomnavn = mellomnavn?.let { SLADDET_TEKST },
    etternavn = etternavn?.let { SLADDET_TEKST },
    fødselsdato = SLADDET_TEKST,
    fnr = fnr?.let { SLADDET_TEKST },
)

fun SøknadDTO.sladdetFor(saksbehandler: Saksbehandler): SøknadDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this
