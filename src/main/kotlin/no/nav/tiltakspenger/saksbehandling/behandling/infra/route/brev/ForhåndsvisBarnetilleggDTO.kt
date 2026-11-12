package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.ValgtBarnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.OppdaterBarnMedBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.OppdaterBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Barnetillegget slik frontenden sender det til forhåndsvisning av vedtaksbrev.
 *
 * Frontenden sender enten [barn] eller [perioder], ikke begge:
 * - [barn]: barna saksbehandler har valgt, som ved lagring, se [OppdaterBarnetilleggDTO].
 *   Tom liste betyr ingen barnetillegg.
 * - [perioder]: antall barn per periode, for behandlinger med [BarnetilleggV1] som ikke kan endres.
 *   Tom liste betyr ingen barnetillegg.
 */
data class ForhåndsvisBarnetilleggDTO(
    val barn: List<OppdaterBarnMedBarnetilleggDTO>? = null,
    val perioder: List<BarnetilleggPeriodeDTO>? = null,
    val begrunnelse: String?,
) {
    init {
        require((barn == null) != (perioder == null)) { "Barnetillegget må ha enten barn eller perioder" }
    }

    fun tilValgtBarnetillegg(): ValgtBarnetillegg {
        if (barn != null) {
            return OppdaterBarnetilleggDTO(barn = barn, begrunnelse = begrunnelse).tilValgtBarnetillegg()
        }
        return ValgtBarnetillegg.PerioderMedAntallBarn(
            perioderMedBarn = perioder!!.map { it.periode.toDomain() to AntallBarn(it.antallBarn) },
            begrunnelse = begrunnelse?.let { Begrunnelse.create(it) },
        )
    }
}
