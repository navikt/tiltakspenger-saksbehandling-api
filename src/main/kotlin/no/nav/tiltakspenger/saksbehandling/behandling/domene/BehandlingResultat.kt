package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface BehandlingResultat {

    /* Denne benyttes både i søknadsbehandlinger og revurderinger */
    sealed interface Innvilgelse {
        val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?
        val antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode>?
        val barnetillegg: Barnetillegg?

        fun valider(status: Behandlingsstatus, virkningsperiode: Periode?) {
            // TODO - Disse sjekkene er veldig lite intuitivt fra saksbehandlers side fordi det bare fører til en exception
            when (status) {
                KLAR_TIL_BESLUTNING,
                UNDER_BESLUTNING,
                VEDTATT,
                -> {
                    requireNotNull(virkningsperiode) {
                        "Virkningsperiode må være satt for innvilget behandling med status $status"
                    }

                    valgteTiltaksdeltakelser?.also {
                        require(it.periodisering.totalPeriode == virkningsperiode) {
                            "Total periode for valgte tiltaksdeltakelser (${it.periodisering.totalPeriode}) må stemme overens med virkningsperioden ($virkningsperiode)"
                        }
                    } ?: throw IllegalStateException("Valgte tiltaksdeltakelser må være satt for innvilget behandling med status $status")

                    barnetillegg?.also {
                        val barnetilleggsperiode = it.periodisering.totalPeriode
                        require(barnetilleggsperiode == virkningsperiode) {
                            "Barnetilleggsperioden ($barnetilleggsperiode) må ha samme periode som virkningsperioden($virkningsperiode)"
                        }
                    }
                    require(antallDagerPerMeldeperiode?.totalPeriode == virkningsperiode) {
                        "Innvilgelsesperioden ($virkningsperiode) må være lik som antallDagerPerMeldeperiode sin totale periode($antallDagerPerMeldeperiode)"
                    }
                }
                UNDER_AUTOMATISK_BEHANDLING,
                KLAR_TIL_BEHANDLING,
                UNDER_BEHANDLING,
                AVBRUTT,
                -> Unit
            }
        }
    }
}

sealed interface BehandlingResultatType

enum class SøknadsbehandlingType : BehandlingResultatType {
    INNVILGELSE,
    AVSLAG,
}

enum class RevurderingType : BehandlingResultatType {
    STANS,
    INNVILGELSE,
}
