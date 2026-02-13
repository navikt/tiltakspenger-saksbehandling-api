package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.ARBEIDSMARKEDSLOVEN_13
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.ARBEIDSMARKEDSLOVEN_13_L1
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.ARBEIDSMARKEDSLOVEN_15
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TILTAKSPENGEFORSKRIFTEN_11
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TILTAKSPENGEFORSKRIFTEN_2
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TILTAKSPENGEFORSKRIFTEN_3
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TILTAKSPENGEFORSKRIFTEN_7_L1
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TILTAKSPENGEFORSKRIFTEN_7_L3
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TILTAKSPENGEFORSKRIFTEN_8
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TILTAKSPENGEFORSKRIFTEN_8_L2
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TILTAKSPENGEFORSKRIFTEN_9

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=679150248
 */
enum class Avslagsgrunnlag(val hjemler: List<Hjemmel>) {
    DeltarIkkePåArbeidsmarkedstiltak(listOf(TILTAKSPENGEFORSKRIFTEN_2, ARBEIDSMARKEDSLOVEN_13)),
    Alder(listOf(TILTAKSPENGEFORSKRIFTEN_3)),
    Livsoppholdytelser(listOf(TILTAKSPENGEFORSKRIFTEN_7_L1, ARBEIDSMARKEDSLOVEN_13_L1)),
    Kvalifiseringsprogrammet(listOf(TILTAKSPENGEFORSKRIFTEN_7_L3)),
    Introduksjonsprogrammet(listOf(TILTAKSPENGEFORSKRIFTEN_7_L3)),
    LønnFraTiltaksarrangør(listOf(TILTAKSPENGEFORSKRIFTEN_8)),
    LønnFraAndre(listOf(TILTAKSPENGEFORSKRIFTEN_8_L2, ARBEIDSMARKEDSLOVEN_13)),
    Institusjonsopphold(listOf(TILTAKSPENGEFORSKRIFTEN_9)),
    FremmetForSent(listOf(TILTAKSPENGEFORSKRIFTEN_11, ARBEIDSMARKEDSLOVEN_15)),
}
