export JAVA_HOME=$(realpath /usr/bin/javadoc | sed 's@bin/javadoc$@@')

mvn clean -P release -DskipTests deploy
