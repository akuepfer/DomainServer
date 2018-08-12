
echo "build the server target/DomainServer-1.0-SNAPSHOT.jar"
mvn -Dmaven.test.skip=true clean install

echo "copy all used jar files to target/dependency/"
mvn dependency:copy-dependencies

