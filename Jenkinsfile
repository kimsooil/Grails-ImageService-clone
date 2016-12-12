node {
  env.JAVA_HOME = tool 'jdk8'
  env.GRADLE_HOME = tool 'gradle2.4'
  env.GRAILS_HOME = tool 'grails3.0.2'
  env.PATH = "${env.GRADLE_HOME}/bin:${env.GRAILS_HOME}/bin:${env.PATH}"
  checkout scm
  stage('Build ImageFetcher') {
    dir('ImageFetcher') {
      sh 'gradle distZip'
      archiveArtifacts artifacts: 'build/distributions/ImageFetcher*.zip'
    }
  }
  stage('Build ImageServer') {
    dir('ImageServer') {
      sh 'gradle distZip'
      archiveArtifacts artifacts: 'build/distributions/ImageService*.zip'
    }
  }
}