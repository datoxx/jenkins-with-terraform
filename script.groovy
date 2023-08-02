def incremenVersion() {
    echo "incrementing maven version"
    sh "mvn build-helper:parse-version versions:set \
    -DnewVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}.\\\${parsedVersion.nextIncrementalVersion} versions:commit"
    def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
    def version = matcher[0][1]
    env.IMAGE_NAME = "$version-$BUILD_NUMBER"
}

def buildJar() {
    echo "building the application..."
    sh 'mvn clean package'
} 

def buildImage() {
    echo "building the docker image..."
    withCredentials([usernamePassword(credentialsId: 'docker-hub', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
        sh "docker build -t $DOCKER_REPO:$IMAGE_NAME ."
        sh "echo $PASS | docker login -u $USER --password-stdin"
        sh "docker push $DOCKER_REPO:$IMAGE_NAME"   
    }
} 

def deployAppEC2() {
    echo "waiting for EC2 server to initialize" 
    sleep(time: 90, unit: "SECONDS") 

    echo 'deploying docker image to EC2..'
    echo "${EC2_PUBLIC_IP}"

    def shellCmd = "bash ./server-cmds.sh $IMAGE_NAME $DOCKER_CREDS_USR $DOCKER_CREDS_PSW $DOCKER_REPO"
    def ec2Instance = "ec2-user@$EC2_PUBLIC_IP"

    sshagent(['server-ssh-key']) {
        sh "scp -o StrictHostKeyChecking=no server-cmds.sh ${ec2Instance}:/home/ec2-user"
        sh "scp -o StrictHostKeyChecking=no docker-compose.yaml ${ec2Instance}:/home/ec2-user"
        sh "ssh -o StrictHostKeyChecking=no ${ec2Instance} $shellCmd"
    }
} 

def deployEks() {
    echo 'deploying docker image...'
    sh 'envsubst < kubernetes/deployment.yaml | kubectl apply -f -'
    sh 'envsubst < kubernetes/service.yaml | kubectl apply -f -'

} 


def commitGit() {
    withCredentials([usernamePassword(credentialsId: 'maven-id', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
        // only first time
        sh "git config --global user.email 'jenkins@gmail.com'"
        sh "git config --global user.name 'jenkins'"

        // for logs info
        sh "git status"
        sh "git branch"
        sh "git config --list "
        sh "git remote -v"

        //commit chages
        sh ('git remote  set-url origin https://$USER:$PASS@gitlab.com/davit.ardzenadze.05/java-maven-app.git https://gitlab.com/davit.ardzenadze.05/java-maven-app.git')
        sh "git remote -v"
        sh "git add ."
        sh "git commit -m 'add new chages from jenkins' "
        sh "git push origin HEAD:master"
    }
} 

return this
