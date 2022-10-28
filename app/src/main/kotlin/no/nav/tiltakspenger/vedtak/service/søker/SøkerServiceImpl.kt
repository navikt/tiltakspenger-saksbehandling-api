package no.nav.tiltakspenger.vedtak.service.søker

import no.nav.tiltakspenger.vedtak.repository.SøkerRepository

class SøkerServiceImpl(
    val søkerRepository: SøkerRepository
) : SøkerService {

    override fun hentSøkerOgSøknader(ident: String): SøkerDTO? {
        val søker = søkerRepository.hent(ident) ?: return null
        return SøkerDTO(
            ident = søker.ident,
            søknader = søker.søknader.map {
                ListeSøknadDTO(
                    søknadId = it.søknadId,
                    arrangoernavn = it.tiltak.arrangoernavn,
                    tiltakskode = it.tiltak.tiltakskode?.navn,
                    startdato = it.tiltak.startdato,
                    sluttdato = it.tiltak.sluttdato,
                )
            }
        )
    }
}
