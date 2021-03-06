CREATE TABLE TESTTABLE (
	PK1 VARCHAR(10) NOT NULL,
	PK2 INTEGER NOT NULL DEFAULT 42,
	AT1 VARCHAR(5),
	AT2 VARCHAR(5) NOT NULL,
	AT3 VARCHAR(5)
) ENGINE=MyISAM;
ALTER TABLE TESTTABLE ADD CONSTRAINT TESTTABLE_PK PRIMARY KEY(PK1,PK2);
CREATE INDEX TESTTABLE_IDX1 ON TESTTABLE (AT2);
CREATE UNIQUE INDEX TESTTABLE_IDX2 ON TESTTABLE (AT3);