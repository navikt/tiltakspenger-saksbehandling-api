package no.nav.tiltakspenger.vedtak.innsending

import no.nav.tiltakspenger.domene.personopplysninger.Personopplysninger
import java.time.LocalDateTime

data class InnhentedePersonopplysninger(
    val personopplysningerliste: List<Personopplysninger>,
    val tidsstempelInnhentet: LocalDateTime,
    val tidsstempelSkjermingInnhentet: LocalDateTime? = null,
)
