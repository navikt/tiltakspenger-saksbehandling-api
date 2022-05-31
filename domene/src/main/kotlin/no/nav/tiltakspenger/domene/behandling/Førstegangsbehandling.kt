package no.nav.tiltakspenger.domene.behandling

import no.nav.tiltakspenger.domene.Periode
import no.nav.tiltakspenger.domene.Søknad
import no.nav.tiltakspenger.domene.Vilkårsvurderinger
import no.nav.tiltakspenger.domene.fakta.Faktum
import java.time.LocalDate

class Førstegangsbehandling private constructor(
    val ident: String,
    var søknad: Søknad?,
    val vilkårsvurderinger: Vilkårsvurderinger,
    var tilstand: Tilstand
) : Saksbehandling {
    constructor(ident: String) : this(
        vilkårsvurderinger = Vilkårsvurderinger(
            periode = Periode(fra = LocalDate.now(), til = LocalDate.now()),
            vilkårsvurderinger = emptyList()
        ),
        søknad = null,
        tilstand = Tilstand.Start,
        ident = ident
    )

    override fun behandle(søknad: Søknad) {
        tilstand.behandle(søknad, this)
    }
//    fun hentFakta() {
//        vilkårsvurdering.forEach { it.start }
//        // Noen må hente fakta
//    }

//    fun onTilstandChange(newTilstand: Tilstand) {
//        when (newTilstand) {
//            Tilstand.Tilstandstype.HENTER_OPPLYSNINGER -> hentFakta()
//        }
//    }

    fun vurder() {
        vilkårsvurderinger.vilkårsvurderinger
            // .forEach { it.vurder(faktum) }
    }

    sealed class Tilstand {
        open fun behandle(søknad: Søknad, førstegangsbehandling: Førstegangsbehandling) {
            println("KAN IKKE BEHANDLE")
        }

        open fun onEntry(førstegangsbehandling: Førstegangsbehandling) {}

        enum class Tilstandstype {
            START,
            SØKNAD_MOTTATT,
            HENTER_OPPLYSNINGER,
            VILKÅRSVURDERING,
            TIL_MANUELL_BEHANDLING,
            FERDIG
        }

        object Start : Tilstand() {
            fun håndterSøknad(søknad: Søknad) {
                // TODO masse greier
                // registere ting på førstegangsbehandlingen gitt fra søknaden
                // trenger PDL-stuff
                //
            }

            override fun behandle(søknad: Søknad, førstegangsbehandling: Førstegangsbehandling) {
                førstegangsbehandling.søknad = søknad
                requireNotNull(førstegangsbehandling.søknad) { "Her burde søknaden være satt" }
                førstegangsbehandling.nesteTilstand(Vurder)
            }
        }

        object Vurder : Tilstand() {
            override fun onEntry(førstegangsbehandling: Førstegangsbehandling) {
                val søknad = requireNotNull(førstegangsbehandling.søknad)
                førstegangsbehandling.vurder()
            }

            fun håndter(faktum: Faktum, førstegangsbehandling: Førstegangsbehandling) {
                // førstegangsbehandling.vurder(faktum)
            }
        }
    }

    private fun nesteTilstand(nestetilstand: Tilstand.Vurder) {
        tilstand = nestetilstand
        tilstand.onEntry(this)
    }
}
