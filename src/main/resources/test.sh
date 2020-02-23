#bin/bash
curl -H "Content-Type: application/json" -X POST -d '{"name":"my cocktail","ingredients":"ingredient1,ingredients2,ingredients3","recipe":"shake it"}' http://localhost:8080/add
curl -X PUT -F image=@test_image.jpg http://localhost:8080/images/my%20cocktail/add
curl -X GET http://localhost:8080/get?name=my%20cocktail
curl -X GET http://localhost:8080/images/my%20cocktail -o test.jpg

curl -X GET http://localhost:8080/search?query=margarita
curl -X GET http://localhost:8080/search?ingredients=rom
