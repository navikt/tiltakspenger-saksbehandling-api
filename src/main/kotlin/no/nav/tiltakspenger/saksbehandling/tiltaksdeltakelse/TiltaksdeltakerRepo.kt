package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import java.time.LocalDateTime

interface TiltaksdeltakerRepo {
    fun hentEllerLagre(
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakTypeDTO,
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): TiltaksdeltakerId

    // denne er primært tenkt brukt for testformål
    fun lagre(
        id: TiltaksdeltakerId,
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakTypeDTO,
        sakId: SakId,
        sessionContext: SessionContext? = null,
    )

    fun hentInternId(eksternId: String): TiltaksdeltakerId?

    fun hentEksternId(
        id: TiltaksdeltakerId,
        sessionContext: SessionContext?,
    ): String

    fun hentTiltaksdeltaker(eksternId: String): Tiltaksdeltaker?

    // Denne skal kun brukes når tiltaksdeltakelser flyttes ut av Arena og får ny eksternId
    fun oppdaterEksternIdForTiltaksdeltaker(
        tiltaksdeltaker: Tiltaksdeltaker,
        sessionContext: SessionContext? = null,
    )

    /**
     * Kalles av kafka-consumerne når de mottar en hendelse for deltakeren.
     * Setter [Tiltaksdeltaker.sakId] og [Tiltaksdeltaker.sisteUbehandletEndringTidspunkt], som plukkes opp av [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb].
     */
    fun registrerUbehandletEndring(
        id: TiltaksdeltakerId,
        sakId: SakId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext? = null,
    )

    /**
     * Deltakere med en ubehandlet endring eldre enn [eldreEnn] — forsinkelsen er for å samle opp hendelser som kommer tett etter hverandre.
     */
    fun hentMedUbehandledeEndringer(eldreEnn: LocalDateTime): List<Tiltaksdeltaker>

    /**
     * Nullstiller [Tiltaksdeltaker.sisteUbehandletEndringTidspunkt] etter at endringen er behandlet.
     * Nullstiller kun dersom markøren fortsatt er [forventetSisteUbehandletEndring] — har det kommet en nyere hendelse i mellomtiden, står den igjen til neste kjøring.
     */
    fun markerEndringSomBehandlet(
        id: TiltaksdeltakerId,
        forventetSisteUbehandletEndring: LocalDateTime,
        sessionContext: SessionContext? = null,
    )
}
