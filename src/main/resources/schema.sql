CREATE TABLE IF NOT EXISTS USERS (
    userid serial PRIMARY KEY,
    username VARCHAR(20) UNIQUE,
    salt VARCHAR,
    password VARCHAR,
    firstname VARCHAR(20),
    lastname VARCHAR(20)
    );
CREATE TABLE IF NOT EXISTS FILES (
    fileid serial PRIMARY KEY,
    filename VARCHAR,
    contenttype VARCHAR,
    filesize VARCHAR,
    userid INT,
    filedata BYTEA,
    foreign key (userid) references USERS(userid)
    );