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
  stage('Build ImageService') {
    dir('ImageService') {
      sh './gradlew distZip'
      archiveArtifacts artifacts: 'build/distributions/ImageService*.zip'
    }
  }
  stage('Get Ansible Roles') {
    sh 'ansible-galaxy install -r ansible/requirements.yml -p ansible/roles/ -f'
  }
  stage('Build ImageFetcher and ImageService') {
    sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml"
  }
}