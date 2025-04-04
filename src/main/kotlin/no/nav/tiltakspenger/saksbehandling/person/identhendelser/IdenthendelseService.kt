package no.nav.tiltakspenger.saksbehandling.person.identhendelser

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseDb
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseRepository
import java.util.UUID

class IdenthendelseService(
    private val sakRepo: SakRepo,
    private val identhendelseRepository: IdenthendelseRepository,
) {
    private val log = KotlinLogging.logger { }

    fun behandleIdenthendelse(aktor: Aktor) {
        try {
            val personidenter = aktor.identifikatorer.map { it.toPersonident() }
            val gjeldendeIdent = finnGjeldendeIdent(personidenter)
            val nyttFnr = Fnr.tryFromString(gjeldendeIdent.ident)
            if (nyttFnr == null) {
                sikkerlogg.error { "Ny ident er ikke gyldig fnr: ${gjeldendeIdent.ident}" }
                throw IllegalArgumentException("Ny ident må være gyldig fnr")
            }

            val historiskeFnrOgNpid = personidenter.filter { it.historisk && it.identtype != Identtype.AKTORID }

            var antallSaker = 0
            if (sakRepo.hentForFnr(nyttFnr).saker.isNotEmpty()) {
                log.warn { "Fant sak på nytt fnr" }
                antallSaker += 1
            }
            historiskeFnrOgNpid.forEach {
                val fnr = Fnr.tryFromString(it.ident) ?: return@forEach
                val saker = sakRepo.hentForFnr(fnr)
                if (saker.saker.isNotEmpty()) {
                    antallSaker += 1
                    if (antallSaker > 1) {
                        log.error { "Fant flere saker knyttet til samme nye fnr, kan ikke behandle identendringen. Se sikkerlogg for mer informasjon" }
                        sikkerlogg.error { "Fant flere saker knyttet til samme nye fnr, kan ikke behandle identendringen: $personidenter" }
                        throw IllegalStateException("Fant flere saker knyttet til samme nye fnr, kan ikke behandle identendringen")
                    }
                    val sak = saker.saker.single()
                    val identhendelseDb = IdenthendelseDb(
                        id = UUID.randomUUID(),
                        gammeltFnr = fnr,
                        nyttFnr = nyttFnr,
                        personidenter = personidenter,
                        sakId = sak.id,
                        produsertHendelse = null,
                        oppdatertDatabase = null,
                    )
                    identhendelseRepository.lagre(identhendelseDb)
                    log.info { "Lagret identhendelse med id ${identhendelseDb.id}" }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Noe gikk galt ved håndtering av identhendelse" }
            throw e
        }
    }

    private fun finnGjeldendeIdent(personidenter: List<Personident>): Personident {
        val gjeldendeIdent = personidenter.firstOrNull {
            !it.historisk && it.identtype == Identtype.FOLKEREGISTERIDENT
        } ?: personidenter.firstOrNull {
            !it.historisk && it.identtype == Identtype.NPID
        }
        if (gjeldendeIdent == null) {
            log.error { "Kan ikke behandle identhendelse uten gjeldende ident, se sikkerlogg for mer informasjon" }
            sikkerlogg.error { "Kan ikke behandle identhendelse uten gjeldende ident: $personidenter" }
            throw IllegalArgumentException("Kan ikke behandle identhendelse uten gjeldende ident")
        }
        return gjeldendeIdent
    }
}
