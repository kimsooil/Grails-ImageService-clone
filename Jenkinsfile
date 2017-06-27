node('master') {
  env.JAVA_HOME = tool 'jdk8'
  env.GRADLE_HOME = tool 'gradle3.3'
  env.GRAILS_HOME = tool 'grails3.0.2'
  env.ANSIBLE_HOME = tool 'ansible2.2.0'
  def mvnHome = tool 'maven3'
  
  env.PATH = "${env.JENKINS_HOME}/bin:${mvnHome}/bin:${env.GRADLE_HOME}/bin:${env.GRAILS_HOME}/bin:${env.PATH}"
  checkout scm
  stage('Get Ansible Roles') {
    sh 'ansible-galaxy install -r ansible/requirements.yml -p ansible/roles/ -f'
  }
  stage('Build ImageFetcher') {
    sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'target_hosts=all java_home=${env.JAVA_HOME} deploy_env=${env.DEPLOY_ENV} package_revision=${env.BUILD_NUMBER}' -t ImageFetcher"
  }
  stage('Build ImageService') {
    sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'target_hosts=all java_home=${env.JAVA_HOME} deploy_env=${env.DEPLOY_ENV} package_revision=${env.BUILD_NUMBER}' -t ImageService"
  }
  stage('Deploy ImageFetcher and ImageService') {
    sshagent (credentials: ['jenkins']) {
      sh "ansible-playbook -i ansible/roles/inventory/${env.DEPLOY_ENV.toLowerCase()}/hosts --user=jenkins --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'target_hosts=image_service java_home=${env.JAVA_HOME} deploy_env=${env.DEPLOY_ENV} package_revision=${env.PACKAGE_REVISION}' -b -t deploy -vvv"    
    }
  }
  stage('Build RPM artifacts') {
    dir('ImageFetcher/build/distributions') {
      archiveArtifacts artifacts: 'ImageFetcher*.rpm'
    }
    dir('ImageService/build/distributions') {
      archiveArtifacts artifacts: 'ImageService*.rpm'
    }
  }
}
