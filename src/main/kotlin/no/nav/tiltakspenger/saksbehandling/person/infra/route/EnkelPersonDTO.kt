package no.nav.tiltakspenger.saksbehandling.person.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.person.BarnMedSkjerming
import no.nav.tiltakspenger.saksbehandling.person.EnkelPersonMedSkjerming
import java.time.LocalDate

data class EnkelPersonDTO(
    val fnr: SladdbarVerdi<String>,
    val fødselsdato: SladdbarVerdi<LocalDate>,
    val fornavn: SladdbarVerdi<String?>,
    val mellomnavn: SladdbarVerdi<String?>,
    val etternavn: SladdbarVerdi<String?>,
    val fortrolig: Boolean,
    val strengtFortrolig: Boolean,
    val strengtFortroligUtland: Boolean,
    val skjermet: Boolean,
    val dødsdato: SladdbarVerdi<LocalDate?>,
)

fun EnkelPersonMedSkjerming.toEnkelPersonDTO(): EnkelPersonDTO = EnkelPersonDTO(
    fnr = fnr.verdi.ikkeSladdet(),
    fødselsdato = fødselsdato.ikkeSladdet(),
    fornavn = fornavn.ikkeSladdet(),
    mellomnavn = mellomnavn.ikkeSladdet(),
    etternavn = etternavn.ikkeSladdet(),
    fortrolig = fortrolig,
    strengtFortrolig = strengtFortrolig,
    strengtFortroligUtland = strengtFortroligUtland,
    skjermet = skjermet,
    dødsdato = dødsdato.ikkeSladdet(),
)

fun BarnMedSkjerming.toEnkelPersonDTO(): EnkelPersonDTO = EnkelPersonDTO(
    fnr = fnr.verdi.ikkeSladdet(),
    fødselsdato = fødselsdato.ikkeSladdet(),
    fornavn = fornavn.ikkeSladdet(),
    mellomnavn = mellomnavn.ikkeSladdet(),
    etternavn = etternavn.ikkeSladdet(),
    fortrolig = fortrolig,
    strengtFortrolig = strengtFortrolig,
    strengtFortroligUtland = strengtFortroligUtland,
    skjermet = skjermet,
    dødsdato = dødsdato.ikkeSladdet(),
)
