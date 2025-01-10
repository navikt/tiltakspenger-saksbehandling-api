package no.nav.tiltakspenger.vedtak.repository.behandling.alder

import no.nav.tiltakspenger.saksbehandling.domene.vilkår.alder.AlderSaksopplysning
import no.nav.tiltakspenger.vedtak.repository.felles.SaksbehandlerDbJson
import java.time.LocalDate
import java.time.LocalDateTime

internal data class AlderSaksopplysningDbJson(
    val fødselsdato: LocalDate,
    val saksbehandler: SaksbehandlerDbJson?,
    val tidsstempel: String,
) {
    fun toDomain(): AlderSaksopplysning =
        when {
            saksbehandler != null -> {
                AlderSaksopplysning.Saksbehandler(
                    fødselsdato = fødselsdato,
                    navIdent = saksbehandler.navIdent,
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                )
            }

            else -> {
                AlderSaksopplysning.Register(
                    fødselsdato = fødselsdato,
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                )
            }
        }
}

internal fun AlderSaksopplysning.toDbJson(): AlderSaksopplysningDbJson =
    AlderSaksopplysningDbJson(
        fødselsdato = fødselsdato,
        saksbehandler = navIdent?.let { SaksbehandlerDbJson(navIdent = it) },
        tidsstempel = tidsstempel.toString(),
    )
