package com.github.alexeylisyutenko.windowsserviceplugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class WindowsServicePluginTest extends Specification {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    File settingsFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        settingsFile = testProjectDir.newFile('settings.gradle')
    }

    private Project setupBasicProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.alexeylisyutenko.windows-service-plugin'
        return project
    }

    private File createFakeFile(String fileName, long size) {
        File file = testProjectDir.newFile(fileName)
        file.withOutputStream { outputStream ->
            Random random = new Random()
            for (int i = 0; i < size; i++) {
                outputStream.write(random.nextInt())
            }
        }
        file
    }

    def "Applying the plugin adds corresponding task to project"() {
        given: "a basic project with com.github.alexeylisyutenko.windows-service-plugin"
        Project project = setupBasicProject()

        when: "project evaluated"
        project.evaluate()

        then: "createWindowsService task added"
        assert project.tasks.createWindowsService instanceof WindowsServicePluginTask
    }

    def "Applying the plugin applies java plugin to project"() {
        given: "a basic project with com.github.alexeylisyutenko.windows-service-plugin"
        Project project = setupBasicProject()

        when: "project evaluated"
        project.evaluate()

        then: "java plugin applied"
        assert project.plugins.hasPlugin('java')
    }

    def 'Applying the plugin provides properties'() {
        given:
        buildFile << """
            plugins {
                id 'com.github.alexeylisyutenko.windows-service-plugin'
            }
            task printProperties {
                doLast {
                    println windowsService.outputDir
                }
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('-q', 'printProperties')
                .withPluginClasspath()
                .build()

        then:
        result.task(':printProperties').outcome == SUCCESS
        result.output.trim() == 'windows-service'
    }

    def "testProject should be successfully executed"() {
        given:
        settingsFile << "rootProject.name = 'testProject'"
        buildFile << """
            plugins {
                id 'com.github.alexeylisyutenko.windows-service-plugin'
            }
            windowsService {
                architecture = 'amd64'
                displayName = 'TestService'
                description = 'Service generated with using gradle plugin'
                startClass = 'Main'
                startMethod = 'main'
                startParams = 'start'
                stopClass = 'Main'
                stopMethod = 'main'
                stopParams = 'stop'
                startup = 'auto'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('createWindowsService')
                .withPluginClasspath()
                .build()

        then:
        result.task(":createWindowsService").outcome == SUCCESS
        new File(testProjectDir.root, "build/windows-service/testProject.exe").exists()
        new File(testProjectDir.root, "build/windows-service/testProjectw.exe").exists()
        new File(testProjectDir.root, "build/windows-service/testProject-install.bat").exists()
        new File(testProjectDir.root, "build/windows-service/testProject-uninstall.bat").exists()
        new File(testProjectDir.root, "build/windows-service/lib/testProject.jar").exists()
    }

    def "outputDir property should change plugin output directory"() {
        given:
        settingsFile << "rootProject.name = 'testProject'"
        buildFile << """
            plugins {
                id 'com.github.alexeylisyutenko.windows-service-plugin'
            }
            windowsService {
                outputDir = 'new-output-directory'
                architecture = 'amd64'
                displayName = 'TestService'
                description = 'Service generated with using gradle plugin'
                startClass = 'Main'
                startMethod = 'main'
                startParams = 'start'
                stopClass = 'Main'
                stopMethod = 'main'
                stopParams = 'stop'
                startup = 'auto'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('createWindowsService')
                .withPluginClasspath()
                .build()

        then:
        result.task(":createWindowsService").outcome == SUCCESS
        !new File(testProjectDir.root, "build/windows-service").exists()
        new File(testProjectDir.root, "build/new-output-directory/testProject.exe").exists()
        new File(testProjectDir.root, "build/new-output-directory/testProjectw.exe").exists()
        new File(testProjectDir.root, "build/new-output-directory/testProject-install.bat").exists()
        new File(testProjectDir.root, "build/new-output-directory/testProject-uninstall.bat").exists()
        new File(testProjectDir.root, "build/new-output-directory/lib/testProject.jar").exists()
    }

    def "description, startParams, stopParams properties should be optional"() {
        given:
        settingsFile << "rootProject.name = 'testProject'"
        buildFile << """
            plugins {
                id 'com.github.alexeylisyutenko.windows-service-plugin'
            }
            windowsService {
                architecture = 'amd64'
                displayName = 'TestService'
                startClass = 'Main'
                startMethod = 'main'
                stopClass = 'Main'
                stopMethod = 'main'
                startup = 'auto'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('createWindowsService')
                .withPluginClasspath()
                .build()

        then:
        result.task(":createWindowsService").outcome == SUCCESS
    }

    def "A script with a full set of all procrun configuration parameters should work properly"() {
        given:
        settingsFile << "rootProject.name = 'testProject'"
        buildFile << """
            plugins {
                id 'com.github.alexeylisyutenko.windows-service-plugin'
            }
            windowsService {
                architecture = 'amd64'
                displayName = 'TestService'
                description = 'Service generated with using gradle plugin'
                startClass = 'Main'
                startMethod = 'main'
                startParams = 'start'
                stopClass = 'Main'
                stopMethod = 'main'
                stopParams = 'stop'
                startup = 'auto'
                interactive = true
                dependsOn = 'AnotherWindowsService'
                environment = 'envKey1=value1;envKey2=value2;envKey3=value3'
                libraryPath = '.\\\\runtime\\\\bin'
                javaHome = '.\\\\runtime'
                jvm = 'C:\\\\Program Files\\\\Java\\\\jdk1.8.0_112\\\\jre\\\\bin\\\\server\\\\jvm.dll'
                jvmOptions = '-XX:NewRatio=1#-XX:+UseConcMarkSweepGC'  
                jvmOptions9 = '-XX:NewRatio=1#-XX:+UseConcMarkSweepGC'  
                jvmMs = 1024
                jvmMx = 2048
                jvmSs = 512
                stopTimeout = 60
                logPath = '.\\\\procrun-logs'     
                logPrefix = 'log-prefix'
                logLevel = 'error'
                logJniMessages = 1
                stdOutput = 'auto'
                stdError = 'stderr.txt'
                pidFile = 'pid.txt'  
                serviceUser = '.\\\\ServiceUser'
                servicePassword = 'servicePassword'  
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('createWindowsService')
                .withPluginClasspath()
                .build()
        def installScriptLines = new File(testProjectDir.root, "build/windows-service/testProject-install.bat").readLines()

        then:
        result.task(":createWindowsService").outcome == SUCCESS

        new File(testProjectDir.root, "build/windows-service/testProject.exe").exists()
        new File(testProjectDir.root, "build/windows-service/testProjectw.exe").exists()
        new File(testProjectDir.root, "build/windows-service/testProject-install.bat").exists()
        new File(testProjectDir.root, "build/windows-service/testProject-uninstall.bat").exists()
        new File(testProjectDir.root, "build/windows-service/lib/testProject.jar").exists()

        installScriptLines.any { it.contains("--DisplayName=TestService") }
        installScriptLines.any { it.contains("--Description=\"Service generated with using gradle plugin\"") }
        installScriptLines.any { it.contains("--StartClass=Main") }
        installScriptLines.any { it.contains("--StartMethod=main") }
        installScriptLines.any { it.contains("++StartParams=start") }
        installScriptLines.any { it.contains("--StopClass=Main") }
        installScriptLines.any { it.contains("--StopMethod=main") }
        installScriptLines.any { it.contains("++StopParams=stop") }
        installScriptLines.any { it.contains("--Type=interactive") }
        installScriptLines.any { it.contains("++DependsOn=AnotherWindowsService") }
        installScriptLines.any { it.contains("++Environment=envKey1=value1;envKey2=value2;envKey3=value3") }
        installScriptLines.any { it.contains("--LibraryPath=.\\runtime\\bin") }
        installScriptLines.any { it.contains("--JavaHome=.\\runtime") }
        installScriptLines.any {
            it.contains("--Jvm=\"C:\\Program Files\\Java\\jdk1.8.0_112\\jre\\bin\\server\\jvm.dll\"")
        }
        installScriptLines.any { it.contains("++JvmOptions=-XX:NewRatio=1#-XX:+UseConcMarkSweepGC") }
        installScriptLines.any { it.contains("++JvmOptions9=-XX:NewRatio=1#-XX:+UseConcMarkSweepGC") }
        installScriptLines.any { it.contains("--JvmMs=1024") }
        installScriptLines.any { it.contains("--JvmMx=2048") }
        installScriptLines.any { it.contains("--JvmSs=512") }
        installScriptLines.any { it.contains("--StopTimeout=60") }
        installScriptLines.any { it.contains("--LogPath=.\\procrun-logs") }
        installScriptLines.any { it.contains("--LogPrefix=log-prefix") }
        installScriptLines.any { it.contains("--LogLevel=Error") }
        installScriptLines.any { it.contains("--LogJniMessages=1") }
        installScriptLines.any { it.contains("--StdOutput=auto") }
        installScriptLines.any { it.contains("--StdError=stderr.txt") }
        installScriptLines.any { it.contains("-PidFile=pid.txt") }
        installScriptLines.any { it.contains("--ServiceUser=.\\ServiceUser") }
        installScriptLines.any { it.contains("--ServicePassword=servicePassword") }
    }

    def "All jar task outputs and runtime dependencies should be copied to the lib directory and added to the classpath"() {
        given:
        createFakeFile('first-dependency.jar', new Random().nextInt(8192))
        createFakeFile('second-dependency.jar', new Random().nextInt(8192))
        createFakeFile('third-dependency.jar', new Random().nextInt(8192))
        settingsFile << "rootProject.name = 'testProject'"
        buildFile << """
            plugins {
                id 'com.github.alexeylisyutenko.windows-service-plugin'
            }
            windowsService {
                architecture = 'amd64'
                displayName = 'TestService'
                description = 'Service generated with using gradle plugin'
                startClass = 'Main'
                startMethod = 'main'
                startParams = 'start'
                stopClass = 'Main'
                stopMethod = 'main'
                stopParams = 'stop'
                startup = 'auto'
            }
            dependencies {
                runtime files('first-dependency.jar', 'second-dependency.jar', 'third-dependency.jar')
            }
        """
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('createWindowsService')
                .withPluginClasspath()
                .build()
        def installScriptLines = new File(testProjectDir.root, "build/windows-service/testProject-install.bat").readLines()

        then:
        result.task(":createWindowsService").outcome == SUCCESS
        installScriptLines.any {
            it.contains("set CLASSPATH=%APP_HOME%lib\\testProject.jar;%APP_HOME%lib\\first-dependency.jar;%APP_HOME%lib\\second-dependency.jar;%APP_HOME%lib\\third-dependency.jar")
        }
        new File(testProjectDir.root, "build/windows-service/lib/testProject.jar").exists()
        new File(testProjectDir.root, "build/windows-service/lib/first-dependency.jar").exists()
        new File(testProjectDir.root, "build/windows-service/lib/second-dependency.jar").exists()
        new File(testProjectDir.root, "build/windows-service/lib/third-dependency.jar").exists()
    }

    def "overridingClasspath should override a classpath obtained from the jar task"() {
        given:
        createFakeFile('first-dependency.jar', new Random().nextInt(8192))
        createFakeFile('second-dependency.jar', new Random().nextInt(8192))
        createFakeFile('third-dependency.jar', new Random().nextInt(8192))
        createFakeFile('first-overriding-classpath-dependency.jar', new Random().nextInt(8192))
        createFakeFile('second-overriding-classpath-dependency.jar', new Random().nextInt(8192))
        settingsFile << "rootProject.name = 'testProject'"
        buildFile << """
            plugins {
                id 'com.github.alexeylisyutenko.windows-service-plugin'
            }
            windowsService {
                architecture = 'amd64'
                displayName = 'TestService'
                description = 'Service generated with using gradle plugin'
                startClass = 'Main'
                startMethod = 'main'
                startParams = 'start'
                stopClass = 'Main'
                stopMethod = 'main'
                stopParams = 'stop'
                startup = 'auto'
                overridingClasspath = files('first-overriding-classpath-dependency.jar', 'second-overriding-classpath-dependency.jar')
            }
            dependencies {
                runtime files('first-dependency.jar', 'second-dependency.jar', 'third-dependency.jar')
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('createWindowsService')
                .withPluginClasspath()
                .build()
        def installScriptLines = new File(testProjectDir.root, "build/windows-service/testProject-install.bat").readLines()

        then:
        result.task(":createWindowsService").outcome == SUCCESS
        installScriptLines.any {
            it.contains("set CLASSPATH=%APP_HOME%lib\\first-overriding-classpath-dependency.jar;%APP_HOME%lib\\second-overriding-classpath-dependency.jar")
        }
        !new File(testProjectDir.root, "build/windows-service/lib/testProject.jar").exists()
        !new File(testProjectDir.root, "build/windows-service/lib/first-dependency.jar").exists()
        !new File(testProjectDir.root, "build/windows-service/lib/second-dependency.jar").exists()
        !new File(testProjectDir.root, "build/windows-service/lib/third-dependency.jar").exists()
        new File(testProjectDir.root, "build/windows-service/lib/first-overriding-classpath-dependency.jar").exists()
        new File(testProjectDir.root, "build/windows-service/lib/second-overriding-classpath-dependency.jar").exists()
    }

    def "The createWindowsService task should fail if architecture is ia64"() {
        given:
        settingsFile << "rootProject.name = 'testProject'"
        buildFile << """
            plugins {
                id 'com.github.alexeylisyutenko.windows-service-plugin'
            }
            windowsService {
                architecture = 'ia64'
                displayName = 'TestService'
                description = 'Service generated with using gradle plugin'
                startClass = 'Main'
                startMethod = 'main'
                startParams = 'start'
                stopClass = 'Main'
                stopMethod = 'main'
                stopParams = 'stop'
                startup = 'auto'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('createWindowsService')
                .withPluginClasspath()
                .buildAndFail()

        then:
        result.task(":createWindowsService").outcome == FAILED
    }

}
