package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.tiltakdeltagelse

import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde.Arena
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde.Komet
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse.TiltaksdeltagelseSaksopplysning
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO

internal data class TiltakDeltagelseSaksopplysningDTO(
    val tiltakNavn: String,
    val deltagelsePeriode: PeriodeDTO,
    val status: String,
    val kilde: TiltakKildeDTO,
)

internal fun TiltaksdeltagelseSaksopplysning.toDTO(): TiltakDeltagelseSaksopplysningDTO =
    TiltakDeltagelseSaksopplysningDTO(
        tiltakNavn = tiltaksnavn,
        kilde =
        when (kilde) {
            Arena -> TiltakKildeDTO.ARENA
            Komet -> TiltakKildeDTO.KOMET
        },
        deltagelsePeriode = deltagelsePeriode.toDTO(),
        status = status.toDTO(),
    )
