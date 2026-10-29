package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdet

/**
 * Sladding av [SøknadDTO].
 * Barna i barnetillegget er identifiserte personer og sladdes på linje med bruker selv.
 * Begrunnelsen for å avbryte eller gjenåpne søknaden er fritekst og sladdes.
 * Tiltaket, svarene i søknaden og journalpost-ID-en er ikke personopplysninger og beholdes.
 */

fun SøknadDTO.sladdet(): SøknadDTO = this.copy(
    barnetillegg = barnetillegg.map { it.sladdet() },
    avbrutt = avbrutt.map { it.sladdet() },
)

fun SøknadshendelseDTO.sladdet(): SøknadshendelseDTO = this.copy(
    begrunnelse = SladdetVerdi,
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
