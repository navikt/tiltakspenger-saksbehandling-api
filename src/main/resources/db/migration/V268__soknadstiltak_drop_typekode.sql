-- typekode er alltid lik tiltaksdeltaker.tiltakstype (verifisert i prod), og brukes ikke lenger.
ALTER TABLE søknadstiltak
    DROP COLUMN IF EXISTS typekode;
