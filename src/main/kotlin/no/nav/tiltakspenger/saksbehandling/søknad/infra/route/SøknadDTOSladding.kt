package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
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
    fornavn = SladdetVerdi,
    mellomnavn = SladdetVerdi,
    etternavn = SladdetVerdi,
    fødselsdato = SladdetVerdi,
    fnr = SladdetVerdi,
)

fun SøknadDTO.sladdetFor(saksbehandler: Saksbehandler): SøknadDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this
