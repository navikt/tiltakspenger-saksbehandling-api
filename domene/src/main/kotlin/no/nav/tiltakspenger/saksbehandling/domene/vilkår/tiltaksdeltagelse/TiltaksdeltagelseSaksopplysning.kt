package no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.UtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.ÅrsakTilEndring
import java.time.LocalDateTime

sealed interface TiltaksdeltagelseSaksopplysning {
    val tiltaksnavn: String
    val eksternDeltagelseId: String
    val gjennomføringId: String?
    val kilde: Tiltakskilde
    val utfallForPeriode: UtfallForPeriode

    // TODO jah: Bør endres til en periodisering som de andre saksopplysningene på de andre vilkårene. Man kan gjenbruke den periodiserte datatypen på tvers av saksopplysningene.
    val deltagelsePeriode: Periode
    val girRett: Boolean
    val status: TiltakDeltakerstatus
    val tidsstempel: LocalDateTime
    val tiltakstype: TiltakstypeSomGirRett
    val årsakTilEndring: ÅrsakTilEndring?
    val navIdent: String?

    fun oppdaterPeriode(periode: Periode): TiltaksdeltagelseSaksopplysning

    data class Register(
        override val tiltaksnavn: String,
        override val eksternDeltagelseId: String,
        override val gjennomføringId: String?,
        override val tidsstempel: LocalDateTime,
        override val deltagelsePeriode: Periode,
        override val girRett: Boolean,
        override val status: TiltakDeltakerstatus,
        override val kilde: Tiltakskilde,
        override val tiltakstype: TiltakstypeSomGirRett,
    ) : TiltaksdeltagelseSaksopplysning {
        override val årsakTilEndring = null
        override val navIdent = null

        /** Støtter i førsteomgang kun å krympe perioden. Dersom man skulle utvidet den, måtte man gjort en ny vurdering og ville derfor hatt en ny saksopplysning. */
        override fun oppdaterPeriode(periode: Periode): Register {
            return copy(deltagelsePeriode = periode)
        }

        // TODO jah: Vi beholder bare dagens logikk her, men vi må endre på denne når vi skal støtte periodisering ved førstegangsbehandling eller registersaksopplysning ved revurdering.
        override val utfallForPeriode = UtfallForPeriode.OPPFYLT
    }

    data class Saksbehandler(
        override val tiltaksnavn: String,
        override val eksternDeltagelseId: String,
        override val gjennomføringId: String?,
        override val tidsstempel: LocalDateTime,
        override val deltagelsePeriode: Periode,
        override val girRett: Boolean,
        override val status: TiltakDeltakerstatus,
        override val kilde: Tiltakskilde,
        override val tiltakstype: TiltakstypeSomGirRett,
        override val navIdent: String,
        override val årsakTilEndring: ÅrsakTilEndring?,
    ) : TiltaksdeltagelseSaksopplysning {

        // TODO jah: Per tidspunkt tvinger vi denne til å være HarSluttet dersom saksbehandler skal endre status. På sikt må vi ha samme logikk på tvers av opplysningstypene.
        override val utfallForPeriode: UtfallForPeriode = UtfallForPeriode.IKKE_OPPFYLT

        override fun oppdaterPeriode(periode: Periode): Saksbehandler {
            return copy(deltagelsePeriode = periode)
        }

        init {
            require(status == TiltakDeltakerstatus.HarSluttet) {
                "TODO jah: Dette er en forenkling i MVP av saksopplysningene. Vi støtter kun endring av periode ved sluttet status."
            }
        }
    }
}
