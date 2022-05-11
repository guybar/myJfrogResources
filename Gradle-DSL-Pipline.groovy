node {
    def server
    def rtGradle = Artifactory.newGradleBuild()
    def buildInfo = Artifactory.newBuildInfo()
    def promotionConfig

    stage ('Clone') {
        git branch: 'main', url: 'https://github.com/spring-projects/spring-petclinic.git'
        //git url: 'https://github.com/jfrog/project-examples.git'
    }

    stage ('Artifactory configuration') {
        // Obtain an Artifactory server instance, defined in Jenkins --> Manage Jenkins --> Configure System:
        server = Artifactory.server 'my-local-artifactory'

        rtGradle.tool = 'gradle-6.9' // Tool name from Jenkins configuration
        rtGradle.deployer repo: 'gradle-local', server: server
        rtGradle.resolver repo: 'gradle-local', server: server
    }

 
    stage ('Test') {
        rtGradle.run rootDir: '.', tasks: 'clean test'
    }

 
    stage ('Build') {
        rtGradle.run rootDir: '.', tasks: 'clean build -x test'
    }
    
    stage ('Deploy') {
        rtGradle.run rootDir: ".", tasks: 'artifactoryPublish', buildInfo: buildInfo
        //rtGradle.run rootDir: "gradle-examples/gradle-example-ci-server/", tasks: 'clean artifactoryPublish', buildInfo: buildInfo

    }

    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }

    stage ('Promotion') {
        promotionConfig = [
            //Mandatory parameters
            'buildName'          : buildInfo.name,
            'buildNumber'        : buildInfo.number,
            'targetRepo'         : 'gradle-virtual-release',

            //Optional parameters
            'comment'            : 'this is the promotion comment',
            'sourceRepo'         : 'gradle-virtual',
            'status'             : 'Released',
            'includeDependencies': false,
            'failFast'           : true,
            'copy'               : true
        ]

        // Promote build
        server.promote promotionConfig
    }
}