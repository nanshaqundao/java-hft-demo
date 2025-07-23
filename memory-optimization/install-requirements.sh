#!/bin/bash

# Download and setup Java 21, Maven, and Gradle (portable installation)

echo "Downloading and setting up Java 21, Maven, and Gradle..."

# Create tools directory
mkdir -p ~/tools
cd ~/tools

# Download OpenJDK 21 (portable)
echo "Downloading OpenJDK 21..."
if [ ! -d "jdk-21" ]; then
    wget -O openjdk-21.tar.gz "https://download.java.net/java/GA/jdk21.0.1/415e3f918a1f4062a0074a2794853d0d/12/GPL/openjdk-21.0.1_linux-x64_bin.tar.gz"
    tar -xzf openjdk-21.tar.gz
    mv jdk-21.0.1 jdk-21
    rm openjdk-21.tar.gz
    echo "OpenJDK 21 downloaded and extracted"
else
    echo "OpenJDK 21 already exists"
fi

# Download Maven (portable)
echo "Downloading Maven..."
if [ ! -d "maven" ]; then
    wget -O maven.tar.gz "https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz"
    tar -xzf maven.tar.gz
    mv apache-maven-3.9.5 maven
    rm maven.tar.gz
    echo "Maven downloaded and extracted"
else
    echo "Maven already exists"
fi

# Download Gradle (portable)
echo "Downloading Gradle..."
if [ ! -d "gradle" ]; then
    # Check if unzip is available, if not use alternative extraction
    if command -v unzip &> /dev/null; then
        wget -O gradle.zip "https://services.gradle.org/distributions/gradle-8.5-bin.zip"
        unzip -q gradle.zip
        mv gradle-8.5 gradle
        rm gradle.zip
    else
        echo "unzip not found, installing unzip or using alternative..."
        # Try to install unzip first
        if command -v apt-get &> /dev/null; then
            echo "Installing unzip..."
            sudo apt-get update && sudo apt-get install -y unzip 2>/dev/null || true
        fi
        
        # If unzip is now available, use it
        if command -v unzip &> /dev/null; then
            wget -O gradle.zip "https://services.gradle.org/distributions/gradle-8.5-bin.zip"
            unzip -q gradle.zip
            mv gradle-8.5 gradle
            rm gradle.zip
        else
            echo "Cannot install unzip, downloading all distribution instead..."
            wget -O gradle-all.zip "https://services.gradle.org/distributions/gradle-8.5-all.zip"
            # Use python to extract if available
            if command -v python3 &> /dev/null; then
                python3 -c "import zipfile; zipfile.ZipFile('gradle-all.zip').extractall('.')"
                mv gradle-8.5 gradle
                rm gradle-all.zip
            else
                echo "Error: Cannot extract Gradle archive. Please install unzip or python3"
                exit 1
            fi
        fi
    fi
    echo "Gradle downloaded and extracted"
else
    echo "Gradle already exists"
fi

# Set up environment variables
JAVA_HOME=~/tools/jdk-21
MAVEN_HOME=~/tools/maven
GRADLE_HOME=~/tools/gradle
PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$GRADLE_HOME/bin:$PATH

# Detect shell and add to appropriate config file
if [ -n "$ZSH_VERSION" ]; then
    SHELL_RC=~/.zshrc
    echo "Detected zsh shell"
elif [ -n "$BASH_VERSION" ]; then
    SHELL_RC=~/.bashrc
    echo "Detected bash shell"
else
    SHELL_RC=~/.bashrc
    echo "Defaulting to bash shell"
fi

# Add environment variables to shell config
echo "" >> $SHELL_RC
echo "# Java 21, Maven, and Gradle - HFT Memory Optimization Project" >> $SHELL_RC
echo "export JAVA_HOME=~/tools/jdk-21" >> $SHELL_RC
echo "export MAVEN_HOME=~/tools/maven" >> $SHELL_RC
echo "export GRADLE_HOME=~/tools/gradle" >> $SHELL_RC
echo "export PATH=\$JAVA_HOME/bin:\$MAVEN_HOME/bin:\$GRADLE_HOME/bin:\$PATH" >> $SHELL_RC

# Export for current session
export JAVA_HOME=~/tools/jdk-21
export MAVEN_HOME=~/tools/maven
export GRADLE_HOME=~/tools/gradle
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$GRADLE_HOME/bin:$PATH

echo ""
echo "=== Installation Complete ==="
echo "Java, Maven, and Gradle have been downloaded to ~/tools/"
echo "Environment variables added to $SHELL_RC"
echo ""
echo "Please run: source $SHELL_RC"
echo "Or restart your terminal to use the new environment"
echo ""

# Verify installations
echo "Current session verification:"
if command -v java &> /dev/null; then
    echo "Java version:"
    java -version
else
    echo "Java not found in current session - please source $SHELL_RC"
fi

if command -v mvn &> /dev/null; then
    echo "Maven version:"
    mvn -version
else
    echo "Maven not found in current session - please source $SHELL_RC"
fi

if command -v gradle &> /dev/null; then
    echo "Gradle version:"
    gradle -version
else
    echo "Gradle not found in current session - please source $SHELL_RC"
fi

echo ""
echo "Java 21 features available: Virtual Threads, Pattern Matching, Records, etc."
echo "Now you can build the project with:"
echo "  Maven: mvn clean compile"
echo "  Gradle: gradle build (if you have a build.gradle file)"