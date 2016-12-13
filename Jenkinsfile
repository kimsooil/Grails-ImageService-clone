node('master') {
  env.JAVA_HOME = tool 'jdk8'
  env.GRADLE_HOME = tool 'gradle2.4'
  env.GRAILS_HOME = tool 'grails3.0.2'
  env.PATH = "${env.JENKINS_HOME}/bin:${env.GRADLE_HOME}/bin:${env.GRAILS_HOME}/bin:${env.PATH}"
  checkout scm
  stage('Get Ansible Roles') {
    sh 'ansible-galaxy install -r ansible/requirements.yml -p ansible/roles/ -f'
  }
  stage('Build ImageFetcher') {
    sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml -t ImageFetcher"
    // archiveArtifacts artifacts: 'ImageFetcher/build/distributions/ImageFetcher*.rpm'
    stash name: "imagefetcherrpm", includes: "ImageFetcher/build/distributions/ImageFetcher*.rpm"
  }
  stage('Build ImageService') {
    sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml -t ImageService"
    // archiveArtifacts artifacts: 'ImageService/build/distributions/ImageService*.rpm'
    stash name: "imageservicerpm", includes: "ImageService/build/distributions/ImageService*.rpm"
  }
}
node('imageservice') {
  stage('Unstash the rpms') {
    unstash 'imagefetcherrpm'
    unstash 'imageservicerpm'
  }
}