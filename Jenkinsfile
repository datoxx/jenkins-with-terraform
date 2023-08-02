#!/usr/bin/env groovy

@Library('jenkins-shared-librar')
def gv

pipeline {
    agent any

    tools {
        maven 'Maven'
    }
    environment {
        DOCKER_REPO = "datoxx/my-repo"
    }


    stages {
        stage("init") {
            steps {
                script {
                    gv = load "script.groovy"
                }
            }
        }
        stage("provision server") {
            environment {
                AWS_ACCESS_KEY_ID = credentials('jenkins_aws_access_key_id')
                AWS_SECRET_ACCESS_KEY = credentials('jenkins_aws_secret_access_key')
                TF_VAR_env_prefix = 'test'
            }
            steps {
                script {
                    dir('terraform') {
                        sh "terraform init"
                        sh "terraform apply --auto-approve"
                        EC2_PUBLIC_IP = sh(
                            script: "terraform output ec2_public_ip",
                            returnStdout: true
                        ).trim()
                    }
                }
            }
        }
        stage("incremet version") {
            steps {
                script {
                    gv.incremenVersion()
                }
            }
        }
        stage("build jar") {
            steps {
                script {
                    gv.buildJar()
                }
            }
        }
        stage("build image") {
            steps {
                script {
                  gv.buildImage()
                }
            }
        }

        stage("deploy") {
            environment {
                DOCKER_CREDS = credentials('docker-hub')
            }

            steps{
                script {
                    gv.deployAppEC2()
                }
            }
        }
        stage("commit version update") {
           
            steps {    
                script {
                   gv.commitGit()
                }
            }
        }
    }   
}
