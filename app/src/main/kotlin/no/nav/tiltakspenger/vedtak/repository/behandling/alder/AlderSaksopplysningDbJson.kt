package no.nav.tiltakspenger.vedtak.repository.behandling.alder

import no.nav.tiltakspenger.saksbehandling.domene.vilkår.alder.AlderSaksopplysning
import no.nav.tiltakspenger.vedtak.repository.behandling.felles.toDbType
import no.nav.tiltakspenger.vedtak.repository.behandling.felles.ÅrsakTilEndringDbType
import no.nav.tiltakspenger.vedtak.repository.felles.SaksbehandlerDbJson
import java.time.LocalDate
import java.time.LocalDateTime

internal data class AlderSaksopplysningDbJson(
    val fødselsdato: LocalDate,
    val årsakTilEndring: ÅrsakTilEndringDbType?,
    val saksbehandler: SaksbehandlerDbJson?,
    val tidsstempel: String,
) {
    fun toDomain(): AlderSaksopplysning =
        when {
            saksbehandler != null -> {
                checkNotNull(årsakTilEndring) { "Årsak til endring er ikke satt for aldersaksopplysning fra saksbehandler." }
                AlderSaksopplysning.Saksbehandler(
                    fødselsdato = fødselsdato,
                    årsakTilEndring = årsakTilEndring.toDomain(),
                    navIdent = saksbehandler.navIdent,
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                )
            }

            else -> {
                require(årsakTilEndring == null) { "Støtter ikke årsak til endring for AlderSaksopplysning.Personopplysning." }
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
        årsakTilEndring = årsakTilEndring?.toDbType(),
        saksbehandler = navIdent?.let { SaksbehandlerDbJson(navIdent = it) },
        tidsstempel = tidsstempel.toString(),
    )
