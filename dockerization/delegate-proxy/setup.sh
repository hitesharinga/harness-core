#!/usr/bin/env bash
set -x
set -e


IMAGES_DIR="images"
STORAGE_DIR_LOCATION="storage"


JRE_SOURCE_URL_1=https://app.harness.io/storage/wingsdelegates/jre/8u131
JRE_SOLARIS_1=jre-8u131-solaris-x64.tar.gz
JRE_MACOSX_1=jre-8u131-macosx-x64.tar.gz
JRE_LINUX_1=jre-8u131-linux-x64.tar.gz

JRE_SOURCE_URL_2=https://app.harness.io/storage/wingsdelegates/jre/8u191
JRE_SOLARIS_2=jre-8u191-solaris-x64.tar.gz
JRE_MACOSX_2=jre-8u191-macosx-x64.tar.gz
JRE_LINUX_2=jre-8u191-linux-x64.tar.gz

KUBECTL_VERSION=v1.13.2

KUBECTL_LINUX_DIR="${IMAGES_DIR}/kubectl/linux/$KUBECTL_VERSION/"
KUBECTL_MAC_DIR="${IMAGES_DIR}/kubectl/darwin/$KUBECTL_VERSION/"

KUBECTL_LINUX_URL=https://app.harness.io/storage/harness-download/kubernetes-release/release/"$KUBECTL_VERSION"/bin/linux/amd64/kubectl
KUBECTL_MAC_URL=https://app.harness.io/storage/harness-download/kubernetes-release/release/"$KUBECTL_VERSION"/bin/darwin/amd64/kubectl

OC_VERSION=v4.2.16
OC_LINUX_URL=https://app.harness.io/storage/harness-download/harness-oc/release/"$OC_VERSION"/bin/linux/amd64/oc
OC_MAC_URL=https://app.harness.io/storage/harness-download/harness-oc/release/"$OC_VERSION"/bin/darwin/amd64/oc

OC_LINUX_DIR="${IMAGES_DIR}/oc/linux/$OC_VERSION/"
OC_MAC_DIR="${IMAGES_DIR}/oc/darwin/$OC_VERSION/"


mkdir -p $IMAGES_DIR


mv artifacts/${PURPOSE}/watcher-${VERSION}.jar ${IMAGES_DIR}/watcher.jar
mv artifacts/${PURPOSE}/delegate-${VERSION}.jar ${IMAGES_DIR}/delegate.jar


curl "${JRE_SOURCE_URL_1}/${JRE_SOLARIS_1}" > "${JRE_SOLARIS_1}"
curl "${JRE_SOURCE_URL_1}/${JRE_MACOSX_1}" > "${JRE_MACOSX_1}"
curl "${JRE_SOURCE_URL_1}/${JRE_LINUX_1}" > "${JRE_LINUX_1}"

curl "${JRE_SOURCE_URL_2}/${JRE_SOLARIS_2}" > "${JRE_SOLARIS_2}"
curl "${JRE_SOURCE_URL_2}/${JRE_MACOSX_2}" > "${JRE_MACOSX_2}"
curl "${JRE_SOURCE_URL_2}/${JRE_LINUX_2}" > "${JRE_LINUX_2}"


mkdir -p $KUBECTL_LINUX_DIR
mkdir -p $KUBECTL_MAC_DIR

curl -L -o "${KUBECTL_MAC_DIR}kubectl" "${KUBECTL_MAC_URL}"
curl -L -o "${KUBECTL_LINUX_DIR}kubectl" "${KUBECTL_LINUX_URL}"

mkdir -p $OC_LINUX_DIR
mkdir -p $OC_MAC_DIR

curl -L -o "${OC_MAC_DIR}oc" "${OC_MAC_URL}"
curl -L -o "${OC_LINUX_DIR}oc" "${OC_LINUX_URL}"


mv "${JRE_SOLARIS_1}" "${IMAGES_DIR}/"
mv "${JRE_MACOSX_1}" "${IMAGES_DIR}/"
mv "${JRE_LINUX_1}" "${IMAGES_DIR}/"

mv "${JRE_SOLARIS_2}" "${IMAGES_DIR}/"
mv "${JRE_MACOSX_2}" "${IMAGES_DIR}/"
mv "${JRE_LINUX_2}" "${IMAGES_DIR}/"


for goversion in v0.2 v0.3; do
    echo "Adding goversion $goversion"
    GOTEMPLATE_LINUX_DIR="${IMAGES_DIR}/go-template/linux/$goversion/"
    GOTEMPLATE_MAC_DIR="${IMAGES_DIR}/go-template/darwin/$goversion/"

    GOTEMPLATE_LINUX_URL=https://app.harness.io/storage/harness-download/snapshot-go-template/release/"$goversion"/bin/linux/amd64/go-template
    GOTEMPLATE_MAC_URL=https://app.harness.io/storage/harness-download/snapshot-go-template/release/"$goversion"/bin/darwin/amd64/go-template

    echo "$GOTEMPLATE_MAC_DIR"
    echo "$GOTEMPLATE_LINUX_DIR"

    mkdir -p $GOTEMPLATE_LINUX_DIR
    mkdir -p $GOTEMPLATE_MAC_DIR

    curl -L -o "${GOTEMPLATE_LINUX_DIR}go-template" "${GOTEMPLATE_LINUX_URL}"
    curl -L -o "${GOTEMPLATE_MAC_DIR}go-template" "${GOTEMPLATE_MAC_URL}"
done

for helmversion in v2.13.1 v3.0.2; do
    echo "Adding helmversion $helmversion"
    HELM_LINUX_DIR="${IMAGES_DIR}/helm/linux/$helmversion/"
    HELM_MAC_DIR="${IMAGES_DIR}/helm/darwin/$helmversion/"

    HELM_LINUX_URL=https://app.harness.io/storage/harness-download/harness-helm/release/"$helmversion"/bin/linux/amd64/helm
    HELM_MAC_URL=https://app.harness.io/storage/harness-download/harness-helm/release/"$helmversion"/bin/darwin/amd64/helm

    echo "$HELM_MAC_DIR"
    echo "$HELM_LINUX_DIR"

    mkdir -p $HELM_LINUX_DIR
    mkdir -p $HELM_MAC_DIR

    curl -L -o "${HELM_LINUX_DIR}helm" "${HELM_LINUX_URL}"
    curl -L -o "${HELM_MAC_DIR}helm" "${HELM_MAC_URL}"
done

for chartmuseumversion in v0.8.2; do
    echo "Adding chartmuseumversion $chartmuseumversion"
    CHARTMUSEUM_LINUX_DIR="${IMAGES_DIR}/chartmuseum/linux/$chartmuseumversion/"
    CHARTMUSEUM_MAC_DIR="${IMAGES_DIR}/chartmuseum/darwin/$chartmuseumversion/"

    CHARTMUSEUM_LINUX_URL=https://app.harness.io/storage/harness-download/harness-chartmuseum/release/"$chartmuseumversion"/bin/linux/amd64/chartmuseum
    CHARTMUSEUM_MAC_URL=https://app.harness.io/storage/harness-download/harness-chartmuseum/release/"$chartmuseumversion"/bin/darwin/amd64/chartmuseum

    echo "$CHARTMUSEUM_MAC_DIR"
    echo "$CHARTMUSEUM_LINUX_DIR"

    mkdir -p $CHARTMUSEUM_LINUX_DIR
    mkdir -p $CHARTMUSEUM_MAC_DIR

    curl -L -o "${CHARTMUSEUM_LINUX_DIR}chartmuseum" "${CHARTMUSEUM_LINUX_URL}"
    curl -L -o "${CHARTMUSEUM_MAC_DIR}chartmuseum" "${CHARTMUSEUM_MAC_URL}"
done

for tfConfigInspectVersion in v1.0; do
  echo "Adding terraform-config-inspect" $tfConfigInspectVersion

  TF_CONFIG_INSPECT_LINUX_DIR="${IMAGES_DIR}/tf-config-inspect/linux/$tfConfigInspectVersion/"
  TF_CONFIG_INSPECT_MAC_DIR="${IMAGES_DIR}/tf-config-inspect/darwin/$tfConfigInspectVersion/"

  TF_CONFIG_INSPECT_LINUX_URL=https://app.harness.io/storage/harness-download/harness-terraform-config-inspect/"$tfConfigInspectVersion"/linux/amd64/terraform-config-inspect
  TF_CONFIG_INSPECT_MAC_URL=https://app.harness.io/storage/harness-download/harness-terraform-config-inspect/"$tfConfigInspectVersion"/darwin/amd64/terraform-config-inspect

  echo "$TF_CONFIG_INSPECT_LINUX_DIR"
  echo "$TF_CONFIG_INSPECT_MAC_DIR"

  mkdir -p "$TF_CONFIG_INSPECT_LINUX_DIR"
  mkdir -p "$TF_CONFIG_INSPECT_MAC_DIR"

  curl -L -o "${TF_CONFIG_INSPECT_LINUX_DIR}terraform-config-inspect" "$TF_CONFIG_INSPECT_LINUX_URL"
  curl -L -o "${TF_CONFIG_INSPECT_MAC_DIR}terraform-config-inspect" "$TF_CONFIG_INSPECT_MAC_URL"


done



function setupDelegateJars(){
   echo "################################ Setting up Delegate Jars ################################"

    DELEGATE_VERSION=$VERSION
    WATCHER_VERSION=$VERSION

    mkdir -p $STORAGE_DIR_LOCATION/wingsdelegates/jre/8u131/
    mkdir -p $STORAGE_DIR_LOCATION/wingsdelegates/jre/8u191/
    cp images/jre-8u131-*.gz $STORAGE_DIR_LOCATION/wingsdelegates/jre/8u131/
    cp images/jre-8u191-*.gz $STORAGE_DIR_LOCATION/wingsdelegates/jre/8u191/

    rm -rf ${STORAGE_DIR_LOCATION}/wingsdelegates/jobs/deploy-prod-delegate/*
    mkdir -p  ${STORAGE_DIR_LOCATION}/wingsdelegates/jobs/deploy-prod-delegate/${DELEGATE_VERSION}
    cp images/delegate.jar ${STORAGE_DIR_LOCATION}/wingsdelegates/jobs/deploy-prod-delegate/${DELEGATE_VERSION}/

    echo "1.0.${DELEGATE_VERSION} jobs/deploy-prod-delegate/${DELEGATE_VERSION}/delegate.jar" > delegateprod.txt

    mv delegateprod.txt ${STORAGE_DIR_LOCATION}/wingsdelegates

    rm -rf ${STORAGE_DIR_LOCATION}/wingswatchers/jobs/deploy-prod-watcher/*
    mkdir -p  ${STORAGE_DIR_LOCATION}/wingswatchers/jobs/deploy-prod-watcher/${WATCHER_VERSION}
    cp images/watcher.jar ${STORAGE_DIR_LOCATION}/wingswatchers/jobs/deploy-prod-watcher/${WATCHER_VERSION}/
    echo "1.0.${WATCHER_VERSION} jobs/deploy-prod-watcher/${WATCHER_VERSION}/watcher.jar" > watcherprod.txt
    mv watcherprod.txt ${STORAGE_DIR_LOCATION}/wingswatchers

}


function setupClientUtils(){
   echo "################################ Setting up Client Utils ################################"

   echo "Copying kubectl go-template helm chartmuseum tf-config-inspect and oc"

    for platform in linux darwin; do
        for kubectlversion in v1.13.2; do
            mkdir -p ${STORAGE_DIR_LOCATION}/harness-download/kubernetes-release/release/$kubectlversion/bin/${platform}/amd64/
            cp images/kubectl/${platform}/$kubectlversion/kubectl ${STORAGE_DIR_LOCATION}/harness-download/kubernetes-release/release/$kubectlversion/bin/${platform}/amd64/
        done

        for gotemplateversion in v0.2 v0.3; do
            mkdir -p ${STORAGE_DIR_LOCATION}/harness-download/snapshot-go-template/release/$gotemplateversion/bin/${platform}/amd64/
            cp images/go-template/${platform}/$gotemplateversion/go-template ${STORAGE_DIR_LOCATION}/harness-download/snapshot-go-template/release/$gotemplateversion/bin/${platform}/amd64/
        done

        for helmversion in v2.13.1; do
            mkdir -p ${STORAGE_DIR_LOCATION}/harness-download/harness-helm/release/$helmversion/bin/${platform}/amd64/
            cp images/helm/${platform}/$helmversion/helm ${STORAGE_DIR_LOCATION}/harness-download/harness-helm/release/$helmversion/bin/${platform}/amd64/
        done

        for chartmuseumversion in v0.8.2; do
            mkdir -p ${STORAGE_DIR_LOCATION}/harness-download/harness-chartmuseum/release/$chartmuseumversion/bin/${platform}/amd64/
            cp images/chartmuseum/${platform}/$chartmuseumversion/chartmuseum ${STORAGE_DIR_LOCATION}/harness-download/harness-chartmuseum/release/$chartmuseumversion/bin/${platform}/amd64/
        done

        for tfConfigInspectVersion in v1.0; do
           mkdir -p ${STORAGE_DIR_LOCATION}/harness-download/harness-terraform-config-inspect/"$tfConfigInspectVersion"/${platform}/amd64/
           cp images/tf-config-inspect/${platform}/"$tfConfigInspectVersion"/terraform-config-inspect ${STORAGE_DIR_LOCATION}/harness-download/harness-terraform-config-inspect/"$tfConfigInspectVersion"/${platform}/amd64/
         done

        for ocversion in v4.2.16; do
            mkdir -p ${STORAGE_DIR_LOCATION}/harness-download/harness-oc/release/$ocversion/bin/${platform}/amd64/
            cp images/oc/${platform}/$ocversion/oc ${STORAGE_DIR_LOCATION}/harness-download/harness-oc/release/$ocversion/bin/${platform}/amd64/
        done
    done
}

setupDelegateJars
setupClientUtils

ls -ltr

ls -la $STORAGE_DIR_LOCATION

