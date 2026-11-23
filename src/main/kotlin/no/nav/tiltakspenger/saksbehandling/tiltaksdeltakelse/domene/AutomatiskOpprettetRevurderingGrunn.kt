package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import java.time.LocalDateTime

/**
 * @param tiltaksdeltakerId Deltakelsen endringen gjelder.
 * Er null for revurderinger som ble opprettet før feltet ble lagt til.
 * @param lagtTil Når grunnen ble lagt til revurderingen.
 * For grunner som ble lagret før feltet ble lagt til, er det tidspunktet revurderingen ble opprettet.
 */
data class AutomatiskOpprettetRevurderingGrunn(
    val endring: TiltaksdeltakerEndring,
    val tiltaksdeltakerId: TiltaksdeltakerId?,
    val lagtTil: LocalDateTime,
)

/**
 * Grunnene til at systemet opprettet en revurdering, i rekkefølgen de ble lagt til.
 * Den første er grunnen revurderingen ble opprettet for.
 * De neste er endringer som har kommet mens revurderingen var en åpen ubestemt revurdering.
 * Se [no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering.leggTilAutomatiskOpprettetGrunn].
 */
data class AutomatiskOpprettedeRevurderingGrunner(
    val grunner: NonEmptyList<AutomatiskOpprettetRevurderingGrunn>,
) {
    constructor(grunn: AutomatiskOpprettetRevurderingGrunn) : this(nonEmptyListOf(grunn))

    val første: AutomatiskOpprettetRevurderingGrunn get() = grunner.head

    fun leggTil(grunn: AutomatiskOpprettetRevurderingGrunn): AutomatiskOpprettedeRevurderingGrunner =
        AutomatiskOpprettedeRevurderingGrunner(grunner + grunn)

    /** Den sist tillagte grunnen for deltakelsen, eller null dersom ingen av grunnene gjelder den. */
    fun sisteFor(tiltaksdeltakerId: TiltaksdeltakerId): AutomatiskOpprettetRevurderingGrunn? =
        grunner.lastOrNull { it.tiltaksdeltakerId == tiltaksdeltakerId }
}
