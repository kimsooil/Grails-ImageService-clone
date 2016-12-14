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
    sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'java_home=${env.JAVA_HOME} package_revision=${env.PACKAGE_REVISION}' -t ImageFetcher"
    stash name: "imagefetcherrpm", includes: "ImageFetcher/build/distributions/ImageFetcher*.rpm"
  }
  stage('Build ImageService') {
    sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'java_home=${env.JAVA_HOME} package_revision=${env.PACKAGE_REVISION}' -t ImageService"
    stash name: "imageservicerpm", includes: "ImageService/build/distributions/ImageService*.rpm"
  }
  stage('Stash Deploy Related') {
    sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'keystash=${env.USF_ANSIBLE_VAULT_KEY}' -t keystash"
    stash name: 'keystash', includes: "rpms/ansible-vault-usf*.rpm"
    stash name: 'ansible', includes: "ansible/**/*"
  }
}
node('imageservice') {
  stage('Unstash the rpms') {
    sh 'rm -rf rpms'
    unstash 'keystash'
    dir('rpms') {
      unstash 'imagefetcherrpm'
      unstash 'imageservicerpm'
    }
  }
  stage('Install Ansible') {
    sh 'rpm -iUvh http://download.fedoraproject.org/pub/epel/7/x86_64/e/epel-release-7-8.noarch.rpm || exit 0'
    sh 'yum -y update'
    sh 'yum -y install ansible'
    sh 'yum -y install rpms/ansible-vault-usf*.rpm || exit 0'
    unstash 'ansible'
  }
  stage('Deploy ImageFetcher and ImageService') {
    sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'java_home=${env.JAVA_HOME}' -t deploy"
  }
}
node('master') {
  stage('Build RPM artifacts') {
    sh 'rm -rf rpms'
    dir('rpms') {
      unstash 'imagefetcherrpm'
      unstash 'imageservicerpm'
      archiveArtifacts artifacts: 'ImageFetcher/build/distributions/ImageFetcher*.rpm'
      archiveArtifacts artifacts: 'ImageService/build/distributions/ImageService*.rpm'
    }
  }
}
