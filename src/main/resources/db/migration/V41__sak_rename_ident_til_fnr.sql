ALTER TABLE sak RENAME COLUMN ident TO fnr;
ALTER TABLE søknad RENAME COLUMN ident TO fnr;
ALTER TABLE statistikk_sak RENAME COLUMN ident TO fnr;

ALTER INDEX saks_ident RENAME TO saks_fnr;
ALTER INDEX søknad_ident RENAME TO søknad_fnr;