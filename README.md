# Debezium to ActiveMQ artemis - PoC


## Quick start

```bash
# Start an activeMQ instance
docker run -it --rm -p 8161:8161 -p 61616:61616 vromero/activemq-artemis

# Start an MySQL example database
docker run -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw debezium/example-mysql:1.2

# Run debezium engine
mvn exec:java@MyDebeziumEngine

```

Now you'll have the essential components running. To verify if the CDC events are correctly propagated:

```bash
# Run ActiveMQ listener (in seperate console)s
mvn exec:java@Listener

# Make some changes in MySQL (password see config file)
docker exec -it mysql bin/bash
mysql -u mysqluser -p
use inventory;
UPDATE customers SET first_name = "John" where id = "1001";

```

## Development


Some usefull mysql commands:

```bash
docker exec -it mysql bin/bash

# login as root user
mysql -u root -p 
SELECT host,user,authentication_string FROM mysql.user;
SHOW GRANTS FOR 'mysqluser';

# add require grants for application user
GRANT SUPER ON *.* TO 'mysqluser'@'%';
GRANT RELOAD ON *.* TO 'mysqluser'@'%';
GRANT REPLICATION SLAVE ON *.* TO 'mysqluser'@'%';

GRANT REPLICATION CLIENT, SUPER ON *.* TO 'mysqluser'@'%';

GRANT ALL ON *.* TO 'mysqluser'@'%';

# login as same user as java app
mysql -u mysqluser -p
show databases;
use inventory;

# Example statements
show tables;
SELECT * FROM customers;

INSERT INTO customers VALUES ( 1005, 'John', 'Gandalf', 'john.gandalf@example.com');
INSERT INTO customers (first_name, last_name, email) VALUES ( 'John', 'Gandalf', 'john.gandalf@example.com');

UPDATE customers SET email = "john.gandalf@example.com" WHERE id = '1005';
```

Login into artemis web console:

Open browser on ``http://127.0.0.1:8161/`` 
* username: artemis
* password: simetraehcapa


## Reference:
* https://github.com/vromero/activemq-artemis-docker