package no.nav.tiltakspenger.vedtak.repository.behandling.kravfrist

import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kravfrist.KravfristSaksopplysning
import no.nav.tiltakspenger.vedtak.repository.felles.SaksbehandlerDbJson
import java.time.LocalDateTime

internal data class KravfristSaksopplysningDbJson(
    val kravdato: LocalDateTime,
    val saksbehandler: SaksbehandlerDbJson?,
    val tidsstempel: String,
) {
    fun toDomain(): KravfristSaksopplysning =
        when {
            saksbehandler != null -> {
                KravfristSaksopplysning.Saksbehandler(
                    kravdato = kravdato,
                    navIdent = saksbehandler.navIdent,
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                )
            }

            else -> {
                KravfristSaksopplysning.Søknad(
                    kravdato = kravdato,
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                )
            }
        }
}

internal fun KravfristSaksopplysning.toDbJson(): KravfristSaksopplysningDbJson =
    KravfristSaksopplysningDbJson(
        kravdato = kravdato,
        saksbehandler = navIdent?.let { SaksbehandlerDbJson(it) },
        tidsstempel = tidsstempel.toString(),
    )
