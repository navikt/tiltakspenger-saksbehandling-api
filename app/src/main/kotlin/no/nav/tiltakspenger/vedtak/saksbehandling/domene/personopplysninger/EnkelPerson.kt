package no.nav.tiltakspenger.vedtak.saksbehandling.domene.personopplysninger

import no.nav.tiltakspenger.libs.common.Fnr

data class EnkelPerson(
    val fnr: Fnr,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fortrolig: Boolean,
    val strengtFortrolig: Boolean,
    val strengtFortroligUtland: Boolean,
)
