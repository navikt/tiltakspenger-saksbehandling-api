package no.nav.tiltakspenger.vedtak.repository.behandling.tiltakDeltagelse

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse.TiltaksdeltagelseSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse.TiltaksdeltagelseVilkår
import no.nav.tiltakspenger.vedtak.repository.behandling.felles.PeriodisertUtfallDbJson
import no.nav.tiltakspenger.vedtak.repository.behandling.felles.toDbJson
import no.nav.tiltakspenger.vedtak.repository.behandling.felles.toDomain

/**
 * Har ansvar for å serialisere/deserialisere TiltakDeltagelseVilkår til og fra json for lagring i database.
 */
internal data class TiltakDeltagelseVilkårDbJson(
    val registerSaksopplysning: TiltakDeltagelseSaksopplysningDbJson,
    val saksbehandlerSaksopplysning: TiltakDeltagelseSaksopplysningDbJson?,
    val avklartSaksopplysning: TiltakDeltagelseSaksopplysningDbJson?,
    val utfallsperioder: List<PeriodisertUtfallDbJson>,
) {
    fun toDomain(vurderingsperiode: Periode): TiltaksdeltagelseVilkår =
        TiltaksdeltagelseVilkår.fromDb(
            registerSaksopplysning = registerSaksopplysning.toDomain() as TiltaksdeltagelseSaksopplysning.Register,
            saksbehandlerSaksopplysning = saksbehandlerSaksopplysning?.toDomain()
                ?.let { it as TiltaksdeltagelseSaksopplysning.Saksbehandler },
            avklartSaksopplysning = avklartSaksopplysning?.toDomain() ?: registerSaksopplysning.toDomain(),
            vurderingsperiode = vurderingsperiode,
            utfall = utfallsperioder.toDomain(),
        )
}

internal fun TiltaksdeltagelseVilkår.toDbJson(): TiltakDeltagelseVilkårDbJson =
    TiltakDeltagelseVilkårDbJson(
        registerSaksopplysning = registerSaksopplysning.toDbJson(),
        utfallsperioder = utfall.toDbJson(),
        avklartSaksopplysning = avklartSaksopplysning.toDbJson(),
        saksbehandlerSaksopplysning = saksbehandlerSaksopplysning?.toDbJson(),
    )
