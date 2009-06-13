CREATE TABLE Table1 (
    tb2_1 varchar(20) NOT NULL,
    tb2_2 varchar(100) DEFAULT ('A'),
    tb2_3 decimal(20,5) NOT NULL
);
CREATE UNIQUE INDEX Tabl11_idx1 ON Table1 (tb2_2);
CREATE INDEX Tabl11_idx2 ON Table1 (tb2_3);
CREATE TABLE Table2 (
    tb3_1 varchar(20) NOT NULL,
    tb3_2 varchar(100) DEFAULT ('A'),
    tb3_3 decimal(20,5) NOT NULL
);
ALTER TABLE Table2 ADD CONSTRAINT PK2 PRIMARY KEY(tb3_1);
CREATE VIEW View1 AS SELECT * from Table1;
ALTER TABLE Table1 ADD CONSTRAINT FK1 FOREIGN KEY (tb2_1) REFERENCES Table2(tb3_1) ON DELETE NO ACTION ON UPDATE NO ACTION;