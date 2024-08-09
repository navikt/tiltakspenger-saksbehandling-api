package no.nav.tiltakspenger.vedtak.routes.behandling.stønadsdager

import no.nav.tiltakspenger.saksbehandling.domene.stønadsdager.StønadsdagerSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde.Arena
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde.Komet
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.tiltakdeltagelse.TiltakKildeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO

internal data class StønadsdagerSaksopplysningDTO(
    val tiltakNavn: String,
    val antallDager: Int,
    val periode: PeriodeDTO,
    val kilde: TiltakKildeDTO,
)

internal fun StønadsdagerSaksopplysning.toDTO(): StønadsdagerSaksopplysningDTO =
    StønadsdagerSaksopplysningDTO(
        tiltakNavn = tiltakNavn,
        antallDager = antallDager,
        kilde =
        when (kilde) {
            Arena -> TiltakKildeDTO.ARENA
            Komet -> TiltakKildeDTO.KOMET
        },
        periode = periode.toDTO(),
    )
