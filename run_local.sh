gradle bootJar
sleep 1
java -jar ./build/libs/jsql-spring-boot-test-app.jar --spring.datasource.url=jdbc:postgresql://172.32.1.31:5452/plugins_test?ssl=false --spring.datasource.username=postgres_user --spring.datasource.password=fgfdbc45trdgf