create keyspace cocktails WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};

use cocktails;

CREATE TABLE catalog (name text, ingredients text, recipe text, image blob, ts bigint, PRIMARY KEY(name, ingredients));