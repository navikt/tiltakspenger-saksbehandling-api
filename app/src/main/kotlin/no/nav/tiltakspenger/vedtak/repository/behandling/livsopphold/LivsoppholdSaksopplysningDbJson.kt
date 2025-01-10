package no.nav.tiltakspenger.vedtak.repository.behandling.livsopphold

import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LivsoppholdSaksopplysning
import no.nav.tiltakspenger.vedtak.repository.felles.PeriodeDbJson
import no.nav.tiltakspenger.vedtak.repository.felles.SaksbehandlerDbJson
import no.nav.tiltakspenger.vedtak.repository.felles.toDbJson
import java.time.LocalDateTime

internal data class LivsoppholdSaksopplysningDbJson(
    val harLivsoppholdYtelser: Boolean,
    val saksbehandler: SaksbehandlerDbJson?,
    val periode: PeriodeDbJson,
    val tidsstempel: String,
) {
    fun toDomain(): LivsoppholdSaksopplysning =
        when {
            saksbehandler != null -> {
                LivsoppholdSaksopplysning.Saksbehandler(
                    harLivsoppholdYtelser = harLivsoppholdYtelser,
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                    navIdent = saksbehandler.navIdent,
                    periode = periode.toDomain(),
                )
            }

            else -> {
                LivsoppholdSaksopplysning.Søknad(
                    harLivsoppholdYtelser = harLivsoppholdYtelser,
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                    periode = periode.toDomain(),
                )
            }
        }
}

internal fun LivsoppholdSaksopplysning.toDbJson(): LivsoppholdSaksopplysningDbJson =
    LivsoppholdSaksopplysningDbJson(
        harLivsoppholdYtelser = harLivsoppholdYtelser,
        saksbehandler = navIdent?.let { SaksbehandlerDbJson(it) },
        periode = periode.toDbJson(),
        tidsstempel = tidsstempel.toString(),
    )
