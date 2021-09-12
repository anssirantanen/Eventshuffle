CREATE EXTENSION "uuid-ossp";
CREATE TABLE event (
    id UUID primary key,
    name text
);
CREATE TABLE event_date (
    event_date date,
    event UUID references event,
    event_voter text[],
    PRIMARY KEY(event_date,event)
);