.PHONY: run login create read update delete test-all

TOKEN = $(shell curl -s -X POST http://localhost:2929/login -H "Content-Type: application/json" -d '{"username":"admin", "password":"admin"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

run:
	mvn clean compile exec:java -Dexec.mainClass="dev.handziur.Main"

login:
	curl -X POST http://localhost:2929/login -H "Content-Type: application/json" -d '{"username":"admin", "password":"admin"}'

create:
	curl -X PUT http://localhost:2929/products -H "Authorization: Bearer $(TOKEN)" -H "Content-Type: application/json" -d '{"id":0,"name":"Dummy","category":"Trip-Hop","quantity":50,"price":15.0}'

read:
	curl -X GET http://localhost:2929/products/1 -H "Authorization: Bearer $(TOKEN)"

update:
	curl -X POST http://localhost:2929/products/1 -H "Authorization: Bearer $(TOKEN)" -H "Content-Type: application/json" -d '{"id":1,"name":"Dummy Remastered","category":"Trip-Hop","quantity":100,"price":25.0}'

delete:
	curl -X DELETE http://localhost:2929/products/1 -H "Authorization: Bearer $(TOKEN)"

test-all: create read update delete