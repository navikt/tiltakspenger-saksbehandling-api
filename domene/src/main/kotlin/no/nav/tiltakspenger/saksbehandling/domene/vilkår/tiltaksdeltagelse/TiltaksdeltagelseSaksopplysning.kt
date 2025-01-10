package no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
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
    val utfallForPeriode: Periodisering<UtfallForPeriode>
    val status: TiltakDeltakerstatus
    val deltagelsePeriode: Periode
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
        override val status: TiltakDeltakerstatus,
        override val deltagelsePeriode: Periode,
        override val kilde: Tiltakskilde,
        override val tiltakstype: TiltakstypeSomGirRett,
    ) : TiltaksdeltagelseSaksopplysning {
        override val årsakTilEndring = null
        override val navIdent = null

        /** Støtter i førsteomgang kun å krympe perioden. Dersom man skulle utvidet den, måtte man gjort en ny vurdering og ville derfor hatt en ny saksopplysning. */
        override fun oppdaterPeriode(periode: Periode): Register {
            return copy(deltagelsePeriode = periode)
        }

        override val utfallForPeriode: Periodisering<UtfallForPeriode> = run {
            val utfall =
                when (status) {
                    TiltakDeltakerstatus.Avbrutt,
                    TiltakDeltakerstatus.Fullført,
                    TiltakDeltakerstatus.HarSluttet,
                    TiltakDeltakerstatus.Deltar,
                    -> UtfallForPeriode.OPPFYLT
                    TiltakDeltakerstatus.IkkeAktuell,
                    TiltakDeltakerstatus.Feilregistrert,
                    -> UtfallForPeriode.IKKE_OPPFYLT
                    TiltakDeltakerstatus.VenterPåOppstart,
                    TiltakDeltakerstatus.PåbegyntRegistrering,
                    TiltakDeltakerstatus.SøktInn,
                    TiltakDeltakerstatus.Venteliste,
                    TiltakDeltakerstatus.Vurderes,
                    -> UtfallForPeriode.UAVKLART
                }
            Periodisering(utfall, deltagelsePeriode)
        }
    }

    data class Saksbehandler(
        override val tiltaksnavn: String,
        override val eksternDeltagelseId: String,
        override val gjennomføringId: String?,
        override val tidsstempel: LocalDateTime,
        override val status: TiltakDeltakerstatus,
        override val deltagelsePeriode: Periode,
        override val kilde: Tiltakskilde,
        override val tiltakstype: TiltakstypeSomGirRett,
        override val navIdent: String,
        override val årsakTilEndring: ÅrsakTilEndring,
    ) : TiltaksdeltagelseSaksopplysning {

        // TODO jah: Per tidspunkt tvinger vi denne til å være HarSluttet dersom saksbehandler skal endre status. På sikt må vi ha samme logikk på tvers av opplysningstypene.
        override val utfallForPeriode: Periodisering<UtfallForPeriode> = Periodisering(UtfallForPeriode.IKKE_OPPFYLT, deltagelsePeriode)

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
