package no.nav.tiltakspenger.saksbehandling.person.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor

/**
 * Sladding av [EnkelPersonDTO].
 * Identiteten til personen erstattes, mens flaggene for adressebeskyttelse og skjerming beholdes.
 * Frontenden bruker flaggene til å styre visningen, og de sier ingenting om hvem personen er.
 */

fun EnkelPersonDTO.sladdet(): EnkelPersonDTO = this.copy(
    fnr = SladdetVerdi,
    fødselsdato = SladdetVerdi,
    fornavn = SladdetVerdi,
    mellomnavn = SladdetVerdi,
    etternavn = SladdetVerdi,
    dødsdato = SladdetVerdi,
)

fun EnkelPersonDTO.sladdetFor(saksbehandler: Saksbehandler): EnkelPersonDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this

fun List<EnkelPersonDTO>.sladdetFor(saksbehandler: Saksbehandler): List<EnkelPersonDTO> =
    if (skalSladdeFor(saksbehandler)) this.map { it.sladdet() } else this
