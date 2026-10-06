<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.carlos</groupId>
        <artifactId>carlos-parent</artifactId>
        <version>3.0.0-SNAPSHOT</version>
    </parent>
    <packaging>pom</packaging>

    <version>${project.version}</version>
    <groupId>${project.groupId}</groupId>
    <artifactId>${project.artifactId}</artifactId>
    <name>${project.projectName}</name>
    <description>${project.describe}</description>

    <modelVersion>4.0.0</modelVersion>

    <properties>

    </properties>

    <dependencies>
        <dependency>
            <groupId>com.carlos</groupId>
            <artifactId>carlos-spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>


    <build>
        <finalName>${r'${project.parent.artifactId}'}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
